package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.ConstraintValidator;
import com.yekdb.constraint.ValidationContext;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.exception.ForeignKeyDeleteRestrictedException;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * FOREIGN KEY referential actions for DELETE.
 *
 * <p>The class name is kept for source compatibility with Sprint 00-25,
 * but Sprint 00-27 expands its responsibility to DELETE referential-action
 * planning.</p>
 *
 * <p>Sprint 00-27 Phase 5 supports:</p>
 * <ul>
 *     <li>ON DELETE RESTRICT</li>
 *     <li>ON DELETE CASCADE</li>
 *     <li>ON DELETE SET NULL</li>
 * </ul>
 *
 * <p>All cascade targets are discovered first. RESTRICT is validated against
 * the final delete set, then SET NULL mutations are planned and constraint-
 * validated before any row is changed. This keeps NOT NULL and other local
 * constraint failures from leaving a partially mutated statement.</p>
 */
final class ForeignKeyDeleteRestrictValidator {

    private static final String DATA_FILE_EXTENSION = ".data";

    private ForeignKeyDeleteRestrictValidator() {
        // Utility class.
    }

    /**
     * Plans and applies DELETE referential actions for the root statement.
     *
     * <p>The root rows themselves are NOT deleted here; DeleteExecutor keeps
     * ownership of the statement's requested row count and root mutation.
     * Only rows discovered through CASCADE are physically tombstoned here.</p>
     */
    static void validate(
            TableManager tableManager,
            Table parentTable,
            List<DeleteCandidate> candidates,
            RecordManager rootRecordManager
    ) {
        Objects.requireNonNull(tableManager, "tableManager cannot be null");
        Objects.requireNonNull(parentTable, "parentTable cannot be null");
        Objects.requireNonNull(candidates, "candidates cannot be null");
        Objects.requireNonNull(rootRecordManager, "rootRecordManager cannot be null");

        if (candidates.isEmpty()) {
            return;
        }

        DeletePlan plan = buildCascadePlan(
                tableManager,
                parentTable,
                candidates,
                rootRecordManager
        );

        validateRestrictFinalState(
                tableManager,
                plan,
                rootRecordManager
        );

        SetNullPlan setNullPlan = buildSetNullPlan(
                tableManager,
                plan,
                rootRecordManager
        );

        validateSetNullPlan(
                tableManager,
                plan.rootTable(),
                rootRecordManager,
                setNullPlan
        );

        applySetNullUpdates(
                tableManager,
                plan.rootTable(),
                rootRecordManager,
                setNullPlan
        );

        applyCascadeDeletes(
                tableManager,
                parentTable,
                candidates,
                plan,
                rootRecordManager
        );
    }

    /**
     * Sprint 00-27 Phase 4:
     * recursively discovers every row reachable through ON DELETE CASCADE.
     * A set-backed plan prevents infinite recursion for cyclic/self-referencing
     * schemas.
     */
    private static DeletePlan buildCascadePlan(
            TableManager tableManager,
            Table rootTable,
            List<DeleteCandidate> rootCandidates,
            RecordManager rootRecordManager
    ) {
        DeletePlan plan = new DeletePlan();
        Deque<PlannedDelete> queue = new ArrayDeque<>();

        for (DeleteCandidate candidate : rootCandidates) {
            PlannedDelete rootDelete = new PlannedDelete(
                    rootTable,
                    candidate.recordId(),
                    candidate.row()
            );

            if (plan.add(rootDelete)) {
                queue.addLast(rootDelete);
            }
        }

        while (!queue.isEmpty()) {
            PlannedDelete parentDelete = queue.removeFirst();

            for (IncomingForeignKey incoming : incomingForeignKeys(
                    tableManager,
                    parentDelete.table()
            )) {
                ReferentialAction action = incoming.foreignKey().onDelete();

                if (action != ReferentialAction.CASCADE) {
                    continue;
                }

                List<PlannedDelete> matchingChildren = findReferencingRows(
                        tableManager,
                        rootTable,
                        rootRecordManager,
                        parentDelete,
                        incoming
                );

                for (PlannedDelete childDelete : matchingChildren) {
                    if (plan.add(childDelete)) {
                        queue.addLast(childDelete);
                    }
                }
            }
        }

        return plan;
    }

