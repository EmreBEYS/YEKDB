package com.yekdb.query.parser;

import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.query.statement.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlParserOrderByTest {

    private final SqlParser parser =
            new SqlParser();

    @Test
    void shouldParseOrderByWithDefaultAscending() {

        Statement statement =
                parser.parse(
                        "SELECT * FROM users ORDER BY age;"
                );

        assertInstanceOf(
                SelectStatement.class,
                statement
        );

        SelectStatement selectStatement =
                (SelectStatement) statement;

        assertTrue(
                selectStatement.hasOrderBy()
        );

        assertEquals(
                1,
                selectStatement
                        .getOrderByItems()
                        .size()
        );

        OrderByItem item =
                selectStatement
                        .getOrderByItems()
                        .getFirst();

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
    void shouldParseExplicitAscendingOrder() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT * FROM users ORDER BY age ASC;"
                );

        OrderByItem item =
                statement
                        .getOrderByItems()
                        .getFirst();

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
    void shouldParseDescendingOrder() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT * FROM users ORDER BY age DESC;"
                );

        OrderByItem item =
                statement
                        .getOrderByItems()
                        .getFirst();

        assertEquals(
                "age",
                item.getColumnName()
        );

        assertEquals(
                SortDirection.DESC,
                item.getDirection()
        );
    }

    @Test
    void shouldParseMultipleOrderByItems() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        """
                        SELECT name, department, age
                        FROM users
                        ORDER BY department ASC, age DESC;
                        """
                );

        List<OrderByItem> orderByItems =
                statement.getOrderByItems();

        assertEquals(
                2,
                orderByItems.size()
        );

        assertEquals(
                "department",
                orderByItems.get(0)
                        .getColumnName()
        );

        assertEquals(
                SortDirection.ASC,
                orderByItems.get(0)
                        .getDirection()
        );

        assertEquals(
                "age",
                orderByItems.get(1)
                        .getColumnName()
        );

        assertEquals(
                SortDirection.DESC,
                orderByItems.get(1)
                        .getDirection()
        );
    }

    @Test
    void shouldParseQualifiedColumnInOrderBy() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        """
                        SELECT u.name
                        FROM users u
                        ORDER BY u.name DESC;
                        """
                );

        OrderByItem item =
                statement
                        .getOrderByItems()
                        .getFirst();

        assertEquals(
                "u.name",
                item.getColumnName()
        );

        assertEquals(
                SortDirection.DESC,
                item.getDirection()
        );
    }

    @Test
    void shouldReturnEmptyOrderByListWhenClauseDoesNotExist() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT id, name FROM users;"
                );

        assertFalse(
                statement.hasOrderBy()
        );

        assertTrue(
                statement
                        .getOrderByItems()
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectOrderWithoutBy() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "SELECT * FROM users ORDER age;"
                )
        );
    }

    @Test
    void shouldRejectEmptyOrderByClause() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "SELECT * FROM users ORDER BY;"
                )
        );
    }
}