package com.yekdb.query.parser;

import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.query.statement.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlParserAdvancedSelectTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    // ==================================================
    // AGGREGATE FUNCTIONS
    // ==================================================

    @Test
    void shouldParseCountStar() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT COUNT(*)
                        FROM employees;
                        """
                );

        assertEquals(
                1,
                statement.getSelectItems().size()
        );

        assertEquals(
                "COUNT(*)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );
    }

    @Test
    void shouldParseCountColumn() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT COUNT(id)
                        FROM employees;
                        """
                );

        assertEquals(
                "COUNT(id)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );
    }

    @Test
    void shouldParseSum() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT SUM(salary)
                        FROM employees;
                        """
                );

        assertEquals(
                "SUM(salary)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );
    }

    @Test
    void shouldParseAverage() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT AVG(salary)
                        FROM employees;
                        """
                );

        assertEquals(
                "AVG(salary)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );
    }

    @Test
    void shouldParseMinimum() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT MIN(age)
                        FROM employees;
                        """
                );

        assertEquals(
                "MIN(age)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );
    }

    @Test
    void shouldParseMaximum() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT MAX(age)
                        FROM employees;
                        """
                );

        assertEquals(
                "MAX(age)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );
    }

    @Test
    void shouldParseQualifiedAggregateColumn() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT SUM(e.salary)
                        FROM employees e;
                        """
                );

        assertEquals(
                "SUM(e.salary)",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );

        assertEquals(
                "e",
                statement.getTableAlias()
        );
    }

    @Test
    void shouldParseAggregateAlias() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT COUNT(*) AS employee_count
                        FROM employees;
                        """
                );

        SelectItem item =
                statement.getSelectItems()
                        .get(0);

        assertEquals(
                "COUNT(*)",
                item.getExpression()
        );

        assertEquals(
                "employee_count",
                item.getAlias()
        );
    }

    @Test
    void shouldRejectSumStar() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        """
                        SELECT SUM(*)
                        FROM employees;
                        """
                )
        );
    }

    // ==================================================
    // GROUP BY
    // ==================================================

    @Test
    void shouldParseSingleGroupByColumn() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT department
                        FROM employees
                        GROUP BY department;
                        """
                );

        assertTrue(
                statement.hasGroupBy()
        );

        GroupByClause clause =
                statement.getGroupByClause();

        assertEquals(
                List.of("department"),
                clause.getColumnNames()
        );
    }

    @Test
    void shouldParseMultipleGroupByColumns() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT department, city
                        FROM employees
                        GROUP BY department, city;
                        """
                );

        assertEquals(
                List.of(
                        "department",
                        "city"
                ),
                statement
                        .getGroupByClause()
                        .getColumnNames()
        );
    }

    @Test
    void shouldParseQualifiedGroupByColumn() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT e.department
                        FROM employees e
                        GROUP BY e.department;
                        """
                );

        assertEquals(
                List.of(
                        "e.department"
                ),
                statement
                        .getGroupByClause()
                        .getColumnNames()
        );
    }

    // ==================================================
    // HAVING
    // ==================================================

    @Test
    void shouldParseHaving() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT department, COUNT(*) AS employee_count
                        FROM employees
                        GROUP BY department
                        HAVING employee_count > 2;
                        """
                );

        assertTrue(
                statement.hasHaving()
        );

        HavingClause havingClause =
                statement.getHavingClause();

        assertNotNull(
                havingClause.getExpression()
        );
    }

    @Test
    void shouldRejectHavingWithoutGroupBy() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        """
                        SELECT COUNT(*) AS employee_count
                        FROM employees
                        HAVING employee_count > 2;
                        """
                )
        );
    }

    // ==================================================
    // ORDER BY
    // ==================================================

    @Test
    void shouldParseOrderByAscendingByDefault() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        ORDER BY age;
                        """
                );

        assertTrue(
                statement.hasOrderBy()
        );

        OrderByItem item =
                statement.getOrderByItems()
                        .get(0);

        assertEquals(
                "age",
                item.getColumnName()
        );

        assertEquals(
                SortDirection.ASC,
                item.getDirection()
        );
    }

    @Test
    void shouldParseOrderByDescending() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        ORDER BY age DESC;
                        """
                );

        assertEquals(
                SortDirection.DESC,
                statement
                        .getOrderByItems()
                        .get(0)
                        .getDirection()
        );
    }

    @Test
    void shouldParseMultipleOrderByItems() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        ORDER BY department ASC, age DESC;
                        """
                );

        assertEquals(
                2,
                statement.getOrderByItems()
                        .size()
        );

        assertEquals(
                "department",
                statement.getOrderByItems()
                        .get(0)
                        .getColumnName()
        );

        assertEquals(
                SortDirection.ASC,
                statement.getOrderByItems()
                        .get(0)
                        .getDirection()
        );

        assertEquals(
                "age",
                statement.getOrderByItems()
                        .get(1)
                        .getColumnName()
        );

        assertEquals(
                SortDirection.DESC,
                statement.getOrderByItems()
                        .get(1)
                        .getDirection()
        );
    }

    // ==================================================
    // LIMIT
    // ==================================================

    @Test
    void shouldParseLimit() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        LIMIT 10;
                        """
                );

        assertTrue(
                statement.hasLimit()
        );

        LimitClause limitClause =
                statement.getLimitClause();

        assertEquals(
                10,
                limitClause.getRowCount()
        );
    }

    @Test
    void shouldParseLimitZero() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        LIMIT 0;
                        """
                );

        assertEquals(
                0,
                statement.getLimitClause()
                        .getRowCount()
        );
    }

    @Test
    void shouldRejectDecimalLimit() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        """
                        SELECT *
                        FROM employees
                        LIMIT 2.5;
                        """
                )
        );
    }

    // ==================================================
    // FETCH
    // ==================================================

    @Test
    void shouldParseFetchFirst() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        FETCH FIRST 5 ROWS ONLY;
                        """
                );

        assertTrue(
                statement.hasFetch()
        );

        FetchClause clause =
                statement.getFetchClause();

        assertEquals(
                FetchClause.Mode.FIRST,
                clause.getMode()
        );

        assertEquals(
                5,
                clause.getRowCount()
        );
    }

    @Test
    void shouldParseFetchNext() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        FETCH NEXT 3 ROWS ONLY;
                        """
                );

        FetchClause clause =
                statement.getFetchClause();

        assertEquals(
                FetchClause.Mode.NEXT,
                clause.getMode()
        );

        assertEquals(
                3,
                clause.getRowCount()
        );
    }

    @Test
    void shouldParseFetchSingularRow() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        FETCH FIRST 1 ROW ONLY;
                        """
                );

        assertEquals(
                1,
                statement.getFetchClause()
                        .getRowCount()
        );
    }

    @Test
    void shouldRejectInvalidFetchMode() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        """
                        SELECT *
                        FROM employees
                        FETCH 5 ROWS ONLY;
                        """
                )
        );
    }

    // ==================================================
    // WHERE + ADVANCED CLAUSES
    // ==================================================

    @Test
    void shouldParseWhereBeforeGroupBy() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT department, COUNT(*) AS employee_count
                        FROM employees
                        WHERE active = true
                        GROUP BY department;
                        """
                );

        assertTrue(
                statement.hasWhereClause()
        );

        assertTrue(
                statement.hasGroupBy()
        );
    }

    @Test
    void shouldParseBetweenBeforeOrderBy() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        WHERE age BETWEEN 18 AND 30
                        ORDER BY age ASC;
                        """
                );

        assertTrue(
                statement.hasWhereClause()
        );

        assertTrue(
                statement.hasOrderBy()
        );
    }

    @Test
    void shouldParseLikeBeforeLimit() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        WHERE name LIKE 'A%'
                        LIMIT 5;
                        """
                );

        assertTrue(
                statement.hasWhereClause()
        );

        assertTrue(
                statement.hasLimit()
        );
    }

    // ==================================================
    // COMPLETE PIPELINE
    // ==================================================

    @Test
    void shouldParseCompleteAdvancedSelect() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT
                            department,
                            COUNT(*) AS employee_count
                        FROM employees e
                        WHERE active = true
                        GROUP BY department
                        HAVING employee_count > 2
                        ORDER BY employee_count DESC
                        LIMIT 10;
                        """
                );

        assertEquals(
                "employees",
                statement.getTableName()
        );

        assertEquals(
                "e",
                statement.getTableAlias()
        );

        assertEquals(
                2,
                statement.getSelectItems()
                        .size()
        );

        assertEquals(
                "department",
                statement.getSelectItems()
                        .get(0)
                        .getExpression()
        );

        assertEquals(
                "COUNT(*)",
                statement.getSelectItems()
                        .get(1)
                        .getExpression()
        );

        assertEquals(
                "employee_count",
                statement.getSelectItems()
                        .get(1)
                        .getAlias()
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
                "department",
                statement.getGroupByClause()
                        .getColumnNames()
                        .get(0)
        );

        assertEquals(
                "employee_count",
                statement.getOrderByItems()
                        .get(0)
                        .getColumnName()
        );

        assertEquals(
                SortDirection.DESC,
                statement.getOrderByItems()
                        .get(0)
                        .getDirection()
        );

        assertEquals(
                10,
                statement.getLimitClause()
                        .getRowCount()
        );
    }

    @Test
    void shouldParseOrderByWithFetch() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM employees
                        ORDER BY age DESC
                        FETCH FIRST 5 ROWS ONLY;
                        """
                );

        assertTrue(
                statement.hasOrderBy()
        );

        assertTrue(
                statement.hasFetch()
        );

        assertFalse(
                statement.hasLimit()
        );
    }

    @Test
    void shouldParseGroupByWithMultipleAggregates() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT
                            department,
                            AVG(salary) AS average_salary,
                            MIN(salary) AS minimum_salary,
                            MAX(salary) AS maximum_salary
                        FROM employees
                        GROUP BY department;
                        """
                );

        assertEquals(
                4,
                statement.getSelectItems()
                        .size()
        );

        assertEquals(
                "AVG(salary)",
                statement.getSelectItems()
                        .get(1)
                        .getExpression()
        );

        assertEquals(
                "MIN(salary)",
                statement.getSelectItems()
                        .get(2)
                        .getExpression()
        );

        assertEquals(
                "MAX(salary)",
                statement.getSelectItems()
                        .get(3)
                        .getExpression()
        );

        assertTrue(
                statement.hasGroupBy()
        );
    }

    // ==================================================
    // BACKWARD COMPATIBILITY
    // ==================================================

    @Test
    void shouldStillParseSimpleSelect() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT id, name
                        FROM users;
                        """
                );

        assertEquals(
                "users",
                statement.getTableName()
        );

        assertEquals(
                2,
                statement.getSelectItems()
                        .size()
        );

        assertFalse(
                statement.hasWhereClause()
        );

        assertFalse(
                statement.hasGroupBy()
        );

        assertFalse(
                statement.hasHaving()
        );

        assertFalse(
                statement.hasOrderBy()
        );

        assertFalse(
                statement.hasLimit()
        );

        assertFalse(
                statement.hasFetch()
        );
    }

    @Test
    void shouldStillParseSelectWildcard() {

        SelectStatement statement =
                parseSelect(
                        """
                        SELECT *
                        FROM users;
                        """
                );

        assertTrue(
                statement.selectsAllColumns()
        );
    }

    // ==================================================
    // HELPER
    // ==================================================

    private SelectStatement parseSelect(
            String sql
    ) {

        Statement statement =
                parser.parse(
                        sql
                );

        assertInstanceOf(
                SelectStatement.class,
                statement
        );

        return (SelectStatement) statement;
    }
}