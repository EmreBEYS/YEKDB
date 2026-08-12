package com.yekdb.query.statement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitClauseTest {

    @Test
    void shouldCreateLimitClause() {

        LimitClause clause =
                new LimitClause(10);

        assertEquals(
                10,
                clause.getRowCount()
        );
    }

    @Test
    void shouldAllowZero() {

        LimitClause clause =
                new LimitClause(0);

        assertEquals(
                0,
                clause.getRowCount()
        );
    }

    @Test
    void shouldRejectNegativeValue() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new LimitClause(-1)
        );
    }

    @Test
    void shouldCreateCorrectToString() {

        LimitClause clause =
                new LimitClause(5);

        assertEquals(
                "LIMIT 5",
                clause.toString()
        );
    }
}