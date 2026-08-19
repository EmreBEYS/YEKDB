package com.yekdb.storage.table;

import com.yekdb.storage.exception.DuplicateColumnException;
import com.yekdb.storage.exception.InvalidColumnException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * YEKDB içerisinde bir tablo şemasını temsil eder.
 *
 * Bir tablo:
 * - tablo adı,
 * - sütun tanımları
 *
 * bilgilerinden oluşur.
 *
 * Bu sınıf fiziksel kayıtları değil yalnızca tablo şemasını
 * temsil eder.
 *
 * Table nesnesi oluşturulduğunda tüm temel şema kurallarının
 * geçerli olduğu garanti edilir.
 *
 * Sürüm: 1.0
 */
public class Table {

    private final String tableName;
    private final List<Column> columns;

    /**
     * Yeni bir tablo oluşturur.
     *
     * @param tableName tablo adı
     * @param columns   tablo sütunları
     */
    public Table(
            String tableName,
            List<Column> columns
    ) {

        this.tableName =
                TableNameValidator.validate(tableName);

        validateColumns(columns);

        /*
         * List.copyOf sayesinde dışarıdan verilen listenin
         * sonradan değiştirilmesi Table nesnesini etkileyemez.
         */
        this.columns = List.copyOf(columns);
    }

    /**
     * Sütun listesinin tablo kurallarına uygun olduğunu doğrular.
     *
     * @param columns sütun listesi
     */
    private void validateColumns(List<Column> columns) {

        if (columns == null || columns.isEmpty()) {
            throw new InvalidColumnException(
                    "Table must contain at least one column."
            );
        }

        if (columns.stream().anyMatch(Objects::isNull)) {
            throw new InvalidColumnException(
                    "Column list cannot contain null values."
            );
        }

        Set<String> columnNames = new HashSet<>();

        for (Column column : columns) {

            if (!columnNames.add(column.getName())) {
                throw new DuplicateColumnException(
                        "Duplicate column names are not allowed."
                );
            }
        }
    }

    /**
     * Tablo adını döndürür.
     *
     * @return tablo adı
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Değiştirilemez sütun listesini döndürür.
     *
     * @return sütun listesi
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Tablodaki sütun sayısını döndürür.
     *
     * @return sütun sayısı
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Verilen isimde bir sütunun tabloda bulunup bulunmadığını
     * kontrol eder.
     *
     * @param columnName sütun adı
     * @return sütun varsa true
     */
    public boolean hasColumn(String columnName) {

        if (columnName == null || columnName.isBlank()) {
            return false;
        }

        String normalizedColumnName;

        try {
            normalizedColumnName =
                    ColumnNameValidator.validate(columnName);

        } catch (InvalidColumnException exception) {
            return false;
        }

        return columns.stream()
                .anyMatch(column ->
                        column.getName()
                                .equals(normalizedColumnName)
                );
    }

    /**
     * Verilen isimdeki sütunu döndürür.
     *
     * @param columnName sütun adı
     * @return sütun
     */
    public Column getColumn(String columnName) {

        String normalizedColumnName =
                ColumnNameValidator.validate(columnName);

        return columns.stream()
                .filter(column ->
                        column.getName()
                                .equals(normalizedColumnName)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Column not found: "
                                        + normalizedColumnName
                        )
                );
    }

    @Override
    public String toString() {
        return "Table{" +
                "tableName='" + tableName + '\'' +
                ", columns=" + columns +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Table table)) {
            return false;
        }

        return tableName.equals(table.tableName)
                && columns.equals(table.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tableName,
                columns
        );
    }
}