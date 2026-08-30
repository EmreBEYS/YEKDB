package com.yekdb.query.expression;

import java.util.Objects;

/**
 * Bir SQL function çağrısının başka bir değer, kolon veya
 * function sonucu ile karşılaştırılmasını temsil eder.
 *
 * Örnekler:
 *
 * LOWER(city) = 'malatya'
 * ABS(balance) > 100
 * LENGTH(name) >= 5
 * LENGTH(TRIM(name)) = 5
 *
 * Sprint 00-28 Phase 5
 */
public final class FunctionComparisonExpression
        implements Expression {

    private final FunctionCallExpression leftFunction;
    private final ComparisonOperator operator;
    private final Object expectedValue;

    public FunctionComparisonExpression(
            FunctionCallExpression leftFunction,
            ComparisonOperator operator,
            Object expectedValue
    ) {

        this.leftFunction =
                Objects.requireNonNull(
                        leftFunction,
                        "Left function cannot be null."
                );

        this.operator =
                Objects.requireNonNull(
                        operator,
                        "Comparison operator cannot be null."
                );

        /*
         * expectedValue bilinçli olarak null olabilir.
         *
         * Mevcut ExpressionEvaluator davranışı:
         *
         * value = null
         * value != null
         *
         * gibi comparison'ları desteklediği için burada
         * Objects.requireNonNull kullanmıyoruz.
         */
        this.expectedValue = expectedValue;
    }

    public FunctionCallExpression getLeftFunction() {
        return leftFunction;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    /**
     * Sağ operand başka bir function çağrısı mı?
     *
     * Örnek:
     *
     * LENGTH(name) = LENGTH(city)
     */
    public boolean hasFunctionRightOperand() {

        return expectedValue
                instanceof FunctionCallExpression;
    }

    /**
     * Sağ operand bir kolon mu?
     *
     * Örnek:
     *
     * LENGTH(name) = expected_length
     */
    public boolean hasColumnRightOperand() {

        return expectedValue
                instanceof ColumnExpression;
    }

    @Override
    public String toString() {

        return leftFunction
                + " "
                + operator.getSymbol()
                + " "
                + formatExpectedValue();
    }

    private String formatExpectedValue() {

        if (expectedValue instanceof String stringValue) {

            return "'"
                    + stringValue
                    + "'";
        }

        return String.valueOf(
                expectedValue
        );
    }
}