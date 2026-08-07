package com.yekdb.query.command;

import java.util.List;
import java.util.Objects;

/**
 * INSERT SQL komutunun execution katmanındaki temsilidir.
 *
 * Örnek:
 *
 * INSERT INTO users (id, name, age)
 * VALUES (1, 'Emre', 21);
 */
public final class InsertCommand implements Command {

    private final String tableName;

    private final List<String> columns;

    private final List<Object> values;

    public InsertCommand(
            String tableName,
            List<String> columns,
            List<Object> values
    ) {
        this.tableName = validateTableName(tableName);

        Objects.requireNonNull(
                columns,
                "Column list cannot be null."
        );

        Objects.requireNonNull(
                values,
                "Value list cannot be null."
        );

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "INSERT command must contain at least one column."
            );
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    "INSERT command must contain at least one value."
            );
        }

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException(
                    "Column count and value count must be equal."
            );
        }

        this.columns = columns.stream()
                .map(InsertCommand::validateColumnName)
                .toList();

        this.values = List.copyOf(values);
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Object> getValues() {
        return values;
    }

    private static String validateTableName(String tableName) {

        String normalizedName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        return normalizedName;
    }

    private static String validateColumnName(String columnName) {

        String normalizedName = Objects.requireNonNull(
                columnName,
                "Column name cannot be null."
        ).trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be blank."
            );
        }

        return normalizedName;
    }

    @Override
    public String toString() {
        return "InsertCommand{" +
                "tableName='" + tableName + '\'' +
                ", columns=" + columns +
                ", values=" + values +
                '}';
    }
}