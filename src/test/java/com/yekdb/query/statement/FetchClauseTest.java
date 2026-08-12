package com.yekdb.query.statement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FetchClauseTest {

    @Test
    void shouldCreateFetchFirst() {

        FetchClause clause =
                new FetchClause(
                        FetchClause.Mode.FIRST,
                        10
                );

        assertEquals(
                FetchClause.Mode.FIRST,
                clause.getMode()
        );

        assertEquals(
                10,
                clause.getRowCount()
        );

        assertTrue(
                clause.isFirst()
        );

        assertFalse(
                clause.isNext()
        );
    }

    @Test
    void shouldCreateFetchNext() {

        FetchClause clause =
                new FetchClause(
                        FetchClause.Mode.NEXT,
                        5
                );

        assertTrue(
                clause.isNext()
        );

        assertFalse(
                clause.isFirst()
        );
    }

    @Test
    void shouldAllowZeroRows() {

        FetchClause clause =
                new FetchClause(
                        FetchClause.Mode.FIRST,
                        0
                );

        assertEquals(
                0,
                clause.getRowCount()
        );
    }

    @Test
    void shouldRejectNegativeValue() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchClause(
                        FetchClause.Mode.FIRST,
                        -1
                )
        );
    }

    @Test
    void shouldRejectNullMode() {

        assertThrows(
                NullPointerException.class,
                () -> new FetchClause(
                        null,
                        10
                )
        );
    }

    @Test
    void shouldCreateCorrectToString() {

        FetchClause clause =
                new FetchClause(
                        FetchClause.Mode.FIRST,
                        10
                );

        assertEquals(
                "FETCH FIRST 10 ROWS ONLY",
                clause.toString()
        );
    }
}