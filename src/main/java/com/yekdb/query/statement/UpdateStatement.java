package com.yekdb.query.statement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parser tarafından ayrıştırılmış UPDATE sorgusunu temsil eder.
 *
 * <p>Örnek SQL:</p>
 *
 * <pre>
 * UPDATE users
 * SET age = 22,
 *     name = 'Emre'
 * WHERE id = 1;
 * </pre>
 */

public final class UpdateStatement implements Statement{
    /**
     * Güncellenecek tablo adı.
     */
    private final String tableName;

    /**
     * Güncellenecek kolon ve yeni değerleri.
     */
    private final Map<String, Object> updatedValues;

    /**
     * WHERE koşulu.
     *
     * İlk sürümde metinsel olarak tutulmaktadır.
     */
    private final String whereClause;

    /**
     * Yeni UpdateStatement oluşturur.
     *
     * @param tableName güncellenecek tablo
     * @param updatedValues güncellenecek alanlar
     * @param whereClause WHERE koşulu
     */
    public UpdateStatement(
            String tableName,
            Map<String, Object> updatedValues,
            String whereClause
    ) {

        this.tableName = validateTableName(tableName);

        Objects.requireNonNull(
                updatedValues,
                "Updated values cannot be null."
        );

        if (updatedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "UPDATE statement must contain at least one column."
            );
        }

        this.updatedValues = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : updatedValues.entrySet()) {
            this.updatedValues.put(
                    validateColumnName(entry.getKey()),
                    entry.getValue()
            );
        }

        this.whereClause = whereClause;
    }

    @Override
    public StatementType getType() {
        return StatementType.UPDATE;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, Object> getUpdatedValues() {
        return java.util.Collections.unmodifiableMap(
                new LinkedHashMap<>(updatedValues)
        );
    }

    public String getWhereClause() {
        return whereClause;
    }

    public boolean hasWhereClause() {
        return whereClause != null
                && !whereClause.isBlank();
    }

    private String validateTableName(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        String normalizedName = tableName.trim();

        if (!normalizedName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
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

        if (!normalizedName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Invalid column name: " + columnName
            );
        }

        return normalizedName;
    }

    @Override
    public String toString() {
        return "UpdateStatement{" +
                "tableName='" + tableName + '\'' +
                ", updatedValues=" + updatedValues +
                ", whereClause='" + whereClause + '\'' +
                '}';
    }
}
