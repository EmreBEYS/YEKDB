package com.yekdb.query.evaluator;

import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.ColumnExpression;
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
 * Desteklenen expression türleri:
 *
 * - ComparisonExpression
 * - BetweenExpression
 * - InExpression
 * - LikeExpression
 * - LogicalExpression
 * - NotExpression
 *
 * Sprint 00-14:
 *
 * - BETWEEN / NOT BETWEEN
 * - IN / NOT IN
 * - LIKE / NOT LIKE
 * - ILIKE / NOT ILIKE
 *
 * Sprint 00-15:
 *
 * - Qualified column çözümleme
 * - table.column / alias.column desteği
 * - Column-to-column karşılaştırma
 * - JOIN ON condition evaluation
 *
 * Örnek:
 *
 * e.department_id = d.id
 */
public final class ExpressionEvaluator {

    /**
     * Expression sonucunu değerlendirir.
     *
     * @param expression değerlendirilecek expression
     * @param rowValues sütun adı -> değer eşleşmeleri
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

        if (expression
                instanceof ComparisonExpression comparisonExpression) {

            return evaluateComparison(
                    comparisonExpression,
                    rowValues
            );
        }

        if (expression
                instanceof BetweenExpression betweenExpression) {

            return evaluateBetween(
                    betweenExpression,
                    rowValues
            );
        }

        if (expression
                instanceof InExpression inExpression) {

            return evaluateIn(
                    inExpression,
                    rowValues
            );
        }

        if (expression
                instanceof LikeExpression likeExpression) {

            return evaluateLike(
                    likeExpression,
                    rowValues
            );
        }

        if (expression
                instanceof LogicalExpression logicalExpression) {

            return evaluateLogical(
                    logicalExpression,
                    rowValues
            );
        }

        if (expression
                instanceof NotExpression notExpression) {

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

    // --------------------------------------------------
    // COMPARISON
    // --------------------------------------------------

    /**
     * Karşılaştırma expression'ını değerlendirir.
     *
     * Desteklenen örnekler:
     *
     * age > 18
     *
     * e.age > 18
     *
     * e.department_id = d.id
     */
    private boolean evaluateComparison(
            ComparisonExpression expression,
            Map<String, Object> rowValues
    ) {

        ColumnExpression leftColumn =
                expression.getLeftColumnExpression();

        Object actualValue =
                resolveColumnValue(
                        leftColumn,
                        rowValues
                );

        Object expectedValue =
                resolveExpectedValue(
                        expression,
                        rowValues
                );

        return switch (expression.operator()) {

            case EQUALS ->
                    valuesEqual(
                            actualValue,
                            expectedValue
                    );

            case NOT_EQUALS ->
                    !valuesEqual(
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
     * ComparisonExpression sağ tarafını çözer.
     *
     * Normal WHERE:
     *
     * age > 18
     *
     * -> 18
     *
     *
     * JOIN:
     *
     * e.department_id = d.id
     *
     * -> rowValues içerisindeki d.id değeri
     */
    private Object resolveExpectedValue(
            ComparisonExpression expression,
            Map<String, Object> rowValues
    ) {

        if (expression.isColumnToColumnComparison()) {

            ColumnExpression rightColumn =
                    expression.getRightColumnExpression();

            return resolveColumnValue(
                    rightColumn,
                    rowValues
            );
        }

        return expression.expectedValue();
    }

    // --------------------------------------------------
    // BETWEEN
    // --------------------------------------------------

    /**
     * BETWEEN / NOT BETWEEN expression'ını değerlendirir.
     *
     * BETWEEN sınırları inclusive'dir.
     *
     * age BETWEEN 18 AND 30
     *
     * age >= 18 AND age <= 30
     */
    private boolean evaluateBetween(
            BetweenExpression expression,
            Map<String, Object> rowValues
    ) {

        Object actualValue =
                resolveColumnValue(
                        ColumnExpression.parse(
                                expression.getColumnName()
                        ),
                        rowValues
                );

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

    // --------------------------------------------------
    // IN
    // --------------------------------------------------

    /**
     * IN / NOT IN expression'ını değerlendirir.
     *
     * department IN ('IT', 'HR')
     *
     * department NOT IN ('IT', 'HR')
     */
    private boolean evaluateIn(
            InExpression expression,
            Map<String, Object> rowValues
    ) {

        Object actualValue =
                resolveColumnValue(
                        ColumnExpression.parse(
                                expression.getColumnName()
                        ),
                        rowValues
                );

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

    // --------------------------------------------------
    // LIKE
    // --------------------------------------------------

    /**
     * LIKE / NOT LIKE / ILIKE / NOT ILIKE
     * expression'ını değerlendirir.
     *
     * SQL wildcard desteği:
     *
     * % -> sıfır veya daha fazla karakter
     * _ -> tam olarak bir karakter
     */
    private boolean evaluateLike(
            LikeExpression expression,
            Map<String, Object> rowValues
    ) {

        Object actualValue =
                resolveColumnValue(
                        ColumnExpression.parse(
                                expression.getColumnName()
                        ),
                        rowValues
                );

        if (actualValue == null) {

            return false;
        }

        String actualText =
                String.valueOf(
                        actualValue
                );

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

    // --------------------------------------------------
    // LOGICAL
    // --------------------------------------------------

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

    // --------------------------------------------------
    // NOT
    // --------------------------------------------------

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

    // --------------------------------------------------
    // COLUMN RESOLUTION
    // --------------------------------------------------

    /**
     * ColumnExpression içerisindeki kolon değerini
     * rowValues map'inden çözer.
     *
     * Desteklenen örnekler:
     *
     * name
     * employee.name
     * e.name
     *
     * JOIN executor'ın rowValues içerisinde
     * qualified key üretmesi beklenir.
     *
     * Örnek:
     *
     * e.id
     * e.name
     * e.department_id
     * d.id
     * d.name
     */
    private Object resolveColumnValue(
            ColumnExpression column,
            Map<String, Object> rowValues
    ) {

        Objects.requireNonNull(
                column,
                "Column expression cannot be null."
        );

        String qualifiedName =
                column.getQualifiedName();

        /*
         * Öncelik doğrudan tam eşleşme.
         *
         * Örnek:
         *
         * e.department_id
         */
        if (rowValues.containsKey(
                qualifiedName
        )) {

            return rowValues.get(
                    qualifiedName
            );
        }

        /*
         * Qualified olmayan kolon.
         *
         * Örnek:
         *
         * age
         */
        if (!column.isQualified()) {

            String columnName =
                    column.getColumnName();

            if (rowValues.containsKey(
                    columnName
            )) {

                return rowValues.get(
                        columnName
                );
            }

            /*
             * JOIN sonrası rowValues sadece qualified
             * key içeriyorsa tek eşleşen kolonu bul.
             *
             * Örnek:
             *
             * e.name
             *
             * ama sorguda:
             *
             * WHERE name = 'Yunus'
             */
            return resolveUnqualifiedColumn(
                    columnName,
                    rowValues
            );
        }

        throw new IllegalArgumentException(
                "Column not found: "
                        + qualifiedName
        );
    }

    /**
     * Qualified olmayan bir kolon adını
     * JOIN satırı içerisinde çözmeye çalışır.
     *
     * Örnek:
     *
     * rowValues:
     *
     * e.id
     * e.name
     * d.id
     * d.title
     *
     *
     * name
     *
     * -> e.name
     *
     *
     * id
     *
     * -> ambiguous
     */
    private Object resolveUnqualifiedColumn(
            String columnName,
            Map<String, Object> rowValues
    ) {

        Object matchedValue =
                null;

        String matchedKey =
                null;

        int matchCount =
                0;

        for (Map.Entry<String, Object> entry
                : rowValues.entrySet()) {

            String key =
                    entry.getKey();

            if (matchesColumnName(
                    key,
                    columnName
            )) {

                matchedValue =
                        entry.getValue();

                matchedKey =
                        key;

                matchCount++;
            }
        }

        if (matchCount == 1) {

            return matchedValue;
        }

        if (matchCount > 1) {

            throw new IllegalArgumentException(
                    "Ambiguous column reference: "
                            + columnName
            );
        }

        throw new IllegalArgumentException(
                "Column not found: "
                        + columnName
        );
    }

    /**
     * Qualified bir key'in verilen kolon adına
     * ait olup olmadığını kontrol eder.
     *
     * e.name + name
     *
     * -> true
     */
    private boolean matchesColumnName(
            String key,
            String columnName
    ) {

        if (key == null
                || columnName == null) {

            return false;
        }

        if (key.equalsIgnoreCase(
                columnName
        )) {

            return true;
        }

        int dotIndex =
                key.lastIndexOf('.');

        if (dotIndex < 0
                || dotIndex == key.length() - 1) {

            return false;
        }

        String keyColumnName =
                key.substring(
                        dotIndex + 1
                );

        return keyColumnName.equalsIgnoreCase(
                columnName
        );
    }

    // --------------------------------------------------
    // EQUALITY
    // --------------------------------------------------

    /**
     * Değer eşitliğini kontrol eder.
     *
     * Farklı Number türleri ortak sayısal değer
     * üzerinden karşılaştırılır.
     *
     * Örnek:
     *
     * Integer(18)
     * Long(18)
     * Double(18.0)
     *
     * eşit kabul edilir.
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

        if (actualValue
                instanceof Number actualNumber
                &&
                expectedValue
                        instanceof Number expectedNumber) {

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

    // --------------------------------------------------
    // LIKE REGEX
    // --------------------------------------------------

    /**
     * SQL LIKE pattern'ini Java regex pattern'ine
     * dönüştürür.
     *
     * % -> .*
     * _ -> .
     *
     * Regex özel karakterleri literal olarak korunur.
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
                            .append(
                                    character
                            );
                }

                default ->
                        regex.append(
                                character
                        );
            }
        }

        regex.append("$");

        return regex.toString();
    }

    // --------------------------------------------------
    // ORDERING COMPARISON
    // --------------------------------------------------

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
         * Integer(18)
         * Long(18)
         * Double(18.0)
         */
        if (actualValue
                instanceof Number actualNumber
                &&
                expectedValue
                        instanceof Number expectedNumber) {

            return Double.compare(
                    actualNumber.doubleValue(),
                    expectedNumber.doubleValue()
            );
        }

        /*
         * Aynı tip Comparable değerler.
         *
         * Örnek:
         *
         * String
         */
        if (actualValue
                instanceof Comparable<?> comparable
                &&
                actualValue
                        .getClass()
                        .isInstance(
                                expectedValue
                        )) {

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