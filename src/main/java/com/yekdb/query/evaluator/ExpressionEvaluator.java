package com.yekdb.query.evaluator;

import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.InExpression;
import com.yekdb.query.expression.LikeExpression;
import com.yekdb.query.expression.LikeOperator;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.NotExpression;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Expression ağacını bir satırdaki sütun değerlerine karşı değerlendirir.
 *
 * <p>Desteklenen expression türleri:</p>
 *
 * <ul>
 *     <li>ComparisonExpression</li>
 *     <li>BetweenExpression</li>
 *     <li>InExpression</li>
 *     <li>LikeExpression</li>
 *     <li>LogicalExpression</li>
 *     <li>NotExpression</li>
 * </ul>
 *
 * Sprint 00-14:
 * BETWEEN / NOT BETWEEN
 * IN / NOT IN
 * LIKE / NOT LIKE
 * ILIKE / NOT ILIKE
 * desteği eklenmiştir.
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

        if (expression instanceof BetweenExpression betweenExpression) {

            return evaluateBetween(
                    betweenExpression,
                    rowValues
            );
        }

        if (expression instanceof InExpression inExpression) {

            return evaluateIn(
                    inExpression,
                    rowValues
            );
        }

        if (expression instanceof LikeExpression likeExpression) {

            return evaluateLike(
                    likeExpression,
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
                    "Column not found: "
                            + columnName
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
     * BETWEEN / NOT BETWEEN expression'ını değerlendirir.
     *
     * <p>BETWEEN sınırları inclusive'dir.</p>
     *
     * <pre>
     * age BETWEEN 18 AND 30
     *
     * age >= 18 AND age <= 30
     * </pre>
     */
    private boolean evaluateBetween(
            BetweenExpression expression,
            Map<String, Object> rowValues
    ) {

        String columnName =
                expression.getColumnName();

        if (!rowValues.containsKey(columnName)) {

            throw new IllegalArgumentException(
                    "Column not found: "
                            + columnName
            );
        }

        Object actualValue =
                rowValues.get(columnName);

        Object lowerBound =
                expression.getLowerBound();

        Object upperBound =
                expression.getUpperBound();

        boolean result =
                compare(
                        actualValue,
                        lowerBound
                ) >= 0
                        &&
                        compare(
                                actualValue,
                                upperBound
                        ) <= 0;

        return expression.isNegated()
                ? !result
                : result;
    }

    /**
     * IN / NOT IN expression'ını değerlendirir.
     *
     * <pre>
     * department IN ('IT', 'HR')
     * department NOT IN ('IT', 'HR')
     * </pre>
     */
    private boolean evaluateIn(
            InExpression expression,
            Map<String, Object> rowValues
    ) {

        String columnName =
                expression.getColumnName();

        if (!rowValues.containsKey(columnName)) {

            throw new IllegalArgumentException(
                    "Column not found: "
                            + columnName
            );
        }

        Object actualValue =
                rowValues.get(columnName);

        boolean result =
                expression.getValues()
                        .stream()
                        .anyMatch(
                                expectedValue ->
                                        valuesEqual(
                                                actualValue,
                                                expectedValue
                                        )
                        );

        return expression.isNegated()
                ? !result
                : result;
    }

    /**
     * LIKE / NOT LIKE / ILIKE / NOT ILIKE
     * expression'ını değerlendirir.
     *
     * <p>SQL wildcard desteği:</p>
     *
     * <ul>
     *     <li>% -> sıfır veya daha fazla karakter</li>
     *     <li>_ -> tam olarak bir karakter</li>
     * </ul>
     */
    private boolean evaluateLike(
            LikeExpression expression,
            Map<String, Object> rowValues
    ) {

        String columnName =
                expression.getColumnName();

        if (!rowValues.containsKey(columnName)) {

            throw new IllegalArgumentException(
                    "Column not found: "
                            + columnName
            );
        }

        Object actualValue =
                rowValues.get(columnName);

        if (actualValue == null) {
            return false;
        }

        String actualText =
                String.valueOf(actualValue);

        String regex =
                toLikeRegex(
                        expression.getPattern()
                );

        LikeOperator operator =
                expression.getOperator();

        boolean caseInsensitive =
                operator == LikeOperator.ILIKE
                        ||
                        operator == LikeOperator.NOT_ILIKE;

        boolean negated =
                operator == LikeOperator.NOT_LIKE
                        ||
                        operator == LikeOperator.NOT_ILIKE;

        Pattern pattern;

        if (caseInsensitive) {

            pattern =
                    Pattern.compile(
                            regex,
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.UNICODE_CASE
                    );

        } else {

            pattern =
                    Pattern.compile(
                            regex
                    );
        }

        boolean result =
                pattern.matcher(
                        actualText
                ).matches();

        return negated
                ? !result
                : result;
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
     * IN karşılaştırmalarında değer eşitliğini kontrol eder.
     *
     * <p>Farklı Number türleri ortak sayısal değer
     * üzerinden karşılaştırılır.</p>
     */
    private boolean valuesEqual(
            Object actualValue,
            Object expectedValue
    ) {

        if (actualValue == null
                || expectedValue == null) {

            return Objects.equals(
                    actualValue,
                    expectedValue
            );
        }

        if (actualValue instanceof Number actualNumber
                && expectedValue instanceof Number expectedNumber) {

            return Double.compare(
                    actualNumber.doubleValue(),
                    expectedNumber.doubleValue()
            ) == 0;
        }

        return Objects.equals(
                actualValue,
                expectedValue
        );
    }

    /**
     * SQL LIKE pattern'ini Java regex pattern'ine dönüştürür.
     *
     * <p>
     * % -> .*
     * _ -> .
     * </p>
     *
     * <p>Regex özel karakterleri literal olarak korunur.</p>
     */
    private String toLikeRegex(
            String sqlPattern
    ) {

        StringBuilder regex =
                new StringBuilder();

        regex.append("^");

        for (int i = 0;
             i < sqlPattern.length();
             i++) {

            char character =
                    sqlPattern.charAt(i);

            switch (character) {

                case '%' ->
                        regex.append(".*");

                case '_' ->
                        regex.append(".");

                case '\\',
                     '.',
                     '^',
                     '$',
                     '|',
                     '?',
                     '*',
                     '+',
                     '(',
                     ')',
                     '[',
                     ']',
                     '{',
                     '}' -> {

                    regex.append("\\")
                            .append(character);
                }

                default ->
                        regex.append(character);
            }
        }

        regex.append("$");

        return regex.toString();
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