package com.yekdb.query.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LikeExpressionTest {

    @Test
    void shouldCreateLikeExpression() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.LIKE
                );

        assertEquals(
                "name",
                expression.getColumnName()
        );

        assertEquals(
                "Ali%",
                expression.getPattern()
        );

        assertEquals(
                LikeOperator.LIKE,
                expression.getOperator()
        );
    }

    @Test
    void shouldCreateNotLikeExpression() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.NOT_LIKE
                );

        assertEquals(
                LikeOperator.NOT_LIKE,
                expression.getOperator()
        );
    }

    @Test
    void shouldCreateILikeExpression() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.ILIKE
                );

        assertEquals(
                LikeOperator.ILIKE,
                expression.getOperator()
        );
    }

    @Test
    void shouldCreateNotILikeExpression() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.NOT_ILIKE
                );

        assertEquals(
                LikeOperator.NOT_ILIKE,
                expression.getOperator()
        );
    }

    @Test
    void shouldRejectNullColumnName() {

        assertThrows(
                NullPointerException.class,
                () -> new LikeExpression(
                        null,
                        "Ali%",
                        LikeOperator.LIKE
                )
        );
    }

    @Test
    void shouldRejectBlankColumnName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new LikeExpression(
                        " ",
                        "Ali%",
                        LikeOperator.LIKE
                )
        );
    }

    @Test
    void shouldRejectNullPattern() {

        assertThrows(
                NullPointerException.class,
                () -> new LikeExpression(
                        "name",
                        null,
                        LikeOperator.LIKE
                )
        );
    }

    @Test
    void shouldRejectNullOperator() {

        assertThrows(
                NullPointerException.class,
                () -> new LikeExpression(
                        "name",
                        "Ali%",
                        null
                )
        );
    }

    @Test
    void shouldGenerateLikeString() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.LIKE
                );

        assertEquals(
                "name LIKE 'Ali%'",
                expression.toString()
        );
    }

    @Test
    void shouldGenerateNotLikeString() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.NOT_LIKE
                );

        assertEquals(
                "name NOT LIKE 'Ali%'",
                expression.toString()
        );
    }

    @Test
    void shouldGenerateILikeString() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.ILIKE
                );

        assertEquals(
                "name ILIKE 'ali%'",
                expression.toString()
        );
    }

    @Test
    void shouldGenerateNotILikeString() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.NOT_ILIKE
                );

        assertEquals(
                "name NOT ILIKE 'ali%'",
                expression.toString()
        );
    }
}