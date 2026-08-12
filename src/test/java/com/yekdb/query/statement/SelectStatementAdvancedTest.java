package com.yekdb.query.statement;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectStatementAdvancedTest {

    @Test
    void shouldCreateCompleteSelectStatement() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        new ComparisonExpression(
                                "active",
                                ComparisonOperator.EQUALS,
                                true
                        ),
                        new GroupByClause(
                                "department"
                        ),
                        new HavingClause(
                                new ComparisonExpression(
                                        "employee_count",
                                        ComparisonOperator.GREATER_THAN,
                                        2
                                )
                        ),
                        List.of(
                                new OrderByItem(
                                        "employee_count",
                                        SortDirection.DESC
                                )
                        ),
                        new LimitClause(
                                10
                        ),
                        null
                );

        assertEquals(
                "employees",
                statement.getTableName()
        );

        assertEquals(
                "e",
                statement.getTableAlias()
        );

        assertTrue(
                statement.hasWhereClause()
        );

        assertTrue(
                statement.hasGroupBy()
        );

        assertTrue(
                statement.hasHaving()
        );

        assertTrue(
                statement.hasOrderBy()
        );

        assertTrue(
                statement.hasLimit()
        );

        assertFalse(
                statement.hasFetch()
        );

        assertEquals(
                2,
                statement.getSelectItems()
                        .size()
        );
    }

    @Test
    void shouldSupportFetch() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "users"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        new FetchClause(
                                FetchClause.Mode.FIRST,
                                5
                        )
                );

        assertTrue(
                statement.hasFetch()
        );

        assertFalse(
                statement.hasLimit()
        );

        assertEquals(
                5,
                statement.getFetchClause()
                        .getRowCount()
        );
    }

    @Test
    void shouldRejectLimitAndFetchTogether() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectStatement(
                        new TableReference(
                                "users"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        new LimitClause(10),
                        new FetchClause(
                                FetchClause.Mode.FIRST,
                                5
                        )
                )
        );
    }

    @Test
    void shouldRejectHavingWithoutGroupBy() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        new HavingClause(
                                new ComparisonExpression(
                                        "count",
                                        ComparisonOperator.GREATER_THAN,
                                        2
                                )
                        ),
                        List.of(),
                        null,
                        null
                )
        );
    }

    @Test
    void oldOrderByConstructorShouldStillWork() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "users"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        List.of(
                                new OrderByItem(
                                        "age",
                                        SortDirection.DESC
                                )
                        )
                );

        assertTrue(
                statement.hasOrderBy()
        );

        assertFalse(
                statement.hasGroupBy()
        );

        assertFalse(
                statement.hasLimit()
        );
    }

    @Test
    void oldSelectConstructorShouldStillWork() {

        SelectStatement statement =
                new SelectStatement(
                        "users",
                        List.of(
                                "id",
                                "name"
                        )
                );

        assertEquals(
                "users",
                statement.getTableName()
        );

        assertEquals(
                List.of(
                        "id",
                        "name"
                ),
                statement.getSelectedColumns()
        );

        assertFalse(
                statement.hasWhereClause()
        );

        assertFalse(
                statement.hasOrderBy()
        );

        assertFalse(
                statement.hasGroupBy()
        );

        assertFalse(
                statement.hasHaving()
        );

        assertFalse(
                statement.hasLimit()
        );

        assertFalse(
                statement.hasFetch()
        );
    }
}