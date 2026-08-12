package com.yekdb.query.command;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectCommandTest {

    // ==================================================
    // LEGACY FACTORIES
    // ==================================================

    @Test
    void allFrom_shouldCreateSelectCommand() {

        SelectCommand command =
                SelectCommand.allFrom(
                        "users"
                );

        assertNotNull(
                command
        );

        assertEquals(
                "users",
                command.getTableName()
        );

        assertNotNull(
                command.getStatement()
        );

        assertEquals(
                List.of("*"),
                command.getSelectedColumns()
        );
    }

    @Test
    void allFromWhere_shouldPreserveWhereExpression() {

        ComparisonExpression where =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        SelectCommand command =
                SelectCommand.allFromWhere(
                        "users",
                        where
                );

        assertEquals(
                "users",
                command.getTableName()
        );

        assertSame(
                where,
                command.getWhereExpression()
        );

        assertTrue(
                command.hasWhereExpression()
        );
    }

    @Test
    void columnsFrom_shouldPreserveSelectedColumns() {

        SelectCommand command =
                SelectCommand.columnsFrom(
                        "users",
                        List.of(
                                "id",
                                "name",
                                "age"
                        )
                );

        assertEquals(
                List.of(
                        "id",
                        "name",
                        "age"
                ),
                command.getSelectedColumns()
        );

        assertEquals(
                3,
                command.getSelectItems().size()
        );
    }

    @Test
    void columnsFromWhere_shouldPreserveColumnsAndWhere() {

        ComparisonExpression where =
                new ComparisonExpression(
                        "active",
                        ComparisonOperator.EQUALS,
                        true
                );

        SelectCommand command =
                SelectCommand.columnsFromWhere(
                        "users",
                        List.of(
                                "id",
                                "name"
                        ),
                        where
                );

        assertEquals(
                List.of(
                        "id",
                        "name"
                ),
                command.getSelectedColumns()
        );

        assertSame(
                where,
                command.getWhereExpression()
        );

        assertTrue(
                command.hasWhereExpression()
        );
    }

    @Test
    void allFromWhere_shouldRejectNullWhereExpression() {

        assertThrows(
                NullPointerException.class,
                () ->
                        SelectCommand.allFromWhere(
                                "users",
                                null
                        )
        );
    }

    @Test
    void columnsFrom_shouldRejectNullColumnList() {

        assertThrows(
                NullPointerException.class,
                () ->
                        SelectCommand.columnsFrom(
                                "users",
                                null
                        )
        );
    }

    @Test
    void columnsFromWhere_shouldRejectNullWhereExpression() {

        assertThrows(
                NullPointerException.class,
                () ->
                        SelectCommand.columnsFromWhere(
                                "users",
                                List.of(
                                        "id"
                                ),
                                null
                        )
        );
    }

    // ==================================================
    // FROM STATEMENT
    // ==================================================

    @Test
    void fromStatement_shouldPreserveStatementInstance() {

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
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertSame(
                statement,
                command.getStatement()
        );
    }

    @Test
    void fromStatement_shouldRejectNullStatement() {

        assertThrows(
                NullPointerException.class,
                () ->
                        SelectCommand.fromStatement(
                                null
                        )
        );
    }

    // ==================================================
    // TABLE
    // ==================================================

    @Test
    void shouldExposeTableReference() {

        TableReference table =
                new TableReference(
                        "employees",
                        "e"
                );

        SelectStatement statement =
                new SelectStatement(
                        table,
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertSame(
                table,
                command.getTable()
        );

        assertEquals(
                "employees",
                command.getTableName()
        );

        assertEquals(
                "e",
                command.getTableAlias()
        );

        assertTrue(
                command.hasTableAlias()
        );
    }

    @Test
    void shouldReportNoTableAlias() {

        SelectCommand command =
                SelectCommand.allFrom(
                        "users"
                );

        assertFalse(
                command.hasTableAlias()
        );
    }

    // ==================================================
    // SELECT ITEMS
    // ==================================================

    @Test
    void shouldExposeSelectItems() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
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
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertEquals(
                2,
                command.getSelectItems().size()
        );

        assertEquals(
                "department",
                command.getSelectItems()
                        .get(0)
                        .getExpression()
        );

        assertEquals(
                "COUNT(*)",
                command.getSelectItems()
                        .get(1)
                        .getExpression()
        );

        assertEquals(
                "employee_count",
                command.getSelectItems()
                        .get(1)
                        .getAlias()
        );
    }

    // ==================================================
    // WHERE
    // ==================================================

    @Test
    void shouldExposeWhereExpression() {

        ComparisonExpression where =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        18
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "users"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        where,
                        null,
                        null,
                        List.of(),
                        null,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertTrue(
                command.hasWhereExpression()
        );

        assertSame(
                where,
                command.getWhereExpression()
        );
    }

    @Test
    void shouldReportMissingWhereExpression() {

        SelectCommand command =
                SelectCommand.allFrom(
                        "users"
                );

        assertFalse(
                command.hasWhereExpression()
        );

        assertNull(
                command.getWhereExpression()
        );
    }

    // ==================================================
    // GROUP BY
    // ==================================================

    @Test
    void shouldPreserveGroupByClause() {

        GroupByClause groupBy =
                new GroupByClause(
                        "department"
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                )
                        ),
                        null,
                        groupBy,
                        null,
                        List.of(),
                        null,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertTrue(
                command.hasGroupBy()
        );

        assertSame(
                groupBy,
                command.getGroupByClause()
        );
    }

    // ==================================================
    // HAVING
    // ==================================================

    @Test
    void shouldPreserveHavingClause() {

        GroupByClause groupBy =
                new GroupByClause(
                        "department"
                );

        HavingClause having =
                new HavingClause(
                        new ComparisonExpression(
                                "employee_count",
                                ComparisonOperator.GREATER_THAN,
                                2
                        )
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
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
                        null,
                        groupBy,
                        having,
                        List.of(),
                        null,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertTrue(
                command.hasHaving()
        );

        assertSame(
                having,
                command.getHavingClause()
        );
    }

    // ==================================================
    // ORDER BY
    // ==================================================

    @Test
    void shouldPreserveOrderByItems() {

        List<OrderByItem> orderBy =
                List.of(
                        new OrderByItem(
                                "age",
                                SortDirection.DESC
                        )
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        orderBy,
                        null,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertTrue(
                command.hasOrderBy()
        );

        assertEquals(
                1,
                command.getOrderByItems().size()
        );

        assertEquals(
                "age",
                command.getOrderByItems()
                        .get(0)
                        .getColumnName()
        );

        assertEquals(
                SortDirection.DESC,
                command.getOrderByItems()
                        .get(0)
                        .getDirection()
        );
    }

    // ==================================================
    // LIMIT
    // ==================================================

    @Test
    void shouldPreserveLimitClause() {

        LimitClause limit =
                new LimitClause(
                        10
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        limit,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertTrue(
                command.hasLimit()
        );

        assertSame(
                limit,
                command.getLimitClause()
        );

        assertEquals(
                10,
                command.getLimitClause()
                        .getRowCount()
        );
    }

    // ==================================================
    // FETCH
    // ==================================================

    @Test
    void shouldPreserveFetchClause() {

        FetchClause fetch =
                new FetchClause(
                        FetchClause.Mode.FIRST,
                        5
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        fetch
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertTrue(
                command.hasFetch()
        );

        assertSame(
                fetch,
                command.getFetchClause()
        );

        assertEquals(
                FetchClause.Mode.FIRST,
                command.getFetchClause()
                        .getMode()
        );

        assertEquals(
                5,
                command.getFetchClause()
                        .getRowCount()
        );
    }

    // ==================================================
    // COMPLETE STATEMENT
    // ==================================================

    @Test
    void shouldPreserveCompleteAdvancedSelectStatement() {

        ComparisonExpression where =
                new ComparisonExpression(
                        "active",
                        ComparisonOperator.EQUALS,
                        true
                );

        GroupByClause groupBy =
                new GroupByClause(
                        "department"
                );

        HavingClause having =
                new HavingClause(
                        new ComparisonExpression(
                                "employee_count",
                                ComparisonOperator.GREATER_THAN,
                                1
                        )
                );

        LimitClause limit =
                new LimitClause(
                        3
                );

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
                        where,
                        groupBy,
                        having,
                        List.of(
                                new OrderByItem(
                                        "employee_count",
                                        SortDirection.DESC
                                )
                        ),
                        limit,
                        null
                );

        SelectCommand command =
                SelectCommand.fromStatement(
                        statement
                );

        assertSame(
                statement,
                command.getStatement()
        );

        assertEquals(
                "employees",
                command.getTableName()
        );

        assertEquals(
                "e",
                command.getTableAlias()
        );

        assertTrue(
                command.hasWhereExpression()
        );

        assertTrue(
                command.hasGroupBy()
        );

        assertTrue(
                command.hasHaving()
        );

        assertTrue(
                command.hasOrderBy()
        );

        assertTrue(
                command.hasLimit()
        );

        assertFalse(
                command.hasFetch()
        );
    }

    // ==================================================
    // TO STRING
    // ==================================================

    @Test
    void toString_shouldContainStatementSummary() {

        SelectCommand command =
                SelectCommand.allFrom(
                        "users"
                );

        String result =
                command.toString();

        assertNotNull(
                result
        );

        assertTrue(
                result.contains(
                        "SelectCommand"
                )
        );

        assertTrue(
                result.contains(
                        "statement="
                )
        );

        assertTrue(
                result.contains(
                        "users"
                )
        );
    }
}