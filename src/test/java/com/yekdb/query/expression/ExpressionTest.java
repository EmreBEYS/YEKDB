package com.yekdb.query.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Query expression modellerinin birim testleri.
 */
class ExpressionTest {

    @Test
    void comparisonOperator_fromSymbolShouldReturnCorrectOperator() {
        assertEquals(
                ComparisonOperator.EQUALS,
                ComparisonOperator.fromSymbol("=")
        );

        assertEquals(
                ComparisonOperator.NOT_EQUALS,
                ComparisonOperator.fromSymbol("!=")
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                ComparisonOperator.fromSymbol(">")
        );

        assertEquals(
                ComparisonOperator.LESS_THAN,
                ComparisonOperator.fromSymbol("<")
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN_OR_EQUALS,
                ComparisonOperator.fromSymbol(">=")
        );

        assertEquals(
                ComparisonOperator.LESS_THAN_OR_EQUALS,
                ComparisonOperator.fromSymbol("<=")
        );
    }

    @Test
    void comparisonOperator_shouldReturnItsSymbol() {
        assertEquals(
                "=",
                ComparisonOperator.EQUALS.getSymbol()
        );

        assertEquals(
                "!=",
                ComparisonOperator.NOT_EQUALS.getSymbol()
        );

        assertEquals(
                ">",
                ComparisonOperator.GREATER_THAN.getSymbol()
        );

        assertEquals(
                "<",
                ComparisonOperator.LESS_THAN.getSymbol()
        );

        assertEquals(
                ">=",
                ComparisonOperator.GREATER_THAN_OR_EQUALS.getSymbol()
        );

        assertEquals(
                "<=",
                ComparisonOperator.LESS_THAN_OR_EQUALS.getSymbol()
        );
    }

    @Test
    void comparisonOperator_shouldRejectUnsupportedSymbol() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> ComparisonOperator.fromSymbol("<>")
                );

        assertTrue(
                exception.getMessage()
                        .contains("Desteklenmeyen")
        );
    }

    @Test
    void comparisonOperator_shouldRejectBlankSymbol() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ComparisonOperator.fromSymbol("   ")
        );
    }

    @Test
    void logicalOperator_fromValueShouldReturnCorrectOperator() {
        assertEquals(
                LogicalOperator.AND,
                LogicalOperator.fromValue("AND")
        );

        assertEquals(
                LogicalOperator.AND,
                LogicalOperator.fromValue("and")
        );

        assertEquals(
                LogicalOperator.OR,
                LogicalOperator.fromValue("OR")
        );

        assertEquals(
                LogicalOperator.OR,
                LogicalOperator.fromValue("or")
        );
    }

    @Test
    void logicalOperator_shouldRejectUnsupportedValue() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> LogicalOperator.fromValue("XOR")
                );

        assertTrue(
                exception.getMessage()
                        .contains("Desteklenmeyen")
        );
    }

    @Test
    void comparisonExpression_shouldStoreValuesCorrectly() {
        ComparisonExpression expression =
                new ComparisonExpression(
                        " age ",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        assertEquals(
                "age",
                expression.columnName()
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                expression.operator()
        );

        assertEquals(
                18,
                expression.expectedValue()
        );
    }

    @Test
    void comparisonExpression_shouldRejectBlankColumnName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ComparisonExpression(
                        "   ",
                        ComparisonOperator.EQUALS,
                        1
                )
        );
    }

    @Test
    void comparisonExpression_shouldRejectNullOperator() {
        assertThrows(
                NullPointerException.class,
                () -> new ComparisonExpression(
                        "age",
                        null,
                        18
                )
        );
    }

    @Test
    void logicalExpression_shouldStoreChildExpressionsCorrectly() {
        Expression left =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression right =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        LogicalExpression logicalExpression =
                new LogicalExpression(
                        left,
                        LogicalOperator.AND,
                        right
                );

        assertEquals(
                left,
                logicalExpression.leftExpression()
        );

        assertEquals(
                LogicalOperator.AND,
                logicalExpression.operator()
        );

        assertEquals(
                right,
                logicalExpression.rightExpression()
        );
    }

    @Test
    void logicalExpression_shouldRejectNullLeftExpression() {
        Expression right =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        assertThrows(
                NullPointerException.class,
                () -> new LogicalExpression(
                        null,
                        LogicalOperator.AND,
                        right
                )
        );
    }

    @Test
    void logicalExpression_shouldRejectNullOperator() {
        Expression left =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression right =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        assertThrows(
                NullPointerException.class,
                () -> new LogicalExpression(
                        left,
                        null,
                        right
                )
        );
    }

    @Test
    void logicalExpression_shouldRejectNullRightExpression() {
        Expression left =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        assertThrows(
                NullPointerException.class,
                () -> new LogicalExpression(
                        left,
                        LogicalOperator.AND,
                        null
                )
        );
    }

    @Test
    void notExpression_shouldStoreExpressionCorrectly() {
        Expression comparisonExpression =
                new ComparisonExpression(
                        "active",
                        ComparisonOperator.EQUALS,
                        true
                );

        NotExpression notExpression =
                new NotExpression(
                        comparisonExpression
                );

        assertEquals(
                comparisonExpression,
                notExpression.expression()
        );
    }

    @Test
    void notExpression_shouldRejectNullExpression() {
        assertThrows(
                NullPointerException.class,
                () -> new NotExpression(null)
        );
    }
}