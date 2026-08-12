package com.yekdb.query.evaluator;

import com.yekdb.query.expression.InExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorInTest {

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
    void shouldReturnTrueWhenValueExistsInList() {

        rowValues.put(
                "department",
                "IT"
        );

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR",
                                "Finance"
                        )
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenValueDoesNotExistInList() {

        rowValues.put(
                "department",
                "Sales"
        );

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR",
                                "Finance"
                        )
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnTrueForNotInWhenValueDoesNotExist() {

        rowValues.put(
                "department",
                "Sales"
        );

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR"
                        ),
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
    void shouldReturnFalseForNotInWhenValueExists() {

        rowValues.put(
                "department",
                "IT"
        );

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR"
                        ),
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
                "age",
                25L
        );

        InExpression expression =
                new InExpression(
                        "age",
                        List.of(
                                18,
                                25,
                                30
                        )
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldCompareDoubleAndIntegerValues() {

        rowValues.put(
                "score",
                95.0
        );

        InExpression expression =
                new InExpression(
                        "score",
                        List.of(
                                80,
                                90,
                                95
                        )
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldEvaluateNullValueWhenNullExistsInList() {

        rowValues.put(
                "value",
                null
        );

        InExpression expression =
                new InExpression(
                        "value",
                        List.of(
                                "A",
                                "B"
                        )
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenNullDoesNotMatchValues() {

        rowValues.put(
                "value",
                null
        );

        InExpression expression =
                new InExpression(
                        "value",
                        List.of(
                                "A",
                                "B",
                                "C"
                        )
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnDoesNotExist() {

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR"
                        )
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