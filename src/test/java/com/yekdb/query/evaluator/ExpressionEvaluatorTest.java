package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorTest {

    private ExpressionEvaluator evaluator;
    private Map<String, Object> row;

    @BeforeEach
    void setUp() {

        evaluator = new ExpressionEvaluator();

        row = new HashMap<>();

        row.put("id", 1);
        row.put("name", "Yunus");
        row.put("age", 21);
        row.put("salary", 50000.0);
        row.put("city", "Malatya");
        row.put("active", true);
    }

    @Test
    void shouldEvaluateEqualsExpression() {

        Expression expression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateNotEqualsExpression() {

        Expression expression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.NOT_EQUALS,
                        "Ankara"
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateGreaterThanExpression() {

        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateLessThanExpression() {

        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.LESS_THAN,
                        30
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateGreaterThanOrEqualsExpression() {

        Expression expression =
                new ComparisonExpression(
                        "salary",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        50000
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateLessThanOrEqualsExpression() {

        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.LESS_THAN_OR_EQUALS,
                        21
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateAndExpression() {

        Expression ageExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression cityExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        Expression expression =
                new LogicalExpression(
                        ageExpression,
                        LogicalOperator.AND,
                        cityExpression
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldReturnFalseForAndExpressionWhenOneSideIsFalse() {

        Expression ageExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression cityExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Ankara"
                );

        Expression expression =
                new LogicalExpression(
                        ageExpression,
                        LogicalOperator.AND,
                        cityExpression
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateOrExpression() {

        Expression ageExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.LESS_THAN,
                        18
                );

        Expression cityExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        Expression expression =
                new LogicalExpression(
                        ageExpression,
                        LogicalOperator.OR,
                        cityExpression
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldEvaluateNotExpression() {

        Expression cityExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Ankara"
                );

        Expression expression =
                new NotExpression(
                        cityExpression
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldCompareDifferentNumericTypes() {

        Expression expression =
                new ComparisonExpression(
                        "salary",
                        ComparisonOperator.EQUALS,
                        50000.0
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        row
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnDoesNotExist() {

        Expression expression =
                new ComparisonExpression(
                        "unknownColumn",
                        ComparisonOperator.EQUALS,
                        1
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> evaluator.evaluate(
                                expression,
                                row
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Column not found")
        );
    }

    @Test
    void shouldThrowExceptionWhenExpressionIsNull() {

        assertThrows(
                NullPointerException.class,
                () -> evaluator.evaluate(
                        null,
                        row
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenRowValuesAreNull() {

        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        assertThrows(
                NullPointerException.class,
                () -> evaluator.evaluate(
                        expression,
                        null
                )
        );
    }
}