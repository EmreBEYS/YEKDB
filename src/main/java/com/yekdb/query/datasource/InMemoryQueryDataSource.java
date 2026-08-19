package com.yekdb.query.datasource;

import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Tablo ve satırları bellekte tutan QueryDataSource uygulamasıdır.
 *
 * Sprint 00-11 kapsamında QueryExecutor entegrasyonunu
 * test etmek ve göstermek için kullanılır.
 */
public final class InMemoryQueryDataSource
        implements QueryDataSource {

    private final Map<String, Table> tables;
    private final Map<String, List<Row>> tableRows;

    public InMemoryQueryDataSource() {
        this.tables = new LinkedHashMap<>();
        this.tableRows = new LinkedHashMap<>();
    }

    /**
     * Tabloyu ve tabloya ait satırları veri kaynağına ekler.
     */
    public void register(
            Table table,
            List<Row> rows
    ) {
        Objects.requireNonNull(
                table,
                "Table cannot be  null."
        );

        Objects.requireNonNull(
                rows,
                "Row list cannot be null."
        );

        for (Row row : rows) {
            if (row == null) {
                throw new IllegalArgumentException(
                        "The row list cannot contain null rows."
                );
            }

            if (row.size() != table.getColumnCount()) {
                throw new IllegalArgumentException(
                        "The number of row values does not match the number of table columns."
                );
            }
        }

        String tableName = normalize(
                table.getTableName()
        );

        tables.put(tableName, table);
        tableRows.put(tableName, List.copyOf(rows));
    }

    @Override
    public Table getTable(String tableName) {
        String normalizedName = normalize(tableName);

        Table table = tables.get(normalizedName);

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table not found in data source: "
                            + tableName
            );
        }

        return table;
    }

    @Override
    public List<Row> getRows(String tableName) {
        String normalizedName = normalize(tableName);

        List<Row> rows = tableRows.get(normalizedName);

        if (rows == null) {
            throw new IllegalArgumentException(
                    "Table rows not found: "
                            + tableName
            );
        }

        return rows;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "The table name cannot be null or empty."
            );
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}