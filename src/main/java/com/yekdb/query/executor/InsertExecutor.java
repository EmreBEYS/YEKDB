package com.yekdb.query.executor;

import com.yekdb.query.command.InsertCommand;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * INSERT komutlarını yürüten executor bileşenidir.
 *
 * InsertExecutor:
 *
 * - INSERT komutundaki sütunları doğrular.
 * - Değerleri tablo sütun sırasına göre düzenler.
 * - Değer tiplerini tablo şemasına göre kontrol eder.
 * - Row nesnesi oluşturur.
 * - RecordManager üzerinden fiziksel kaydı gerçekleştirir.
 *
 * Sprint 00-12 kapsamında eklenmiştir.
 */
public final class InsertExecutor {

    /**
     * INSERT komutunu çalıştırır.
     *
     * @param table         hedef tablo şeması
     * @param command       çalıştırılacak INSERT komutu
     * @param recordManager fiziksel kayıt yöneticisi
     * @return oluşturulan fiziksel Record
     * @throws IOException fiziksel yazma hatası oluşursa
     */
    public Record execute(
            Table table,
            InsertCommand command,
            RecordManager recordManager
    ) throws IOException {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                command,
                "InsertCommand cannot be null."
        );

        Objects.requireNonNull(
                recordManager,
                "RecordManager cannot be null."
        );

        validateTargetTable(
                table,
                command
        );

        validateColumns(
                table,
                command
        );

        Row row = createRow(
                table,
                command
        );

        return recordManager.insert(row);
    }

    /**
     * Command içerisindeki hedef tablo ile verilen tablo
     * şemasının aynı tabloyu temsil ettiğini doğrular.
     */
    private void validateTargetTable(
            Table table,
            InsertCommand command
    ) {

        if (!table.getTableName().equalsIgnoreCase(
                command.getTableName()
        )) {

            throw new IllegalArgumentException(
                    "INSERT target table does not match supplied table. " +
                            "Expected: " + table.getTableName() +
                            ", actual: " + command.getTableName()
            );
        }
    }

    /**
     * INSERT komutunda belirtilen sütunların tabloda
     * bulunduğunu ve tekrar etmediğini doğrular.
     */
    private void validateColumns(
            Table table,
            InsertCommand command
    ) {

        List<String> columns =
                command.getColumns();

        for (String columnName : columns) {

            if (!table.hasColumn(columnName)) {
                throw new IllegalArgumentException(
                        "Column not found in table '" +
                                table.getTableName() +
                                "': " +
                                columnName
                );
            }
        }

        long distinctColumnCount =
                columns.stream()
                        .map(String::toLowerCase)
                        .distinct()
                        .count();

        if (distinctColumnCount != columns.size()) {
            throw new IllegalArgumentException(
                    "INSERT command contains duplicate columns."
            );
        }

        /*
         * Sprint 00-12'nin mevcut Row modeli tüm tablo
         * sütunları için bir değer beklediğinden eksik
         * sütunlu INSERT şimdilik desteklenmez.
         *
         * DEFAULT ve NULL desteği ilerleyen sprintlerde
         * eklenebilir.
         */
        if (columns.size() != table.getColumnCount()) {
            throw new IllegalArgumentException(
                    "INSERT command must provide a value for every " +
                            "table column. Expected: " +
                            table.getColumnCount() +
                            ", actual: " +
                            columns.size()
            );
        }
    }

    /**
     * INSERT komutundaki değerleri tablonun fiziksel
     * sütun sırasına göre düzenleyerek Row oluşturur.
     */
    private Row createRow(
            Table table,
            InsertCommand command
    ) {

        List<Object> orderedValues =
                new ArrayList<>(
                        table.getColumnCount()
                );

        for (Column tableColumn :
                table.getColumns()) {

            int commandColumnIndex =
                    findColumnIndex(
                            command.getColumns(),
                            tableColumn.getName()
                    );

            if (commandColumnIndex < 0) {
                throw new IllegalArgumentException(
                        "Missing value for column: " +
                                tableColumn.getName()
                );
            }

            Object value =
                    command.getValues().get(
                            commandColumnIndex
                    );

            validateValueType(
                    tableColumn,
                    value
            );

            orderedValues.add(value);
        }

        return new Row(orderedValues);
    }

    /**
     * INSERT komutundaki sütun listesinden belirtilen
     * sütunun indeksini bulur.
     */
    private int findColumnIndex(
            List<String> columns,
            String targetColumn
    ) {

        for (int index = 0;
             index < columns.size();
             index++) {

            if (columns.get(index)
                    .equalsIgnoreCase(targetColumn)) {

                return index;
            }
        }

        return -1;
    }

    /**
     * Bir değerin tablo şemasındaki sütun tipiyle
     * uyumlu olup olmadığını kontrol eder.
     */
    private void validateValueType(
            Column column,
            Object value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "NULL values are not supported yet. Column: " +
                            column.getName()
            );
        }

        DataType dataType =
                column.getDataType();

        boolean valid = switch (dataType) {

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
                    "Invalid value type for column '" +
                            column.getName() +
                            "'. Expected: " +
                            dataType +
                            ", actual: " +
                            value.getClass().getSimpleName()
            );
        }
    }
}