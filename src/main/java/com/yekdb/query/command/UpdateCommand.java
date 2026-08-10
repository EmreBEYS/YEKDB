package com.yekdb.query.command;

import com.yekdb.query.expression.Expression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Execution katmanında kullanılacak UPDATE komutudur.
 *
 * Sprint 00-13 kapsamında WHERE alanı gelişmiş
 * Expression AST yapısını destekler.
 *
 * Desteklenen WHERE yapıları:
 *
 * - Comparison
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 */
public final class UpdateCommand implements Command {

    private final String tableName;

    private final Map<String, Object> updatedValues;

    /**
     * WHERE expression ağacı.
     *
     * WHERE yoksa null olabilir.
     */
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

        /*
         * SET sırasını korumak ve dışarıdan mutation
         * yapılmasını engellemek için immutable
         * LinkedHashMap görünümü oluşturulur.
         */
        this.updatedValues =
                Collections.unmodifiableMap(
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

    @Override
    public String toString() {

        return "UpdateCommand{" +
                "tableName='" + tableName + '\'' +
                ", updatedValues=" + updatedValues +
                ", whereExpression=" + whereExpression +
                '}';
    }
}