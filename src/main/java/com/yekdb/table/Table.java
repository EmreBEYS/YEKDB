package com.yekdb.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB'de bir tablo şemasını temsil eder.
 *
 * Bir tablo, bir tablo adı ve sütun tanımlarının bir listesinden oluşur.
 * Bu sınıf yalnızca şemayı temsil eder, gerçek kayıtları değil.
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
    public Table(String tableName, List<Column> columns) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Table must contain at least one column."
            );
        }

        if (columns.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Column list cannot contain null values."
            );
        }

        long distinctCount = columns.stream()
                .map(column -> column.getName().toLowerCase())
                .distinct()
                .count();

        if (distinctCount != columns.size()) {
            throw new IllegalArgumentException(
                    "Duplicate column names are not allowed."
            );
        }

        this.tableName = tableName.trim().toLowerCase();
        this.columns = new ArrayList<>(columns);
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
        return Collections.unmodifiableList(columns);
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
     * Tabloda verilen isimde bir sütun varsa true döndürür.
     *
     * @param columnName sütun adı
     * @return sütun varsa true
     */
    public boolean hasColumn(String columnName) {

        if (columnName == null || columnName.isBlank()) {
            return false;
        }

        return columns.stream()
                .anyMatch(column ->
                        column.getName().equalsIgnoreCase(columnName.trim()));
    }

    /**
     * Adına göre bir sütun döndürür.
     *
     * @param columnName sütun adı
     * @return eşleşen sütun
     * @throws IllegalArgumentException sütun adı geçersizse veya sütun yoksa
     */
    public Column getColumn(String columnName) {

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be null or blank."
            );
        }

        String normalizedColumnName = columnName.trim();

        return columns.stream()
                .filter(column ->
                        column.getName().equalsIgnoreCase(normalizedColumnName))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Column not found: " + normalizedColumnName
                        ));
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
        return Objects.hash(tableName, columns);
    }
}