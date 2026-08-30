package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.FunctionCallExpression;
import com.yekdb.query.expression.FunctionComparisonExpression;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionComparisonExpressionTest {

    private final ExpressionEvaluator evaluator =
            new ExpressionEvaluator();

    @Test
    void shouldCompareLowerFunctionResult() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "LOWER",
                                new ColumnExpression("city")
                        ),
                        ComparisonOperator.EQUALS,
                        "malatya"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "city",
                                "MALATYA"
                        )
                );

        assertTrue(result);
    }

    @Test
    void shouldCompareUpperFunctionResult() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "UPPER",
                                new ColumnExpression("name")
                        ),
                        ComparisonOperator.EQUALS,
                        "YEKDB"
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "name",
                                "yekdb"
                        )
                )
        );
    }

    @Test
    void shouldCompareAbsFunctionResult() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "ABS",
                                new ColumnExpression("balance")
                        ),
                        ComparisonOperator.GREATER_THAN,
                        100
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "balance",
                                -250
                        )
                )
        );
    }

    @Test
    void shouldCompareLengthFunctionResult() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "LENGTH",
                                new ColumnExpression("name")
                        ),
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        5
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "name",
                                "YEKDB"
                        )
                )
        );
    }

    @Test
    void shouldResolveNestedFunctionComparison() {

        FunctionCallExpression trim =
                FunctionCallExpression.of(
                        "TRIM",
                        new ColumnExpression("name")
                );

        FunctionCallExpression length =
                FunctionCallExpression.of(
                        "LENGTH",
                        trim
                );

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        length,
                        ComparisonOperator.EQUALS,
                        5
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "name",
                                "   YEKDB   "
                        )
                )
        );
    }

    @Test
    void shouldReturnFalseWhenFunctionComparisonDoesNotMatch() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "LOWER",
                                new ColumnExpression("city")
                        ),
                        ComparisonOperator.EQUALS,
                        "ankara"
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "city",
                                "MALATYA"
                        )
                )
        );
    }

    @Test
    void shouldCompareFunctionResultWithColumn() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "LENGTH",
                                new ColumnExpression("name")
                        ),
                        ComparisonOperator.EQUALS,
                        new ColumnExpression(
                                "expected_length"
                        )
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "name",
                                "YEKDB",
                                "expected_length",
                                5
                        )
                )
        );
    }

    @Test
    void shouldCompareTwoFunctionResults() {

        FunctionCallExpression left =
                FunctionCallExpression.of(
                        "LENGTH",
                        new ColumnExpression("name")
                );

        FunctionCallExpression right =
                FunctionCallExpression.of(
                        "LENGTH",
                        new ColumnExpression("city")
                );

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        left,
                        ComparisonOperator.EQUALS,
                        right
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        Map.of(
                                "name",
                                "Emre",
                                "city",
                                "Kulp"
                        )
                )
        );
    }
    @Test
    void shouldFailForUnknownFunction() {

        FunctionComparisonExpression expression =
                new FunctionComparisonExpression(
                        FunctionCallExpression.of(
                                "UNKNOWN_FUNCTION",
                                new ColumnExpression("name")
                        ),
                        ComparisonOperator.EQUALS,
                        "value"
                );

        assertThrows(
                RuntimeException.class,
                () -> evaluator.evaluate(
                        expression,
                        Map.of(
                                "name",
                                "YEKDB"
                        )
                )
        );
    }
}