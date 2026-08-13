package com.yekdb.query.parser;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlParserJoinTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {

        parser =
                new SqlParser();
    }

    // --------------------------------------------------
    // TEST 1
    // BASIC INNER JOIN
    // --------------------------------------------------

    @Test
    void shouldParseBasicInnerJoin() {

        Statement parsed =
                parser.parse(
                        """
                        SELECT e.name, d.name
                        FROM employee e
                        INNER JOIN department d
                        ON e.department_id = d.id;
                        """
                );

        assertInstanceOf(
                SelectStatement.class,
                parsed
        );

        SelectStatement statement =
                (SelectStatement) parsed;

        assertTrue(
                statement.hasJoins()
        );

        assertEquals(
                1,
                statement.getJoinCount()
        );

        JoinClause join =
                statement.getJoins()
                        .get(0);

        assertEquals(
                JoinType.INNER,
                join.getJoinType()
        );

        assertEquals(
                "department",
                join.getTableName()
        );

        assertEquals(
                "d",
                join.getAlias()
        );
    }

    // --------------------------------------------------
    // TEST 2
    // JOIN SHORTHAND
    // --------------------------------------------------

    @Test
    void shouldParseJoinAsInnerJoin() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        """
                        SELECT e.name, d.name
                        FROM employee e
                        JOIN department d
                        ON e.department_id = d.id;
                        """
                );

        assertEquals(
                1,
                statement.getJoinCount()
        );

        assertEquals(
                JoinType.INNER,
                statement.getJoins()
                        .get(0)
                        .getJoinType()
        );
    }

    // --------------------------------------------------
    // TEST 3
    // AS ALIAS
    // --------------------------------------------------

    @Test
    void shouldParseJoinTableAliasWithAs() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        """
                        SELECT e.name, d.name
                        FROM employee AS e
                        INNER JOIN department AS d
                        ON e.department_id = d.id;
                        """
                );

        assertEquals(
                "employee",
                statement.getTableName()
        );

        assertEquals(
                "e",
                statement.getTableAlias()
        );

        JoinClause join =
                statement.getJoins()
                        .get(0);

        assertEquals(
                "department",
                join.getTableName()
        );

        assertEquals(
                "d",
                join.getAlias()
        );
    }

    // --------------------------------------------------
    // TEST 4
    // ON CONDITION
    // --------------------------------------------------

    @Test
    void shouldParseJoinOnColumnComparison() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        """
                        SELECT e.name
                        FROM employee e
                        INNER JOIN department d
                        ON e.department_id = d.id;
                        """
                );

        Expression condition =
                statement.getJoins()
                        .get(0)
                        .getCondition();

        assertInstanceOf(
                ComparisonExpression.class,
                condition
        );

        ComparisonExpression comparison =
                (ComparisonExpression) condition;

        assertEquals(
                ComparisonOperator.EQUALS,
                comparison.operator()
        );

        assertTrue(
                comparison.isColumnToColumnComparison()
        );

        ColumnExpression left =
                comparison.getLeftColumnExpression();

        ColumnExpression right =
                comparison.getRightColumnExpression();

        assertNotNull(
                left
        );

        assertNotNull(
                right
        );

        assertEquals(
                "e",
                left.getQualifier()
        );

        assertEquals(
                "department_id",
                left.getColumnName()
        );

        assertEquals(
                "d",
                right.getQualifier()
        );

        assertEquals(
                "id",
                right.getColumnName()
        );
    }

    // --------------------------------------------------
    // TEST 5
    // JOIN + WHERE
    // --------------------------------------------------

    @Test
    void shouldParseJoinWithWhereClause() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        """
                        SELECT e.name, d.name
                        FROM employee e
                        INNER JOIN department d
                        ON e.department_id = d.id
                        WHERE d.name = 'IT';
                        """
                );

        assertTrue(
                statement.hasJoins()
        );

        assertEquals(
                1,
                statement.getJoinCount()
        );

        assertNotNull(
                statement.getWhereExpression()
        );
    }

    // --------------------------------------------------
    // TEST 6
    // MISSING ON
    // --------------------------------------------------

    @Test
    void shouldRejectJoinWithoutOnClause() {

        ParserException exception =
                assertThrows(
                        ParserException.class,
                        () ->
                                parser.parse(
                                        """
                                        SELECT e.name
                                        FROM employee e
                                        INNER JOIN department d;
                                        """
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Expected ON"
                        )
        );
    }

    // --------------------------------------------------
    // TEST 7
    // INVALID ON CONDITION
    // --------------------------------------------------

    @Test
    void shouldRejectJoinWithInvalidOnCondition() {

        ParserException exception =
                assertThrows(
                        ParserException.class,
                        () ->
                                parser.parse(
                                        """
                                        SELECT e.name
                                        FROM employee e
                                        INNER JOIN department d
                                        ON e.department_id > d.id;
                                        """
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Expected '='"
                        )
        );
    }

    // --------------------------------------------------
    // TEST 8
    // MULTIPLE JOIN NOT SUPPORTED IN 00-15
    // --------------------------------------------------

    @Test
    void shouldRejectMultipleJoinsInSprint0015() {

        ParserException exception =
                assertThrows(
                        ParserException.class,
                        () ->
                                parser.parse(
                                        """
                                        SELECT e.name, d.name
                                        FROM employee e
                                        INNER JOIN department d
                                        ON e.department_id = d.id
                                        INNER JOIN location l
                                        ON d.location_id = l.id;
                                        """
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "exactly one JOIN"
                        )
        );
    }
}