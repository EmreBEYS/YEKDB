package com.yekdb.query.expression;

import com.yekdb.query.function.FunctionParameter;
import com.yekdb.query.function.FunctionRegistry;
import com.yekdb.query.function.SqlFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves runtime values used by SQL scalar functions.
 *
 * Supported values:
 *
 * - literal
 * - ColumnExpression
 * - nested FunctionCallExpression
 *
 * Example:
 *
 * LENGTH(TRIM(name))
 *
 * is resolved recursively:
 *
 * name
 * -> TRIM(name)
 * -> LENGTH(...)
 */
public final class FunctionValueResolver {

    private final FunctionRegistry functionRegistry;

    public FunctionValueResolver(
            FunctionRegistry functionRegistry
    ) {
        this.functionRegistry =
                Objects.requireNonNull(
                        functionRegistry,
                        "Function registry cannot be null."
                );
    }

    /**
     * Resolves a raw SQL expression value.
     *
     * @param value expression value
     * @param rowValues current row values
     * @return evaluated runtime value
     */
    public Object resolve(
            Object value,
            Map<String, Object> rowValues
    ) {
        Objects.requireNonNull(
                rowValues,
                "Row values cannot be null."
        );

        if (value instanceof FunctionCallExpression functionCall) {

            return resolveFunction(
                    functionCall,
                    rowValues
            );
        }

        if (value instanceof ColumnExpression columnExpression) {

            return resolveColumn(
                    columnExpression,
                    rowValues
            );
        }

        /*
         * Everything else is treated as a literal.
         *
         * Examples:
         * "YEKDB"
         * 25
         * -500
         * null
         */
        return value;
    }

    /**
     * Evaluates a FunctionCallExpression.
     */
    private Object resolveFunction(
            FunctionCallExpression functionCall,
            Map<String, Object> rowValues
    ) {

        SqlFunction function =
                functionRegistry.resolve(
                        functionCall.getFunctionName()
                );

        List<FunctionParameter> parameters =
                new ArrayList<>();

        for (Object argument
                : functionCall.getArguments()) {

            Object resolvedArgument =
                    resolve(
                            argument,
                            rowValues
                    );

            parameters.add(
                    FunctionParameter.of(
                            resolvedArgument
                    )
            );
        }

        return function.execute(
                parameters
        );
    }

    /**
     * Resolves a column using the same case-insensitive
     * behavior used by the existing expression subsystem.
     */
    private Object resolveColumn(
            ColumnExpression columnExpression,
            Map<String, Object> rowValues
    ) {

        String qualifiedName =
                columnExpression.getQualifiedName();

        for (Map.Entry<String, Object> entry
                : rowValues.entrySet()) {

            if (entry.getKey()
                    .equalsIgnoreCase(
                            qualifiedName
                    )) {

                return entry.getValue();
            }
        }

        /*
         * Qualified reference:
         *
         * users.name
         */
        if (columnExpression.isQualified()) {

            throw new IllegalArgumentException(
                    "Column not found: "
                            + qualifiedName
            );
        }

        /*
         * Unqualified column.
         *
         * name
         *
         * Joined row içerisinde:
         *
         * users.name
         * customers.name
         *
         * gibi değerler bulunabileceği için ambiguity
         * kontrolünü koruyoruz.
         */
        String suffix =
                "."
                        + columnExpression.getColumnName();

        boolean found = false;
        Object resolvedValue = null;

        for (Map.Entry<String, Object> entry
                : rowValues.entrySet()) {

            String key =
                    entry.getKey();

            if (!key.equalsIgnoreCase(
                    columnExpression.getColumnName()
            )
                    &&
                    !key.toLowerCase()
                            .endsWith(
                                    suffix.toLowerCase()
                            )) {

                continue;
            }

            if (!found) {

                resolvedValue =
                        entry.getValue();

                found = true;
                continue;
            }

            if (!Objects.equals(
                    resolvedValue,
                    entry.getValue()
            )) {

                throw new IllegalArgumentException(
                        "Ambiguous column reference: "
                                + columnExpression.getColumnName()
                );
            }
        }

        if (!found) {

            throw new IllegalArgumentException(
                    "Column not found: "
                            + columnExpression.getColumnName()
            );
        }

        return resolvedValue;
    }
}