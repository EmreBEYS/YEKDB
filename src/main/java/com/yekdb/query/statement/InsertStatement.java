package com.yekdb.query.statement;

import java.util.List;
import java.util.Objects;

/**
 * Parser tarafından ayrıştırılmış INSERT sorgusunu temsil eder.
 *
 * <p>Örnek SQL:</p>
 *
 * <pre>
 * INSERT INTO users (id, name, age)
 * VALUES (1, 'Emre', 21);
 * </pre>
 */
public final class InsertStatement implements Statement {

    /**
     * Verinin ekleneceği tablo adı.
     */
    private final String tableName;

    /**
     * INSERT işleminde kullanılan sütunlar.
     */
    private final List<String> columns;

    /**
     * Sütunlara karşılık gelen değerler.
     */
    private final List<Object> values;

    public InsertStatement(
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
                    "INSERT statement must contain at least one column."
            );
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    "INSERT statement must contain at least one value."
            );
        }

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException(
                    "Column count and value count must be equal."
            );
        }

        this.columns = columns.stream()
                .map(this::validateColumnName)
                .toList();

        this.values = List.copyOf(values);
    }

    @Override
    public StatementType getType() {
        return StatementType.INSERT;
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

    private String validateTableName(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        String normalizedName = tableName.trim();

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid table name: " + tableName
            );
        }

        return normalizedName;
    }

    private String validateColumnName(String columnName) {

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be null or blank."
            );
        }

        String normalizedName = columnName.trim();

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid column name: " + columnName
            );
        }

        return normalizedName;
    }

    @Override
    public String toString() {
        return "InsertStatement{" +
                "tableName='" + tableName + '\'' +
                ", columns=" + columns +
                ", values=" + values +
                '}';
    }
}