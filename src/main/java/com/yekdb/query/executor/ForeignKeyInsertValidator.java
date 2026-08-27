package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.exception.ForeignKeyConstraintViolationException;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Sprint 00-25 Phase 4.
 *
 * INSERT öncesinde FOREIGN KEY değerlerinin referenced tabloda
 * gerçekten mevcut olup olmadığını doğrular.
 *
 * SQL MATCH SIMPLE davranışına yakın olarak local FOREIGN KEY
 * bileşenlerinden herhangi biri NULL ise referans kontrolü atlanır.
 */
final class ForeignKeyInsertValidator {

    private static final String DATA_FILE_EXTENSION = ".data";

    private ForeignKeyInsertValidator() {
        // Utility class.
    }

    static boolean requiresValidation(Table table) {
        Objects.requireNonNull(table, "table cannot be null");

        return table.getConstraints()
                .stream()
                .anyMatch(constraint ->
                        constraint.type() == ConstraintType.FOREIGN_KEY
                );
    }

    static void validate(
            TableManager tableManager,
            Table table,
            Row candidateRow
    ) {
        Objects.requireNonNull(tableManager, "tableManager cannot be null");
        Objects.requireNonNull(table, "table cannot be null");
        Objects.requireNonNull(candidateRow, "candidateRow cannot be null");

        for (Constraint constraint : table.getConstraints()) {
            if (constraint.type() != ConstraintType.FOREIGN_KEY) {
                continue;
            }

            if (!(constraint instanceof ForeignKeyConstraint foreignKey)) {
                throw new IllegalStateException(
                        "FOREIGN_KEY constraint must be ForeignKeyConstraint."
                );
            }

            validateForeignKey(
                    tableManager,
                    table,
                    candidateRow,
                    foreignKey
            );
        }
    }

    private static void validateForeignKey(
            TableManager tableManager,
            Table localTable,
            Row candidateRow,
            ForeignKeyConstraint foreignKey
    ) {
        List<Integer> localIndexes =
                resolveColumnIndexes(
                        localTable,
                        foreignKey.columns()
                );

        List<Object> localValues =
                readValues(
                        candidateRow,
                        localIndexes
                );

        /*
         * MATCH SIMPLE:
         * Composite FK dahil herhangi bir local bileşen NULL ise
         * referenced row araması yapılmaz.
         */
        if (localValues.stream().anyMatch(Objects::isNull)) {
            return;
        }

        Table referencedTable =
                tableManager.getTable(
                        foreignKey.referencedTableName()
                );

        boolean exists =
                referencedRowExists(
                        tableManager,
                        referencedTable,
                        foreignKey.referencedColumnNames(),
                        localValues
                );

        if (!exists) {
            throw new ForeignKeyConstraintViolationException(
                    foreignKey.columns(),
                    foreignKey.referencedTableName(),
                    foreignKey.referencedColumnNames(),
                    localValues
            );
        }
    }

    private static boolean referencedRowExists(
            TableManager tableManager,
            Table referencedTable,
            List<String> referencedColumns,
            List<Object> expectedValues
    ) {
        Path dataFile =
                tableManager
                        .getDatabaseDirectory()
                        .resolve(
                                referencedTable
                                        .getTableName()
                                        .toLowerCase(Locale.ROOT)
                                        + DATA_FILE_EXTENSION
                        );

        /*
         * Tablo var fakat henüz hiç INSERT yapılmadıysa .data dosyası
         * oluşmamış olabilir. Bu durumda referenced row yoktur.
         */
        if (!Files.isRegularFile(dataFile)) {
            return false;
        }

        StorageEngine storageEngine =
                new StorageEngine(dataFile);

        try {
            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            List<Integer> referencedIndexes =
                    resolveColumnIndexes(
                            referencedTable,
                            referencedColumns
                    );

            for (Record record : recordManager.getActiveRecords()) {
                Row existingRow =
                        recordManager.getRow(
                                record.getRecordId()
                        );

                if (matches(
                        existingRow,
                        referencedIndexes,
                        expectedValues
                )) {
                    return true;
                }
            }

            return false;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read referenced table '"
                            + referencedTable.getTableName()
                            + "' during FOREIGN KEY validation.",
                    exception
            );

        } finally {
            if (storageEngine.isInitialized()) {
                try {
                    storageEngine.shutdown();
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Failed to close referenced table storage engine: "
                                    + referencedTable.getTableName(),
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
        List<Integer> indexes =
                new ArrayList<>(columnNames.size());

        for (String columnName : columnNames) {
            indexes.add(
                    findColumnIndex(
                            table,
                            columnName
                    )
            );
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
                "FOREIGN KEY references unknown column: "
                        + columnName
        );
    }

    private static List<Object> readValues(
            Row row,
            List<Integer> indexes
    ) {
        List<Object> values =
                new ArrayList<>(indexes.size());

        for (int index : indexes) {
            values.add(row.getValue(index));
        }

        return Collections.unmodifiableList(
                new ArrayList<>(values)
        );
    }

    private static boolean matches(
            Row row,
            List<Integer> indexes,
            List<Object> expectedValues
    ) {
        for (int index = 0; index < indexes.size(); index++) {
            Object actualValue =
                    row.getValue(
                            indexes.get(index)
                    );

            if (!Objects.equals(
                    actualValue,
                    expectedValues.get(index)
            )) {
                return false;
            }
        }

        return true;
    }
}
