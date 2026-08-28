package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ConstraintValidator;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.ValidationContext;
import com.yekdb.constraint.exception.ForeignKeyConstraintViolationException;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * ALTER TABLE ... ADD constraint işlemi sırasında mevcut fiziksel
 * kayıtların yeni constraint ile uyumlu olup olmadığını doğrular.
 *
 * Sprint 00-26 Phase 5.
 */
final class AlterTableConstraintValidator {

    private static final String DATA_FILE_EXTENSION = ".data";

    private AlterTableConstraintValidator() {
        // Utility class.
    }

    static void validateExistingRows(
            Path databaseDirectory,
            TableCatalog tableCatalog,
            Table candidateTable,
            Constraint addedConstraint
    ) {
        Objects.requireNonNull(databaseDirectory, "databaseDirectory cannot be null");
        Objects.requireNonNull(tableCatalog, "tableCatalog cannot be null");
        Objects.requireNonNull(candidateTable, "candidateTable cannot be null");
        Objects.requireNonNull(addedConstraint, "addedConstraint cannot be null");

        Path dataFile = resolveDataFile(
                databaseDirectory,
                candidateTable.getTableName()
        );

        if (!Files.isRegularFile(dataFile)) {
            return;
        }

        StorageEngine storageEngine = new StorageEngine(dataFile);

        try {
            storageEngine.initialize();

            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            List<Record> activeRecords = recordManager.getActiveRecords();

            if (activeRecords.isEmpty()) {
                return;
            }

            if (addedConstraint.type() == ConstraintType.FOREIGN_KEY) {
                validateForeignKeyRows(
                        databaseDirectory,
                        tableCatalog,
                        candidateTable,
                        addedConstraint,
                        recordManager,
                        activeRecords
                );
                return;
            }

            for (Record record : activeRecords) {
                Row row = recordManager.getRow(record.getRecordId());

                ConstraintValidator.validate(
                        new ValidationContext(
                                candidateTable,
                                row,
                                recordManager,
                                record.getRecordId()
                        ),
                        List.of(addedConstraint)
                );
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to validate existing rows for ALTER TABLE constraint on table: "
                            + candidateTable.getTableName(),
                    exception
            );

        } finally {
            shutdown(storageEngine, candidateTable.getTableName());
        }
    }

    private static void validateForeignKeyRows(
            Path databaseDirectory,
            TableCatalog tableCatalog,
            Table childTable,
            Constraint constraint,
            RecordManager childRecordManager,
            List<Record> activeChildRecords
    ) throws IOException {
        if (!(constraint instanceof ForeignKeyConstraint foreignKey)) {
            throw new IllegalStateException(
                    "FOREIGN_KEY constraint metadata must be ForeignKeyConstraint."
            );
        }

        Table parentTable = tableCatalog.getTable(
                foreignKey.referencedTableName()
        );

        List<Integer> childIndexes = resolveColumnIndexes(
                childTable,
                foreignKey.columns()
        );

        List<Integer> parentIndexes = resolveColumnIndexes(
                parentTable,
                foreignKey.referencedColumnNames()
        );

        Path parentDataFile = resolveDataFile(
                databaseDirectory,
                parentTable.getTableName()
        );

        List<Row> parentRows = readActiveRows(
                parentDataFile,
                parentTable.getTableName()
        );

        for (Record childRecord : activeChildRecords) {
            Row childRow = childRecordManager.getRow(
                    childRecord.getRecordId()
            );

            List<Object> localValues = readValues(
                    childRow,
                    childIndexes
            );

            // MATCH SIMPLE: herhangi bir local bileşen NULL ise kontrol atlanır.
            if (localValues.stream().anyMatch(Objects::isNull)) {
                continue;
            }

            boolean referenced = parentRows.stream()
                    .anyMatch(parentRow -> valuesMatch(
                            parentRow,
                            parentIndexes,
                            localValues
                    ));

            if (!referenced) {
                throw new ForeignKeyConstraintViolationException(
                        foreignKey.columns(),
                        foreignKey.referencedTableName(),
                        foreignKey.referencedColumnNames(),
                        localValues
                );
            }
        }
    }

    private static List<Row> readActiveRows(
            Path dataFile,
            String tableName
    ) {
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

            List<Row> rows = new ArrayList<>();

            for (Record record : recordManager.getActiveRecords()) {
                rows.add(
                        recordManager.getRow(record.getRecordId())
                );
            }

            return List.copyOf(rows);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read table rows during ALTER TABLE FOREIGN KEY validation: "
                            + tableName,
                    exception
            );

        } finally {
            shutdown(storageEngine, tableName);
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
                "Unknown column in ALTER TABLE constraint: " + columnName
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

        return Collections.unmodifiableList(
                new ArrayList<>(values)
        );
    }

    private static boolean valuesMatch(
            Row row,
            List<Integer> indexes,
            List<Object> expectedValues
    ) {
        for (int index = 0; index < indexes.size(); index++) {
            Object actual = row.getValue(indexes.get(index));
            Object expected = expectedValues.get(index);

            if (!Objects.equals(actual, expected)) {
                return false;
            }
        }

        return true;
    }

    private static Path resolveDataFile(
            Path databaseDirectory,
            String tableName
    ) {
        return databaseDirectory.resolve(
                tableName.toLowerCase(Locale.ROOT)
                        + DATA_FILE_EXTENSION
        );
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
                    "Failed to close storage during ALTER TABLE constraint validation: "
                            + tableName,
                    exception
            );
        }
    }
}