    /**
     * RESTRICT is checked after CASCADE closure is known.
     * A referencing child does not block deletion when that child is already
     * part of the final cascade delete set.
     */
    private static void validateRestrictFinalState(
            TableManager tableManager,
            DeletePlan plan,
            RecordManager rootRecordManager
    ) {
        for (PlannedDelete parentDelete : plan.discoveryOrder()) {
            for (IncomingForeignKey incoming : incomingForeignKeys(
                    tableManager,
                    parentDelete.table()
            )) {
                ReferentialAction action = incoming.foreignKey().onDelete();

                if (action != ReferentialAction.RESTRICT) {
                    continue;
                }

                List<PlannedDelete> referencingRows = findReferencingRows(
                        tableManager,
                        plan.rootTable(),
                        rootRecordManager,
                        parentDelete,
                        incoming
                );

                for (PlannedDelete child : referencingRows) {
                    if (plan.contains(child.table(), child.recordId())) {
                        continue;
                    }

                    ForeignKeyConstraint foreignKey = incoming.foreignKey();
                    List<Integer> parentIndexes = resolveColumnIndexes(
                            parentDelete.table(),
                            foreignKey.referencedColumnNames()
                    );

                    List<Object> parentValues = readValues(
                            parentDelete.row(),
                            parentIndexes
                    );

                    throw new ForeignKeyDeleteRestrictedException(
                            parentDelete.table().getTableName(),
                            foreignKey.referencedColumnNames(),
                            incoming.referencingTable().getTableName(),
                            foreignKey.columns(),
                            parentValues
                    );
                }
            }
        }
    }

    /**
     * Sprint 00-27 Phase 5:
     * plans every ON DELETE SET NULL mutation after the complete CASCADE
     * delete set is known. Rows already scheduled for deletion are skipped.
     */
    private static SetNullPlan buildSetNullPlan(
            TableManager tableManager,
            DeletePlan deletePlan,
            RecordManager rootRecordManager
    ) {
        SetNullPlan setNullPlan = new SetNullPlan();

        for (PlannedDelete parentDelete : deletePlan.discoveryOrder()) {
            for (IncomingForeignKey incoming : incomingForeignKeys(
                    tableManager,
                    parentDelete.table()
            )) {
                if (incoming.foreignKey().onDelete() != ReferentialAction.SET_NULL) {
                    continue;
                }

                List<PlannedDelete> referencingRows = findReferencingRows(
                        tableManager,
                        deletePlan.rootTable(),
                        rootRecordManager,
                        parentDelete,
                        incoming
                );

                List<Integer> childIndexes = resolveColumnIndexes(
                        incoming.referencingTable(),
                        incoming.foreignKey().columns()
                );

                for (PlannedDelete child : referencingRows) {
                    if (deletePlan.contains(child.table(), child.recordId())) {
                        continue;
                    }

                    setNullPlan.add(
                            child.table(),
                            child.recordId(),
                            child.row(),
                            childIndexes
                    );
                }
            }
        }

        return setNullPlan;
    }

    /**
     * Validates the final SET NULL rows before any physical mutation occurs.
     * In particular, an FK column protected by NOT NULL fails here and both
     * parent and child rows remain unchanged.
     */
    private static void validateSetNullPlan(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            SetNullPlan setNullPlan
    ) {
        if (setNullPlan.isEmpty()) {
            return;
        }

        Map<String, OpenTableStorage> openedStorages = new HashMap<>();

        try {
            for (PlannedSetNull plannedSetNull : setNullPlan.mutations()) {
                RecordManager recordManager = recordManagerFor(
                        tableManager,
                        rootTable,
                        rootRecordManager,
                        plannedSetNull.table(),
                        openedStorages
                );

                ConstraintValidator.validate(
                        new ValidationContext(
                                plannedSetNull.table(),
                                plannedSetNull.updatedRow(),
                                recordManager,
                                plannedSetNull.recordId()
                        ),
                        plannedSetNull.table().getConstraints()
                );
            }
        } finally {
            closeStorages(openedStorages.values());
        }
    }

