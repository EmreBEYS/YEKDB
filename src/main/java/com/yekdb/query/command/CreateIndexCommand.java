package com.yekdb.query.command;

import java.util.Objects;

/**
 * CREATE INDEX / CREATE UNIQUE INDEX SQL komutunu temsil eder.
 */
public final class CreateIndexCommand implements Command {

    private final String indexName;
    private final String tableName;
    private final String columnName;
    private final boolean unique;

    public CreateIndexCommand(
            String indexName,
            String tableName,
            String columnName,
            boolean unique
    ) {
        this.indexName =
                requireName(
                        indexName,
                        "Index name cannot be blank."
                );

        this.tableName =
                requireName(
                        tableName,
                        "Table name cannot be blank."
                );

        this.columnName =
                requireName(
                        columnName,
                        "Column name cannot be blank."
                );

        this.unique = unique;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isUnique() {
        return unique;
    }

    private static String requireName(
            String value,
            String message
    ) {
        String normalized =
                Objects.requireNonNull(
                        value,
                        message
                ).trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    message
            );
        }

        return normalized;
    }
}