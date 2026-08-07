package com.yekdb.query.statement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parser tarafından ayrıştırılmış UPDATE sorgusunu temsil eder.
 *
 * Örnek:
 *
 * UPDATE users
 * SET name = 'Emre', age = 22
 * WHERE id = 1;
 */
public final class UpdateStatement implements Statement {

    private final String tableName;
    private final Map<String, Object> updatedValues;
    private final String whereClause;

    public UpdateStatement(
            String tableName,
            Map<String, Object> updatedValues,
            String whereClause
    ) {
        this.tableName =
                validateIdentifier(
                        tableName,
                        "Table name"
                );

        Objects.requireNonNull(
                updatedValues,
                "Updated values cannot be null."
        );

        if (updatedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "UPDATE statement must contain at least one SET value."
            );
        }

        Map<String, Object> normalizedValues =
                new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry :
                updatedValues.entrySet()) {

            String columnName =
                    validateIdentifier(
                            entry.getKey(),
                            "Column name"
                    );

            if (normalizedValues.containsKey(
                    columnName
            )) {
                throw new IllegalArgumentException(
                        "Duplicate UPDATE column: "
                                + columnName
                );
            }

            normalizedValues.put(
                    columnName,
                    entry.getValue()
            );
        }

        this.updatedValues =
                Map.copyOf(
                        normalizedValues
                );

        if (whereClause != null
                && whereClause.isBlank()) {
            throw new IllegalArgumentException(
                    "WHERE clause cannot be blank."
            );
        }

        this.whereClause =
                whereClause == null
                        ? null
                        : whereClause.trim();
    }

    @Override
    public StatementType getType() {
        return StatementType.UPDATE;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, Object> getUpdatedValues() {
        return updatedValues;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public boolean hasWhereClause() {
        return whereClause != null;
    }

    private String validateIdentifier(
            String value,
            String fieldName
    ) {
        if (value == null
                || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be null or blank."
            );
        }

        String normalized =
                value.trim();

        if (!normalized.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid "
                            + fieldName.toLowerCase()
                            + ": "
                            + value
            );
        }

        return normalized;
    }
}