    /**
     * Applies SET NULL updates only after all planned rows passed validation.
     */
    private static void applySetNullUpdates(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            SetNullPlan setNullPlan
    ) {
        if (setNullPlan.isEmpty()) {
            return;
        }

        Map<String, OpenTableStorage> openedStorages = new HashMap<>();

        try {
            for (PlannedSetNull plannedSetNull : setNullPlan.mutations()) {
                RecordManager recordManager = recordManagerFor(
                        tableManager,
                        rootTable,
                        rootRecordManager,
                        plannedSetNull.table(),
                        openedStorages
                );

                try {
                    recordManager.update(
                            plannedSetNull.recordId(),
                            plannedSetNull.updatedRow()
                    );
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Failed to apply ON DELETE SET NULL for table '"
                                    + plannedSetNull.table().getTableName()
                                    + "', recordId="
                                    + plannedSetNull.recordId()
                                    + ".",
                            exception
                    );
                }
            }
        } finally {
            closeStorages(openedStorages.values());
        }
    }

    private static RecordManager recordManagerFor(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            Table table,
            Map<String, OpenTableStorage> openedStorages
    ) {
        if (sameTable(table, rootTable)) {
            return rootRecordManager;
        }

        OpenTableStorage storage = openedStorages.computeIfAbsent(
                normalizeTableName(table.getTableName()),
                ignored -> openTableStorage(tableManager, table)
        );

        return storage.recordManager();
    }

    /**
     * Applies only rows introduced by CASCADE. Root statement candidates are
     * left to DeleteExecutor so affected-row count keeps SQL statement meaning.
     *
     * <p>Reverse discovery order gives descendants priority over ancestors.</p>
     */
    private static void applyCascadeDeletes(
            TableManager tableManager,
            Table rootTable,
            List<DeleteCandidate> rootCandidates,
            DeletePlan plan,
            RecordManager rootRecordManager
    ) {
        Set<Long> rootCandidateIds = new HashSet<>();
        for (DeleteCandidate candidate : rootCandidates) {
            rootCandidateIds.add(candidate.recordId());
        }

        List<PlannedDelete> order = new ArrayList<>(plan.discoveryOrder());
        Map<String, OpenTableStorage> openedStorages = new HashMap<>();

        try {
            for (int index = order.size() - 1; index >= 0; index--) {
                PlannedDelete plannedDelete = order.get(index);

                boolean isRootCandidate = sameTable(
                        plannedDelete.table(),
                        rootTable
                ) && rootCandidateIds.contains(plannedDelete.recordId());

                if (isRootCandidate) {
                    continue;
                }

                RecordManager recordManager;

                if (sameTable(plannedDelete.table(), rootTable)) {
                    recordManager = rootRecordManager;
                } else {
                    OpenTableStorage storage = openedStorages.computeIfAbsent(
                            normalizeTableName(plannedDelete.table().getTableName()),
                            ignored -> openTableStorage(
                                    tableManager,
                                    plannedDelete.table()
                            )
                    );
                    recordManager = storage.recordManager();
                }

                try {
                    recordManager.delete(plannedDelete.recordId());
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Failed to apply ON DELETE CASCADE for table '"
                                    + plannedDelete.table().getTableName()
                                    + "', recordId="
                                    + plannedDelete.recordId()
                                    + ".",
                            exception
                    );
                }
            }
        } finally {
            closeStorages(openedStorages.values());
        }
    }

    private static List<IncomingForeignKey> incomingForeignKeys(
            TableManager tableManager,
            Table parentTable
    ) {
        List<IncomingForeignKey> incoming = new ArrayList<>();

        for (Table referencingTable : tableManager.listTables()) {
            for (Constraint constraint : referencingTable.getConstraints()) {
                if (constraint.type() != ConstraintType.FOREIGN_KEY) {
                    continue;
                }

                if (!(constraint instanceof ForeignKeyConstraint foreignKey)) {
                    throw new IllegalStateException(
                            "FOREIGN_KEY constraint must be ForeignKeyConstraint."
                    );
                }

                if (!foreignKey.referencedTableName()
                        .equalsIgnoreCase(parentTable.getTableName())) {
                    continue;
                }

                incoming.add(
                        new IncomingForeignKey(
                                referencingTable,
                                foreignKey
                        )
                );
            }
        }

        return incoming;
    }

    private static List<PlannedDelete> findReferencingRows(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            PlannedDelete parentDelete,
            IncomingForeignKey incoming
    ) {
        Table referencingTable = incoming.referencingTable();
        ForeignKeyConstraint foreignKey = incoming.foreignKey();

        List<Integer> parentIndexes = resolveColumnIndexes(
                parentDelete.table(),
                foreignKey.referencedColumnNames()
        );

        List<Integer> childIndexes = resolveColumnIndexes(
                referencingTable,
                foreignKey.columns()
        );

        List<Object> parentValues = readValues(
                parentDelete.row(),
                parentIndexes
        );

        List<RecordRow> childRows = loadActiveRows(
                tableManager,
                referencingTable,
                rootTable,
                rootRecordManager
        );

        List<PlannedDelete> matches = new ArrayList<>();

        for (RecordRow child : childRows) {
            List<Object> childValues = readValues(
                    child.row(),
                    childIndexes
            );

            // MATCH SIMPLE: any NULL local component means no reference.
            if (childValues.stream().anyMatch(Objects::isNull)) {
                continue;
            }

            if (valuesEqual(childValues, parentValues)) {
                matches.add(
                        new PlannedDelete(
                                referencingTable,
                                child.recordId(),
                                child.row()
                        )
                );
            }
        }

        return matches;
    }

    private static List<RecordRow> loadActiveRows(
            TableManager tableManager,
            Table table,
            Table rootTable,
            RecordManager rootRecordManager
    ) {
        if (sameTable(table, rootTable)) {
            return readRows(rootRecordManager);
        }

        Path dataFile = dataFile(tableManager, table);
        if (!Files.isRegularFile(dataFile)) {
            return List.of();
        }

        StorageEngine storageEngine = new StorageEngine(dataFile);

        try {
            storageEngine.initialize();

            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            return readRows(recordManager);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read table '"
                            + table.getTableName()
                            + "' during FOREIGN KEY DELETE planning.",
                    exception
            );
        } finally {
            shutdown(storageEngine, table.getTableName());
        }
    }

    private static List<RecordRow> readRows(
            RecordManager recordManager
    ) {
        try {
            List<RecordRow> rows = new ArrayList<>();

            for (Record record : recordManager.getActiveRecords()) {
                rows.add(
                        new RecordRow(
                                record.getRecordId(),
                                recordManager.getRow(record.getRecordId())
                        )
                );
            }

            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read active records during FOREIGN KEY DELETE planning.",
                    exception
            );
        }
    }

    private static OpenTableStorage openTableStorage(
            TableManager tableManager,
            Table table
    ) {
        StorageEngine storageEngine = new StorageEngine(
                dataFile(tableManager, table)
        );

        try {
            storageEngine.initialize();

            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            return new OpenTableStorage(
                    table,
                    storageEngine,
                    recordManager
            );
        } catch (IOException exception) {
            shutdown(storageEngine, table.getTableName());
            throw new IllegalStateException(
                    "Failed to open table '"
                            + table.getTableName()
                            + "' for DELETE referential action.",
                    exception
            );
        }
    }

    private static void closeStorages(
            Iterable<OpenTableStorage> storages
    ) {
        IllegalStateException failure = null;

        for (OpenTableStorage storage : storages) {
            try {
                if (storage.storageEngine().isInitialized()) {
                    storage.storageEngine().shutdown();
                }
            } catch (IOException exception) {
                IllegalStateException current = new IllegalStateException(
                        "Failed to close cascade storage engine for table: "
                                + storage.table().getTableName(),
                        exception
                );

                if (failure == null) {
                    failure = current;
                } else {
                    failure.addSuppressed(current);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private static void shutdown(
            StorageEngine storageEngine,
            String tableName
    ) {
        if (!storageEngine.isInitialized()) {
            return;
        }

        try {
            storageEngine.shutdown();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to close storage engine for table: " + tableName,
                    exception
            );
        }
    }

    private static Path dataFile(
            TableManager tableManager,
            Table table
    ) {
        return tableManager
                .getDatabaseDirectory()
                .resolve(
                        normalizeTableName(table.getTableName())
                                + DATA_FILE_EXTENSION
                );
    }

    private static List<Integer> resolveColumnIndexes(
            Table table,
            List<String> columnNames
    ) {
        List<Integer> indexes = new ArrayList<>(columnNames.size());

        for (String columnName : columnNames) {
            indexes.add(findColumnIndex(table, columnName));
        }

        return List.copyOf(indexes);
    }

    private static int findColumnIndex(
            Table table,
            String columnName
    ) {
        List<Column> columns = table.getColumns();

        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index)
                    .getName()
                    .equalsIgnoreCase(columnName)) {
                return index;
            }
        }

        throw new IllegalArgumentException(
                "FOREIGN KEY references unknown column: " + columnName
        );
    }

    private static List<Object> readValues(
            Row row,
            List<Integer> indexes
    ) {
        List<Object> values = new ArrayList<>(indexes.size());

        for (int index : indexes) {
            values.add(row.getValue(index));
        }

        return values;
    }

    private static boolean valuesEqual(
            List<Object> left,
            List<Object> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            if (!Objects.equals(left.get(index), right.get(index))) {
                return false;
            }
        }

        return true;
    }

    private static boolean sameTable(
            Table left,
            Table right
    ) {
        return left.getTableName().equalsIgnoreCase(right.getTableName());
    }

    private static String normalizeTableName(String tableName) {
        return tableName.toLowerCase(Locale.ROOT);
    }

    record DeleteCandidate(long recordId, Row row) {
        DeleteCandidate {
            Objects.requireNonNull(row, "row cannot be null");
        }
    }

    private record IncomingForeignKey(
            Table referencingTable,
            ForeignKeyConstraint foreignKey
    ) {
    }

    private record RecordRow(long recordId, Row row) {
    }

    private record PlannedDelete(
            Table table,
            long recordId,
            Row row
    ) {
        private PlannedDelete {
            Objects.requireNonNull(table, "table cannot be null");
            Objects.requireNonNull(row, "row cannot be null");
        }
    }

    private static final class PlannedSetNull {

        private final Table table;
        private final long recordId;
        private final Row originalRow;
        private final Set<Integer> nullColumnIndexes = new LinkedHashSet<>();

        private PlannedSetNull(
                Table table,
                long recordId,
                Row originalRow
        ) {
            this.table = Objects.requireNonNull(table, "table cannot be null");
            this.recordId = recordId;
            this.originalRow = Objects.requireNonNull(
                    originalRow,
                    "originalRow cannot be null"
            );
        }

        void addNullIndexes(List<Integer> indexes) {
            nullColumnIndexes.addAll(indexes);
        }

        Table table() {
            return table;
        }

        long recordId() {
            return recordId;
        }

        Row updatedRow() {
            Row updated = new Row(originalRow.getValues());
            for (int index : nullColumnIndexes) {
                updated.setValue(index, null);
            }
            return updated;
        }
    }

    private static final class SetNullPlan {

        private final Map<String, LinkedHashMap<Long, PlannedSetNull>> byTable =
                new LinkedHashMap<>();
        private final List<PlannedSetNull> mutations = new ArrayList<>();

        void add(
                Table table,
                long recordId,
                Row row,
                List<Integer> nullIndexes
        ) {
            String tableKey = normalizeTableName(table.getTableName());
            LinkedHashMap<Long, PlannedSetNull> records = byTable.computeIfAbsent(
                    tableKey,
                    ignored -> new LinkedHashMap<>()
            );

            PlannedSetNull mutation = records.get(recordId);
            if (mutation == null) {
                mutation = new PlannedSetNull(table, recordId, row);
                records.put(recordId, mutation);
                mutations.add(mutation);
            }

            mutation.addNullIndexes(nullIndexes);
        }

        boolean isEmpty() {
            return mutations.isEmpty();
        }

        List<PlannedSetNull> mutations() {
            return List.copyOf(mutations);
        }
    }

    private record OpenTableStorage(
            Table table,
            StorageEngine storageEngine,
            RecordManager recordManager
    ) {
    }

    /**
     * LinkedHashMap keeps stable discovery order for deterministic tests and
     * child-first reverse application.
     */
    private static final class DeletePlan {

        private final Map<String, LinkedHashMap<Long, PlannedDelete>> byTable =
                new LinkedHashMap<>();

        private final List<PlannedDelete> discoveryOrder = new ArrayList<>();
        private Table rootTable;

        boolean add(PlannedDelete plannedDelete) {
            if (rootTable == null) {
                rootTable = plannedDelete.table();
            }

            String tableKey = normalizeTableName(
                    plannedDelete.table().getTableName()
            );

            LinkedHashMap<Long, PlannedDelete> records =
                    byTable.computeIfAbsent(
                            tableKey,
                            ignored -> new LinkedHashMap<>()
                    );

            if (records.containsKey(plannedDelete.recordId())) {
                return false;
            }

            records.put(plannedDelete.recordId(), plannedDelete);
            discoveryOrder.add(plannedDelete);
            return true;
        }

        boolean contains(Table table, long recordId) {
            Map<Long, PlannedDelete> records = byTable.get(
                    normalizeTableName(table.getTableName())
            );

            return records != null && records.containsKey(recordId);
        }

        List<PlannedDelete> discoveryOrder() {
            return List.copyOf(discoveryOrder);
        }

        Table rootTable() {
            return rootTable;
        }
    }
}
