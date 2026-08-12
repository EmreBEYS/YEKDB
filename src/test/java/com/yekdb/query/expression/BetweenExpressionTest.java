package com.yekdb.query.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetweenExpressionTest {

    @Test
    void shouldCreateBetweenExpression() {

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertEquals(
                "age",
                expression.getColumnName()
        );

        assertEquals(
                18,
                expression.getLowerBound()
        );

        assertEquals(
                30,
                expression.getUpperBound()
        );

        assertFalse(
                expression.isNegated()
        );
    }

    @Test
    void shouldCreateNotBetweenExpression() {

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30,
                        true
                );

        assertTrue(
                expression.isNegated()
        );
    }

    @Test
    void shouldRejectNullColumnName() {

        assertThrows(
                NullPointerException.class,
                () -> new BetweenExpression(
                        null,
                        18,
                        30
                )
        );
    }

    @Test
    void shouldRejectBlankColumnName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new BetweenExpression(
                        " ",
                        18,
                        30
                )
        );
    }

    @Test
    void shouldGenerateBetweenString() {

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30
                );

        assertEquals(
                "age BETWEEN 18 AND 30",
                expression.toString()
        );
    }

    @Test
    void shouldGenerateNotBetweenString() {

        BetweenExpression expression =
                new BetweenExpression(
                        "age",
                        18,
                        30,
                        true
                );

        assertEquals(
                "age NOT BETWEEN 18 AND 30",
                expression.toString()
        );
    }
}