package com.yekdb.query.optimizer;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.BooleanConstantExpression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Query Optimization V2 expression optimizer hazırlık testleri.
 */
class ExpressionOptimizerTest {

    @Test
    void phaseOneOptimizerShouldReturnNullExpressionAsIs() {

        Expression optimized =
                new ExpressionOptimizer()
                        .optimize(
                                null
                        );

        assertNull(
                optimized
        );
    }

    @Test
    void phaseOneOptimizerShouldReturnExistingExpressionAsIs() {

        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression optimized =
                new ExpressionOptimizer()
                        .optimize(
                                expression
                        );

        assertSame(
                expression,
                optimized
        );
    }

    @Test
    void shouldRemoveTruePredicateFromAndExpression() {

        Expression predicate =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression expression =
                new LogicalExpression(
                        predicate,
                        LogicalOperator.AND,
                        new BooleanConstantExpression(
                                true
                        )
                );

        Expression optimized =
                new ExpressionOptimizer()
                        .optimize(
                                expression
                        );

        assertSame(
                predicate,
                optimized
        );
    }

    @Test
    void shouldReduceAndFalseToConstantFalse() {

        Expression expression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                18
                        ),
                        LogicalOperator.AND,
                        new BooleanConstantExpression(
                                false
                        )
                );

        BooleanConstantExpression optimized =
                assertInstanceOf(
                        BooleanConstantExpression.class,
                        new ExpressionOptimizer()
                                .optimize(
                                        expression
                                )
                );

        assertFalse(
                optimized.value()
        );
    }
}
