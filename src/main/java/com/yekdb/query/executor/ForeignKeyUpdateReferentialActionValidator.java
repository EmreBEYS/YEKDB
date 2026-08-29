package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ConstraintValidator;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.ValidationContext;
import com.yekdb.constraint.exception.ForeignKeyUpdateRestrictedException;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Sprint 00-27 Phase 6 - FOREIGN KEY referential actions for UPDATE.
 *
 * <p>Supported actions:</p>
 * <ul>
 *     <li>ON UPDATE RESTRICT</li>
 *     <li>ON UPDATE CASCADE</li>
 *     <li>ON UPDATE SET NULL</li>
 * </ul>
 *
 * <p>The complete propagation plan is built and local constraints are
 * validated before any physical child mutation is applied. Root UPDATE
 * candidates are supplied by {@link UpdateExecutor}; this class adds the
 * rows reached through referential actions.</p>
 */
final class ForeignKeyUpdateReferentialActionValidator {

    private static final String DATA_FILE_EXTENSION = ".data";

    private ForeignKeyUpdateReferentialActionValidator() {
        // Utility class.
    }

    static UpdatePlan plan(
            TableManager tableManager,
            Table rootTable,
            List<UpdateCandidate> rootCandidates,
            RecordManager rootRecordManager
    ) {
        Objects.requireNonNull(tableManager, "tableManager cannot be null");
        Objects.requireNonNull(rootTable, "rootTable cannot be null");
        Objects.requireNonNull(rootCandidates, "rootCandidates cannot be null");
        Objects.requireNonNull(rootRecordManager, "rootRecordManager cannot be null");

        UpdatePlan plan = new UpdatePlan(rootTable);
        Deque<PlannedUpdate> queue = new ArrayDeque<>();

        for (UpdateCandidate candidate : rootCandidates) {
            PlannedUpdate rootUpdate = plan.addOrMerge(
                    rootTable,
                    candidate.recordId(),
                    candidate.currentRow(),
                    candidate.updatedRow(),
                    true
            );
            queue.addLast(rootUpdate);
        }

        while (!queue.isEmpty()) {
            PlannedUpdate parentUpdate = queue.removeFirst();

            for (IncomingForeignKey incoming : incomingForeignKeys(
                    tableManager,
                    parentUpdate.table()
            )) {
                ForeignKeyConstraint foreignKey = incoming.foreignKey();

                List<Integer> parentIndexes = resolveColumnIndexes(
                        parentUpdate.table(),
                        foreignKey.referencedColumnNames()
                );

                List<Object> oldParentValues = readValues(
                        parentUpdate.originalRow(),
                        parentIndexes
                );
                List<Object> newParentValues = readValues(
                        parentUpdate.updatedRow(),
                        parentIndexes
                );

                if (valuesEqual(oldParentValues, newParentValues)) {
                    continue;
                }

                List<RecordRow> referencingRows = findReferencingRows(
                        tableManager,
                        rootTable,
                        rootRecordManager,
                        plan,
                        incoming,
                        oldParentValues
                );

                if (referencingRows.isEmpty()) {
                    continue;
                }

                ReferentialAction action = foreignKey.onUpdate();

                if (action == ReferentialAction.RESTRICT) {
                    throw new ForeignKeyUpdateRestrictedException(
                            parentUpdate.table().getTableName(),
                            foreignKey.referencedColumnNames(),
                            incoming.referencingTable().getTableName(),
                            foreignKey.columns(),
                            oldParentValues,
                            newParentValues
                    );
                }

                List<Integer> childIndexes = resolveColumnIndexes(
                        incoming.referencingTable(),
                        foreignKey.columns()
                );

                for (RecordRow child : referencingRows) {
                    Row childUpdatedRow = new Row(child.row().getValues());

                    for (int index = 0; index < childIndexes.size(); index++) {
                        Object newValue = action == ReferentialAction.CASCADE
                                ? newParentValues.get(index)
                                : null;
                        childUpdatedRow.setValue(childIndexes.get(index), newValue);
                    }

                    PlannedUpdate existing = plan.get(
                            incoming.referencingTable(),
                            child.recordId()
                    );

                    if (existing != null
                            && rowsEqual(existing.updatedRow(), childUpdatedRow)) {
                        continue;
                    }

                    PlannedUpdate plannedChild = plan.addOrMerge(
                            incoming.referencingTable(),
                            child.recordId(),
                            child.row(),
                            childUpdatedRow,
                            false
                    );

                    queue.addLast(plannedChild);
                }
            }
        }

        validateGeneratedMutations(
                tableManager,
                rootTable,
                rootRecordManager,
                plan
        );

        return plan;
    }

