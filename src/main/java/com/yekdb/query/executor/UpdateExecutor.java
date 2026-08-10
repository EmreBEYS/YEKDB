package com.yekdb.query.executor;

import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.evaluator.WhereEvaluator;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * UPDATE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
 *
 * Sprint 00-13 kapsamında gelişmiş WHERE Expression Engine
 * ile entegre edilmiştir.
 *
 * Desteklenen WHERE yapıları:
 *
 * - Comparison
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 *
 * İşlem akışı:
 *
 * UpdateCommand
 *      ↓
 * Active Records
 *      ↓
 * Record -> Row
 *      ↓
 * WhereEvaluator
 *      ↓
 * ExpressionEvaluator
 *      ↓
 * SET değerlerini Row üzerine uygula
 *      ↓
 * RecordManager.update(...)
 */
public final class UpdateExecutor {

    /**
     * UPDATE komutunu çalıştırır.
     *
     * @param table         güncellenecek tablo
     * @param command       UPDATE komutu
     * @param recordManager fiziksel kayıt yöneticisi
     * @return güncellenen satır sayısı
     */
    public int execute(
            Table table,
            UpdateCommand command,
            RecordManager recordManager
    ) throws IOException {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                command,
                "UpdateCommand cannot be null."
        );

        Objects.requireNonNull(
                recordManager,
                "RecordManager cannot be null."
        );

        validateTargetTable(
                table,
                command
        );

        validateUpdatedColumns(
                table,
                command.getUpdatedValues()
        );

        List<Record> activeRecords =
                recordManager.getActiveRecords();

        int updatedRowCount = 0;

        for (Record record : activeRecords) {

            long recordId =
                    record.getRecordId();

            Row currentRow =
                    recordManager.getRow(
                            recordId
                    );

            /*
             * Sprint 00-13:
             *
             * WHERE expression artık doğrudan
             * WhereEvaluator üzerinden merkezi
             * ExpressionEvaluator motoruna gönderilir.
             */
            if (!matchesWhere(
                    table,
                    currentRow,
                    command
            )) {

                continue;
            }

            Row updatedRow =
                    createUpdatedRow(
                            table,
                            currentRow,
                            command.getUpdatedValues()
                    );

            recordManager.update(
                    recordId,
                    updatedRow
            );

            updatedRowCount++;
        }

        return updatedRowCount;
    }

    /**
     * Command içerisindeki tablo ile gerçek tablo
     * aynı tabloyu göstermelidir.
     */
    private void validateTargetTable(
            Table table,
            UpdateCommand command
    ) {

        if (!table.getTableName()
                .equalsIgnoreCase(
                        command.getTableName()
                )) {

            throw new IllegalArgumentException(
                    "UPDATE target table mismatch. Expected '"
                            + table.getTableName()
                            + "' but received '"
                            + command.getTableName()
                            + "'."
            );
        }
    }

    /**
     * SET bölümündeki bütün kolonların tabloda
     * bulunduğunu ve değer tiplerinin doğru olduğunu
     * kontrol eder.
     */
    private void validateUpdatedColumns(
            Table table,
            Map<String, Object> updatedValues
    ) {

        for (Map.Entry<String, Object> entry :
                updatedValues.entrySet()) {

            String columnName =
                    entry.getKey();

            if (!table.hasColumn(
                    columnName
            )) {

                throw new IllegalArgumentException(
                        "Column not found in table '"
                                + table.getTableName()
                                + "': "
                                + columnName
                );
            }

            Column column =
                    table.getColumn(
                            columnName
                    );

            validateValueType(
                    column,
                    entry.getValue()
            );
        }
    }

    /**
     * WHERE ifadesini değerlendirir.
     *
     * WHERE yoksa tüm aktif satırlar eşleşir.
     *
     * WHERE varsa yeni Sprint 00-13
     * Expression Engine kullanılır.
     */
    private boolean matchesWhere(
            Table table,
            Row row,
            UpdateCommand command
    ) {

        if (!command.hasWhereExpression()) {

            return true;
        }

        return WhereEvaluator.evaluate(
                command.getWhereExpression(),
                row,
                table
        );
    }

    /**
     * Mevcut Row'un kopyasını oluşturur ve
     * SET bölümündeki değerleri ilgili kolonlara uygular.
     */
    private Row createUpdatedRow(
            Table table,
            Row currentRow,
            Map<String, Object> updatedValues
    ) {

        Row updatedRow =
                new Row(
                        currentRow.getValues()
                );

        List<Column> columns =
                table.getColumns();

        for (Map.Entry<String, Object> entry :
                updatedValues.entrySet()) {

            String targetColumn =
                    entry.getKey();

            int columnIndex =
                    findColumnIndex(
                            columns,
                            targetColumn
                    );

            updatedRow.setValue(
                    columnIndex,
                    entry.getValue()
            );
        }

        return updatedRow;
    }

    /**
     * Fiziksel Row sırasındaki kolon indeksini bulur.
     *
     * Column isimleri case-insensitive karşılaştırılır.
     */
    private int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {

        for (int index = 0;
             index < columns.size();
             index++) {

            if (columns.get(index)
                    .getName()
                    .equalsIgnoreCase(
                            columnName
                    )) {

                return index;
            }
        }

        throw new IllegalArgumentException(
                "Column index could not be resolved: "
                        + columnName
        );
    }

    /**
     * UPDATE ile verilen yeni değerin kolon tipiyle
     * uyumlu olup olmadığını kontrol eder.
     *
     * NULL desteği henüz storage katmanında bulunmadığı
     * için null değerler şimdilik reddedilir.
     */
    private void validateValueType(
            Column column,
            Object value
    ) {

        if (value == null) {

            throw new IllegalArgumentException(
                    "NULL values are not supported yet for column: "
                            + column.getName()
            );
        }

        DataType dataType =
                column.getDataType();

        boolean valid =
                switch (dataType) {

                    case INT ->
                            value instanceof Integer;

                    case LONG ->
                            value instanceof Long;

                    case DOUBLE ->
                            value instanceof Double;

                    case BOOLEAN ->
                            value instanceof Boolean;

                    case STRING ->
                            value instanceof String;
                };

        if (!valid) {

            throw new IllegalArgumentException(
                    "Invalid value type for column '"
                            + column.getName()
                            + "'. Expected "
                            + dataType
                            + " but received "
                            + value.getClass()
                            .getSimpleName()
                            + "."
            );
        }
    }
}