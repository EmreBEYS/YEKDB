package com.yekdb.query.evaluator;


import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.NotExpression;

import java.util.Map;
import java.util.Objects;

/**
 * Expression ağacını bir satırdaki sütun değerlerine karşı değerlendirir.
 *
 * Desteklenen expression türleri:
 *
 * - ComparisonExpression
 * - LogicalExpression
 * - NotExpression
 *
 * Örnek:
 *
 * age > 18 AND city = 'Malatya'
 */
public final class ExpressionEvaluator {

    /**
     * Expression sonucunu değerlendirir.
     *
     * @param expression değerlendirilecek expression
     * @param rowValues  sütun adı -> değer eşleşmeleri
     * @return expression doğruysa true
     */
    public boolean evaluate(
            Expression expression,
            Map<String, Object> rowValues
    ) {

        Objects.requireNonNull(
                expression,
                "Expression cannot be null."
        );

        Objects.requireNonNull(
                rowValues,
                "Row values cannot be null."
        );

        if (expression instanceof ComparisonExpression comparisonExpression) {

            return evaluateComparison(
                    comparisonExpression,
                    rowValues
            );
        }

        if (expression instanceof LogicalExpression logicalExpression) {

            return evaluateLogical(
                    logicalExpression,
                    rowValues
            );
        }

        if (expression instanceof NotExpression notExpression) {

            return evaluateNot(
                    notExpression,
                    rowValues
            );
        }

        throw new IllegalArgumentException(
                "Unsupported expression type: "
                        + expression.getClass().getName()
        );
    }

    /**
     * Tek bir karşılaştırmayı değerlendirir.
     */
    private boolean evaluateComparison(
            ComparisonExpression expression,
            Map<String, Object> rowValues
    ) {

        String columnName =
                expression.columnName();

        if (!rowValues.containsKey(columnName)) {

            throw new IllegalArgumentException(
                    "Column not found: " + columnName
            );
        }

        Object actualValue =
                rowValues.get(columnName);

        Object expectedValue =
                expression.expectedValue();

        return switch (expression.operator()) {

            case EQUALS ->
                    Objects.equals(
                            actualValue,
                            expectedValue
                    );

            case NOT_EQUALS ->
                    !Objects.equals(
                            actualValue,
                            expectedValue
                    );

            case GREATER_THAN ->
                    compare(
                            actualValue,
                            expectedValue
                    ) > 0;

            case LESS_THAN ->
                    compare(
                            actualValue,
                            expectedValue
                    ) < 0;

            case GREATER_THAN_OR_EQUALS ->
                    compare(
                            actualValue,
                            expectedValue
                    ) >= 0;

            case LESS_THAN_OR_EQUALS ->
                    compare(
                            actualValue,
                            expectedValue
                    ) <= 0;
        };
    }

    /**
     * AND / OR expression'larını değerlendirir.
     */
    private boolean evaluateLogical(
            LogicalExpression expression,
            Map<String, Object> rowValues
    ) {

        return switch (expression.operator()) {

            case AND ->
                    evaluate(
                            expression.leftExpression(),
                            rowValues
                    )
                            &&
                            evaluate(
                                    expression.rightExpression(),
                                    rowValues
                            );

            case OR ->
                    evaluate(
                            expression.leftExpression(),
                            rowValues
                    )
                            ||
                            evaluate(
                                    expression.rightExpression(),
                                    rowValues
                            );
        };
    }

    /**
     * NOT expression'ını değerlendirir.
     */
    private boolean evaluateNot(
            NotExpression expression,
            Map<String, Object> rowValues
    ) {

        return !evaluate(
                expression.expression(),
                rowValues
        );
    }

    /**
     * Sıralama gerektiren değerleri karşılaştırır.
     */
    private int compare(
            Object actualValue,
            Object expectedValue
    ) {

        if (actualValue == null
                || expectedValue == null) {

            throw new IllegalArgumentException(
                    "Ordering comparison cannot be performed with null values."
            );
        }

        /*
         * Sayısal türler farklı olabilir.
         *
         * Örnek:
         * Integer(18)
         * Long(18)
         * Double(18.0)
         */
        if (actualValue instanceof Number actualNumber
                && expectedValue instanceof Number expectedNumber) {

            return Double.compare(
                    actualNumber.doubleValue(),
                    expectedNumber.doubleValue()
            );
        }

        /*
         * Aynı tip Comparable değerler.
         *
         * Örnek:
         * String
         */
        if (actualValue instanceof Comparable<?> comparable
                && actualValue.getClass()
                .isInstance(expectedValue)) {

            return compareComparable(
                    comparable,
                    expectedValue
            );
        }

        throw new IllegalArgumentException(
                "Values cannot be compared: "
                        + actualValue
                        + " and "
                        + expectedValue
        );
    }

    /**
     * Comparable generic dönüşümünü tek noktada tutar.
     */
    @SuppressWarnings("unchecked")
    private int compareComparable(
            Comparable<?> actualValue,
            Object expectedValue
    ) {

        Comparable<Object> comparable =
                (Comparable<Object>) actualValue;

        return comparable.compareTo(
                expectedValue
        );
    }
}