    static void applyGeneratedMutations(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            UpdatePlan plan
    ) {
        Objects.requireNonNull(plan, "plan cannot be null");

        Map<String, OpenTableStorage> openedStorages = new HashMap<>();

        try {
            for (PlannedUpdate update : plan.discoveryOrder()) {
                if (update.rootCandidate()) {
                    continue;
                }

                RecordManager recordManager = recordManagerFor(
                        tableManager,
                        rootTable,
                        rootRecordManager,
                        update.table(),
                        openedStorages
                );

                try {
                    recordManager.update(
                            update.recordId(),
                            update.updatedRow()
                    );
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Failed to apply ON UPDATE referential action for table '"
                                    + update.table().getTableName()
                                    + "', recordId="
                                    + update.recordId()
                                    + ".",
                            exception
                    );
                }
            }
        } finally {
            closeStorages(openedStorages.values());
        }
    }

    private static void validateGeneratedMutations(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            UpdatePlan plan
    ) {
        Map<String, OpenTableStorage> openedStorages = new HashMap<>();

        try {
            for (PlannedUpdate update : plan.discoveryOrder()) {
                if (update.rootCandidate()) {
                    continue;
                }

                RecordManager recordManager = recordManagerFor(
                        tableManager,
                        rootTable,
                        rootRecordManager,
                        update.table(),
                        openedStorages
                );

                ConstraintValidator.validate(
                        new ValidationContext(
                                update.table(),
                                update.updatedRow(),
                                recordManager,
                                update.recordId()
                        ),
                        update.table().getConstraints()
                );
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

                incoming.add(new IncomingForeignKey(referencingTable, foreignKey));
            }
        }

        return incoming;
    }

    private static List<RecordRow> findReferencingRows(
            TableManager tableManager,
            Table rootTable,
            RecordManager rootRecordManager,
            UpdatePlan plan,
            IncomingForeignKey incoming,
            List<Object> oldParentValues
    ) {
        List<Integer> childIndexes = resolveColumnIndexes(
                incoming.referencingTable(),
                incoming.foreignKey().columns()
        );

        List<RecordRow> rows = loadActiveRows(
                tableManager,
                incoming.referencingTable(),
                rootTable,
                rootRecordManager
        );

        List<RecordRow> matches = new ArrayList<>();

        for (RecordRow stored : rows) {
            PlannedUpdate alreadyPlanned = plan.get(
                    incoming.referencingTable(),
                    stored.recordId()
            );

            Row effectiveRow = alreadyPlanned == null
                    ? stored.row()
                    : alreadyPlanned.updatedRow();

            List<Object> childValues = readValues(effectiveRow, childIndexes);

            // MATCH SIMPLE: any NULL local component means no active reference.
            if (childValues.stream().anyMatch(Objects::isNull)) {
                continue;
            }

            if (valuesEqual(childValues, oldParentValues)) {
                matches.add(new RecordRow(stored.recordId(), effectiveRow));
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
                            + "' during FOREIGN KEY UPDATE planning.",
                    exception
            );
        } finally {
            shutdown(storageEngine, table.getTableName());
        }
    }

    private static List<RecordRow> readRows(RecordManager recordManager) {
        try {
            List<RecordRow> rows = new ArrayList<>();
            for (Record record : recordManager.getActiveRecords()) {
                long recordId = record.getRecordId();
                rows.add(new RecordRow(recordId, recordManager.getRow(recordId)));
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read active records during FOREIGN KEY UPDATE planning.",
                    exception
            );
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

    private static OpenTableStorage openTableStorage(
            TableManager tableManager,
            Table table
    ) {
        StorageEngine storageEngine = new StorageEngine(dataFile(tableManager, table));

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );
            return new OpenTableStorage(storageEngine, recordManager);
        } catch (IOException exception) {
            shutdown(storageEngine, table.getTableName());
            throw new IllegalStateException(
                    "Failed to open table '"
                            + table.getTableName()
                            + "' for UPDATE referential action.",
                    exception
            );
        }
    }

    private static void closeStorages(Iterable<OpenTableStorage> storages) {
        IllegalStateException failure = null;

        for (OpenTableStorage storage : storages) {
            try {
                storage.storageEngine().shutdown();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "Failed to close UPDATE referential-action storage.",
                            exception
                    );
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private static void shutdown(StorageEngine storageEngine, String tableName) {
        try {
            storageEngine.shutdown();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to close table '" + tableName + "'.",
                    exception
            );
        }
    }

    private static Path dataFile(TableManager tableManager, Table table) {
        return tableManager.getDatabaseDirectory().resolve(
                normalizeTableName(table.getTableName()) + DATA_FILE_EXTENSION
        );
    }

    private static boolean sameTable(Table left, Table right) {
        return left.getTableName().equalsIgnoreCase(right.getTableName());
    }

    private static String normalizeTableName(String tableName) {
        return tableName.toLowerCase(Locale.ROOT);
    }

    private static List<Integer> resolveColumnIndexes(
            Table table,
            List<String> columnNames
    ) {
        List<Integer> indexes = new ArrayList<>(columnNames.size());

        for (String columnName : columnNames) {
            boolean found = false;
            for (int index = 0; index < table.getColumns().size(); index++) {
                if (table.getColumns().get(index).getName()
                        .equalsIgnoreCase(columnName)) {
                    indexes.add(index);
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalStateException(
                        "Column '" + columnName + "' not found in table '"
                                + table.getTableName() + "'."
                );
            }
        }

        return indexes;
    }

    private static List<Object> readValues(Row row, List<Integer> indexes) {
        List<Object> values = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            values.add(row.getValue(index));
        }
        return values;
    }


    private static boolean rowsEqual(Row left, Row right) {
        return left.getValues().equals(right.getValues());
    }

    private static boolean valuesEqual(List<Object> left, List<Object> right) {
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

    record UpdateCandidate(
            long recordId,
            Row currentRow,
            Row updatedRow
    ) {
        UpdateCandidate {
            Objects.requireNonNull(currentRow, "currentRow cannot be null");
            Objects.requireNonNull(updatedRow, "updatedRow cannot be null");
        }
    }

    static final class UpdatePlan {
        private final Table rootTable;
        private final Map<RowKey, PlannedUpdate> updates = new LinkedHashMap<>();

        private UpdatePlan(Table rootTable) {
            this.rootTable = rootTable;
        }

        PlannedUpdate addOrMerge(
                Table table,
                long recordId,
                Row originalRow,
                Row updatedRow,
                boolean rootCandidate
        ) {
            RowKey key = new RowKey(normalizeTableName(table.getTableName()), recordId);
            PlannedUpdate existing = updates.get(key);

            if (existing == null) {
                PlannedUpdate created = new PlannedUpdate(
                        table,
                        recordId,
                        new Row(originalRow.getValues()),
                        new Row(updatedRow.getValues()),
                        rootCandidate
                );
                updates.put(key, created);
                return created;
            }

            existing.replaceUpdatedRow(updatedRow);
            existing.rootCandidate = existing.rootCandidate || rootCandidate;
            return existing;
        }

        PlannedUpdate get(Table table, long recordId) {
            return updates.get(new RowKey(
                    normalizeTableName(table.getTableName()),
                    recordId
            ));
        }

        List<PlannedUpdate> discoveryOrder() {
            return List.copyOf(updates.values());
        }

        Table rootTable() {
            return rootTable;
        }
    }

    static final class PlannedUpdate {
        private final Table table;
        private final long recordId;
        private final Row originalRow;
        private Row updatedRow;
        private boolean rootCandidate;

        private PlannedUpdate(
                Table table,
                long recordId,
                Row originalRow,
                Row updatedRow,
                boolean rootCandidate
        ) {
            this.table = table;
            this.recordId = recordId;
            this.originalRow = originalRow;
            this.updatedRow = updatedRow;
            this.rootCandidate = rootCandidate;
        }

        Table table() {
            return table;
        }

        long recordId() {
            return recordId;
        }

        Row originalRow() {
            return originalRow;
        }

        Row updatedRow() {
            return updatedRow;
        }

        boolean rootCandidate() {
            return rootCandidate;
        }

        private void replaceUpdatedRow(Row row) {
            this.updatedRow = new Row(row.getValues());
        }
    }

    private record IncomingForeignKey(
            Table referencingTable,
            ForeignKeyConstraint foreignKey
    ) {
    }

    private record RecordRow(long recordId, Row row) {
    }

    private record RowKey(String tableName, long recordId) {
    }

    private record OpenTableStorage(
            StorageEngine storageEngine,
            RecordManager recordManager
    ) {
    }
}
