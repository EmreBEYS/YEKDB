package com.yekdb.query.expression;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InExpressionTest {

    @Test
    void shouldCreateInExpression() {

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR",
                                "Finance"
                        )
                );

        assertEquals(
                "department",
                expression.getColumnName()
        );

        assertEquals(
                List.of(
                        "IT",
                        "HR",
                        "Finance"
                ),
                expression.getValues()
        );

        assertFalse(
                expression.isNegated()
        );
    }

    @Test
    void shouldCreateNotInExpression() {

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
                expression.isNegated()
        );
    }

    @Test
    void shouldRejectNullColumnName() {

        assertThrows(
                NullPointerException.class,
                () -> new InExpression(
                        null,
                        List.of("IT")
                )
        );
    }

    @Test
    void shouldRejectBlankColumnName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new InExpression(
                        " ",
                        List.of("IT")
                )
        );
    }

    @Test
    void shouldRejectNullValues() {

        assertThrows(
                NullPointerException.class,
                () -> new InExpression(
                        "department",
                        null
                )
        );
    }

    @Test
    void shouldRejectEmptyValues() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new InExpression(
                        "department",
                        List.of()
                )
        );
    }

    @Test
    void shouldCopyValuesList() {

        List<Object> values =
                new ArrayList<>();

        values.add("IT");
        values.add("HR");

        InExpression expression =
                new InExpression(
                        "department",
                        values
                );

        values.add("Finance");

        assertEquals(
                2,
                expression.getValues().size()
        );

        assertFalse(
                expression.getValues()
                        .contains("Finance")
        );
    }

    @Test
    void shouldExposeUnmodifiableValues() {

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR"
                        )
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> expression
                        .getValues()
                        .add("Finance")
        );
    }

    @Test
    void shouldGenerateInString() {

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR"
                        )
                );

        assertEquals(
                "department IN [IT, HR]",
                expression.toString()
        );
    }

    @Test
    void shouldGenerateNotInString() {

        InExpression expression =
                new InExpression(
                        "department",
                        List.of(
                                "IT",
                                "HR"
                        ),
                        true
                );

        assertEquals(
                "department NOT IN [IT, HR]",
                expression.toString()
        );
    }
}