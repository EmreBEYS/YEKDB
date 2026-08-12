package com.yekdb.query.expression;

import java.util.Objects;

/**
 * SQL LIKE / NOT LIKE / ILIKE / NOT ILIKE
 * predicate yapısını temsil eder.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * name LIKE 'Ali%'
 * name NOT LIKE 'Ali%'
 * name ILIKE 'ali%'
 * name NOT ILIKE 'ali%'
 * </pre>
 *
 * Sprint 00-14
 */
public final class LikeExpression implements Expression {

    private final String columnName;
    private final String pattern;
    private final LikeOperator operator;

    public LikeExpression(
            String columnName,
            String pattern,
            LikeOperator operator
    ) {

        this.columnName = Objects.requireNonNull(columnName, "columnName cannot be null");

        if (columnName.isBlank()) {
            throw new IllegalArgumentException("columnName cannot be blank");
        }

        this.pattern = Objects.requireNonNull(pattern, "pattern cannot be null");

        this.operator = Objects.requireNonNull(operator, "operator cannot be null");
    }

    public String getColumnName() {
        return columnName;
    }

    public String getPattern() {
        return pattern;
    }

    public LikeOperator getOperator() {
        return operator;
    }

    @Override
    public String toString() {

        return columnName
                + " "
                + switch (operator) {

            case LIKE ->
                    "LIKE";

            case NOT_LIKE ->
                    "NOT LIKE";

            case ILIKE ->
                    "ILIKE";

            case NOT_ILIKE ->
                    "NOT ILIKE";
        }
                + " '"
                + pattern
                + "'";
    }
}