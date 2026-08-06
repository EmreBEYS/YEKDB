package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateEvaluatorTest {

    @Test
    void shouldReturnTrueWhenIntegerIsGreaterThanExpectedValue() {
        boolean result = PredicateEvaluator.evaluate(
                25,
                18,
                ComparisonOperator.GREATER_THAN
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenIntegerIsNotGreaterThanExpectedValue() {
        boolean result = PredicateEvaluator.evaluate(
                16,
                18,
                ComparisonOperator.GREATER_THAN
        );

        assertFalse(result);
    }

    @Test
    void shouldCompareDifferentNumericTypesAsEqual() {
        boolean result = PredicateEvaluator.evaluate(
                18,
                18.0,
                ComparisonOperator.EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenStringsAreEqual() {
        boolean result = PredicateEvaluator.evaluate(
                "Malatya",
                "Malatya",
                ComparisonOperator.EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenStringsAreNotEqual() {
        boolean result = PredicateEvaluator.evaluate(
                "Malatya",
                "Ankara",
                ComparisonOperator.NOT_EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenNumberIsGreaterThanOrEqual() {
        boolean result = PredicateEvaluator.evaluate(
                50_000,
                50_000,
                ComparisonOperator.GREATER_THAN_OR_EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenNumberIsLessThanOrEqual() {
        boolean result = PredicateEvaluator.evaluate(
                10,
                20,
                ComparisonOperator.LESS_THAN_OR_EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenBothValuesAreNull() {
        boolean result = PredicateEvaluator.evaluate(
                null,
                null,
                ComparisonOperator.EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenOnlyActualValueIsNull() {
        boolean result = PredicateEvaluator.evaluate(
                null,
                18,
                ComparisonOperator.EQUALS
        );

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenNullAndNonNullAreNotEqual() {
        boolean result = PredicateEvaluator.evaluate(
                null,
                18,
                ComparisonOperator.NOT_EQUALS
        );

        assertTrue(result);
    }

    @Test
    void shouldThrowExceptionWhenOrderingComparisonContainsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> PredicateEvaluator.evaluate(
                                null,
                                18,
                                ComparisonOperator.GREATER_THAN
                        )
                );

        assertEquals(
                "Sıralama karşılaştırmalarında null değer kullanılamaz.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenValuesCannotBeCompared() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> PredicateEvaluator.evaluate(
                                "18",
                                18,
                                ComparisonOperator.GREATER_THAN
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Değerler karşılaştırılamıyor")
        );
    }

    @Test
    void shouldThrowExceptionWhenOperatorIsNull() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> PredicateEvaluator.evaluate(
                                10,
                                5,
                                null
                        )
                );

        assertEquals(
                "Karşılaştırma operatörü null olamaz.",
                exception.getMessage()
        );
    }
}