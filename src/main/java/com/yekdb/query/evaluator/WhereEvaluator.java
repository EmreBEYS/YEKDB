package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.NotExpression;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.Objects;
import java.util.function.Function;

/**
 * WHERE expression ağacını değerlendirir.
 *
 * Bu sınıf iki kullanım biçimini korur:
 *
 * - Function tabanlı eski API
 * - Row + Table tabanlı API
 *
 * Function tabanlı API doğrudan dışarıdan verilen
 * valueProvider üzerinden kolon değerini okur.
 */
public final class WhereEvaluator {

    private WhereEvaluator() {
    }

    /**
     * Bir WHERE ifadesini dışarıdan verilen kolon
     * değer sağlayıcısı üzerinden değerlendirir.
     *
     * @param expression değerlendirilecek expression
     * @param valueProvider kolon adına göre değer sağlayan fonksiyon
     * @return koşul sonucu
     */
    public static boolean evaluate(
            Expression expression,
            Function<String, Object> valueProvider
    ) {

        Objects.requireNonNull(
                expression,
                "Expression cannot be null."
        );

        Objects.requireNonNull(
                valueProvider,
                "Value provider cannot be null."
        );

        if (expression instanceof ComparisonExpression comparisonExpression) {

            return evaluateComparison(
                    comparisonExpression,
                    valueProvider
            );
        }

        if (expression instanceof LogicalExpression logicalExpression) {

            return evaluateLogical(
                    logicalExpression,
                    valueProvider
            );
        }

        if (expression instanceof NotExpression notExpression) {

            return !evaluate(
                    notExpression.expression(),
                    valueProvider
            );
        }

        throw new IllegalArgumentException(
                "Unsupported expression type: "
                        + expression.getClass().getName()
        );
    }

    /**
     * Tek bir karşılaştırma ifadesini değerlendirir.
     */
    private static boolean evaluateComparison(
            ComparisonExpression expression,
            Function<String, Object> valueProvider
    ) {

        Object actualValue =
                valueProvider.apply(
                        expression.columnName()
                );

        return PredicateEvaluator.evaluate(
                actualValue,
                expression.expectedValue(),
                expression.operator()
        );
    }

    /**
     * AND / OR ifadesini recursive değerlendirir.
     */
    private static boolean evaluateLogical(
            LogicalExpression expression,
            Function<String, Object> valueProvider
    ) {

        return switch (expression.operator()) {

            case AND ->
                    evaluateAnd(
                            expression,
                            valueProvider
                    );

            case OR ->
                    evaluateOr(
                            expression,
                            valueProvider
                    );
        };
    }

    /**
     * AND için kısa devre değerlendirmesi yapar.
     */
    private static boolean evaluateAnd(
            LogicalExpression expression,
            Function<String, Object> valueProvider
    ) {

        boolean leftResult =
                evaluate(
                        expression.leftExpression(),
                        valueProvider
                );

        if (!leftResult) {
            return false;
        }

        return evaluate(
                expression.rightExpression(),
                valueProvider
        );
    }

    /**
     * OR için kısa devre değerlendirmesi yapar.
     */
    private static boolean evaluateOr(
            LogicalExpression expression,
            Function<String, Object> valueProvider
    ) {

        boolean leftResult =
                evaluate(
                        expression.leftExpression(),
                        valueProvider
                );

        if (leftResult) {
            return true;
        }

        return evaluate(
                expression.rightExpression(),
                valueProvider
        );
    }

    /**
     * Bir WHERE ifadesini gerçek Row ve Table
     * nesneleri üzerinde değerlendirir.
     *
     * @param expression değerlendirilecek expression
     * @param row değerlendirilecek satır
     * @param table satırın ait olduğu tablo şeması
     * @return koşul sonucu
     */
    public static boolean evaluate(
            Expression expression,
            Row row,
            Table table
    ) {

        Objects.requireNonNull(
                row,
                "Row cannot be null."
        );

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        RowValueProvider valueProvider =
                new RowValueProvider(
                        row,
                        table
                );

        return evaluate(
                expression,
                valueProvider
        );
    }
}
