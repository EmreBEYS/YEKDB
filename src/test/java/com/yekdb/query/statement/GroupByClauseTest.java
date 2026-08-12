package com.yekdb.query.statement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupByClauseTest {

    @Test
    void shouldCreateSingleColumnGroupBy() {

        GroupByClause clause =
                new GroupByClause(
                        "department"
                );

        assertEquals(
                List.of(
                        "department"
                ),
                clause.getColumnNames()
        );
    }

    @Test
    void shouldCreateMultipleColumnGroupBy() {

        GroupByClause clause =
                new GroupByClause(
                        List.of(
                                "department",
                                "city"
                        )
                );

        assertEquals(
                2,
                clause.size()
        );
    }

    @Test
    void shouldTrimColumnNames() {

        GroupByClause clause =
                new GroupByClause(
                        List.of(
                                " department ",
                                " city "
                        )
                );

        assertEquals(
                "department",
                clause.getColumnNames()
                        .get(0)
        );

        assertEquals(
                "city",
                clause.getColumnNames()
                        .get(1)
        );
    }

    @Test
    void shouldRejectEmptyColumnList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GroupByClause(
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectNullColumnList() {

        assertThrows(
                NullPointerException.class,
                () -> new GroupByClause(
                        (List<String>) null
                )
        );
    }

    @Test
    void shouldRejectBlankColumn() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GroupByClause(
                        List.of(
                                " "
                        )
                )
        );
    }

    @Test
    void shouldCreateCorrectToString() {

        GroupByClause clause =
                new GroupByClause(
                        List.of(
                                "department",
                                "city"
                        )
                );

        assertEquals(
                "GROUP BY department, city",
                clause.toString()
        );
    }
}