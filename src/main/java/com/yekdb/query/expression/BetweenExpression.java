package com.yekdb.query.expression;

import java.util.Objects;

/**
 * SQL BETWEEN / NOT BETWEEN predicate yapısını temsil eder.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * age BETWEEN 18 AND 30
 * age NOT BETWEEN 18 AND 30
 * </pre>
 *
 * Sprint 00-14
 */
public final class BetweenExpression implements Expression {

    private final String columnName;
    private final Object lowerBound;
    private final Object upperBound;
    private final boolean negated;

    /**
     * Normal BETWEEN expression oluşturur.
     */
    public BetweenExpression(
            String columnName,
            Object lowerBound,
            Object upperBound
    ) {
        this(
                columnName,
                lowerBound,
                upperBound,
                false
        );
    }

    /**
     * BETWEEN veya NOT BETWEEN expression oluşturur.
     */
    public BetweenExpression(
            String columnName,
            Object lowerBound,
            Object upperBound,
            boolean negated
    ) {

        this.columnName = Objects.requireNonNull(
                columnName,
                "columnName cannot be null"
        );

        if (columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "columnName cannot be blank"
            );
        }

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.negated = negated;
    }

    public String getColumnName() {
        return columnName;
    }

    public Object getLowerBound() {
        return lowerBound;
    }

    public Object getUpperBound() {
        return upperBound;
    }

    public boolean isNegated() {
        return negated;
    }

    @Override
    public String toString() {

        return columnName
                + (negated
                ? " NOT BETWEEN "
                : " BETWEEN ")
                + lowerBound
                + " AND "
                + upperBound;
    }
}