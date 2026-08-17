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

import static com.yekdb.query.evaluator.ExpressionValueSupport.compare;
import static com.yekdb.query.evaluator.ExpressionValueSupport.resolveColumnValue;
import static com.yekdb.query.evaluator.ExpressionValueSupport.toLikeRegex;
import static com.yekdb.query.evaluator.ExpressionValueSupport.valuesEqual;

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
 * BETWEEN / NOT BETWEEN
 * IN / NOT IN
 * LIKE / NOT LIKE
 * ILIKE / NOT ILIKE
 *
 * Sprint 00-15:
 * Qualified kolon çözümleme
 * Column-to-column JOIN karşılaştırmaları
 *
 * Sprint 00-16:
 * Outer JOIN NULL comparison desteği
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

    // ==================================================
    // COMPARISON
    // ==================================================

    /**
     * Tek bir karşılaştırmayı değerlendirir.
     *
     * Destek:
     *
     * age > 18
     *
     * ve:
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

        Object expectedValue;

        /*
         * JOIN ON condition:
         *
         * e.department_id = d.id
         *
         * Sağ operand sabit değer değil,
         * başka bir kolon olabilir.
         */
        if (expression.isColumnToColumnComparison()) {

            ColumnExpression rightColumn =
                    expression.getRightColumnExpression();

            expectedValue =
                    resolveColumnValue(
                            rightColumn,
                            rowValues
                    );

        } else {

            expectedValue =
                    expression.expectedValue();
        }

        /*
         * Outer JOIN sonucunda eşleşmeyen tarafın
         * kolon değeri NULL olabilir.
         *
         * Ordering comparison NULL için eşleşme
         * üretmemelidir.
         */
        if (actualValue == null
                || expectedValue == null) {

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

                case GREATER_THAN,
                     LESS_THAN,
                     GREATER_THAN_OR_EQUALS,
                     LESS_THAN_OR_EQUALS ->
                        false;
            };
        }

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

    // ==================================================
    // BETWEEN
    // ==================================================

    /**
     * BETWEEN / NOT BETWEEN expression'ını değerlendirir.
     *
     * BETWEEN sınırları inclusive'dir.
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

        /*
         * BETWEEN sıralama karşılaştırması gerektirir.
         * Eski evaluator sözleşmesi ve regression testleri
         * null gerçek değer için exception bekler.
         */
        if (actualValue == null) {
            throw new IllegalArgumentException(
                    "Ordering comparison cannot be performed with null values."
            );
        }

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

    // ==================================================
    // IN
    // ==================================================

    /**
     * IN / NOT IN expression'ını değerlendirir.
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

    // ==================================================
    // LIKE
    // ==================================================

    /**
     * LIKE / NOT LIKE / ILIKE / NOT ILIKE
     * expression'ını değerlendirir.
     *
     * SQL wildcard:
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

        /*
         * NULL LIKE pattern eşleşme üretmez.
         */
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
                                    |
                                    Pattern.UNICODE_CASE
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

    // ==================================================
    // LOGICAL
    // ==================================================

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

}
