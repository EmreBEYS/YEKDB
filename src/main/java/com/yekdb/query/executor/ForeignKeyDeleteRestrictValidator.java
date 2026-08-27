package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Sprint 00-25 Phase 6.
 *
 * DELETE öncesinde parent row'un başka tablolardaki FOREIGN KEY kayıtları
 * tarafından referanslanıp referanslanmadığını kontrol eder.
 *
 * Bu sprintte yalnızca RESTRICT / NO ACTION davranışı uygulanır.
 * CASCADE / SET NULL desteği bilinçli olarak kapsam dışıdır.
 */
final class ForeignKeyDeleteRestrictValidator {

    private static final String DATA_FILE_EXTENSION = ".data";

    private ForeignKeyDeleteRestrictValidator() {
        // Utility class.
    }

    static void validate(
            TableManager tableManager,
            Table parentTable,
            List<DeleteCandidate> candidates
    ) {
        Objects.requireNonNull(tableManager, "tableManager cannot be null");
        Objects.requireNonNull(parentTable, "parentTable cannot be null");
        Objects.requireNonNull(candidates, "candidates cannot be null");

        if (candidates.isEmpty()) {
            return;
        }

        Set<Long> deletingRecordIds = new HashSet<>();
        for (DeleteCandidate candidate : candidates) {
            deletingRecordIds.add(candidate.recordId());
        }

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

                validateReferencingTable(
                        tableManager,
                        parentTable,
                        referencingTable,
                        foreignKey,
                        candidates,
                        deletingRecordIds
                );
            }
        }
    }

    private static void validateReferencingTable(
            TableManager tableManager,
            Table parentTable,
            Table referencingTable,
            ForeignKeyConstraint foreignKey,
            List<DeleteCandidate> candidates,
            Set<Long> deletingRecordIds
    ) {
        Path dataFile = tableManager
                .getDatabaseDirectory()
                .resolve(
                        referencingTable.getTableName()
                                .toLowerCase(Locale.ROOT)
                                + DATA_FILE_EXTENSION
                );

        if (!Files.isRegularFile(dataFile)) {
            return;
        }

        List<Integer> parentIndexes = resolveColumnIndexes(
                parentTable,
                foreignKey.referencedColumnNames()
        );

        List<Integer> childIndexes = resolveColumnIndexes(
                referencingTable,
                foreignKey.columns()
        );

        StorageEngine storageEngine = new StorageEngine(dataFile);

        try {
            storageEngine.initialize();

            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            for (Record childRecord : recordManager.getActiveRecords()) {
                /*
                 * Self-referencing table durumunda aynı DELETE statement
                 * içinde silinecek child row'ları final-state yaklaşımıyla
                 * referans engeli olarak saymayız.
                 */
                if (referencingTable.getTableName()
                        .equalsIgnoreCase(parentTable.getTableName())
                        && deletingRecordIds.contains(childRecord.getRecordId())) {
                    continue;
                }

                Row childRow = recordManager.getRow(childRecord.getRecordId());
                List<Object> childValues = readValues(childRow, childIndexes);

                // MATCH SIMPLE: herhangi bir local FK bileşeni NULL ise referans yoktur.
                if (childValues.stream().anyMatch(Objects::isNull)) {
                    continue;
                }

                for (DeleteCandidate candidate : candidates) {
                    List<Object> parentValues = readValues(
                            candidate.row(),
                            parentIndexes
                    );

                    if (valuesEqual(childValues, parentValues)) {
                        throw new ForeignKeyDeleteRestrictedException(
                                parentTable.getTableName(),
                                foreignKey.referencedColumnNames(),
                                referencingTable.getTableName(),
                                foreignKey.columns(),
                                parentValues
                        );
                    }
                }
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read referencing table '"
                            + referencingTable.getTableName()
                            + "' during FOREIGN KEY DELETE validation.",
                    exception
            );

        } finally {
            if (storageEngine.isInitialized()) {
                try {
                    storageEngine.shutdown();
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Failed to close referencing table storage engine: "
                                    + referencingTable.getTableName(),
                            exception
                    );
                }
            }
        }
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

    record DeleteCandidate(long recordId, Row row) {
        DeleteCandidate {
            Objects.requireNonNull(row, "row cannot be null");
        }
    }
}
