package com.yekdb.query.evaluator;

import com.yekdb.query.expression.BetweenExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorBetweenTest {

    private ExpressionEvaluator evaluator;
    private Map<String, Object> rowValues;

    @BeforeEach
    void setUp() {

        evaluator =
                new ExpressionEvaluator();

        rowValues =
                new HashMap<>();
    }

    @Test
    void shouldReturnTrueWhenValueIsBetween() {

        rowValues.put(
                "age",
                25
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenValueIsBelowLowerBound() {

        rowValues.put(
                "age",
                17
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenValueIsAboveUpperBound() {

        rowValues.put(
                "age",
                31
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldIncludeLowerBound() {

        rowValues.put(
                "age",
                18
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldIncludeUpperBound() {

        rowValues.put(
                "age",
                30
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldEvaluateNotBetween() {

        rowValues.put(
                "age",
                35
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30,
                        true
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenValueIsInsideNotBetweenRange() {

        rowValues.put(
                "age",
                25
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30,
                        true
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldCompareDifferentNumberTypes() {

        rowValues.put(
                "salary",
                2500L
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "salary",
                        2000,
                        3000.0
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldEvaluateStringBetween() {

        rowValues.put(
                "name",
                "Mehmet"
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "name",
                        "Ali",
                        "Zeynep"
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnDoesNotExist() {

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenActualValueIsNull() {

        rowValues.put(
                "age",
                null
        );

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }
}