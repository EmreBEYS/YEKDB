package com.yekdb.query.command;

import com.yekdb.query.expression.Expression;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Execution katmanında kullanılacak UPDATE komutudur.
 */
public final class UpdateCommand implements Command {

    private final String tableName;

    private final Map<String, Object> updatedValues;

    private final Expression whereExpression;

    public UpdateCommand(
            String tableName,
            Map<String, Object> updatedValues,
            Expression whereExpression
    ) {

        if (tableName == null
                || tableName.isBlank()) {

            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        Objects.requireNonNull(
                updatedValues,
                "Updated values cannot be null."
        );

        if (updatedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "UPDATE command must contain at least one value."
            );
        }

        this.tableName =
                tableName.trim();

        this.updatedValues =
                Map.copyOf(
                        new LinkedHashMap<>(
                                updatedValues
                        )
                );

        this.whereExpression =
                whereExpression;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, Object> getUpdatedValues() {
        return updatedValues;
    }

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public boolean hasWhereExpression() {
        return whereExpression != null;
    }
}