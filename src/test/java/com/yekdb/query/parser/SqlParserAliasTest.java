package com.yekdb.query.parser;

import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlParserAliasTest {

    private final SqlParser parser =
            new SqlParser();

    @Test
    void shouldParseTableAliasWithAs() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT name FROM department AS d;"
                );

        assertEquals(
                "department",
                statement.getTableName()
        );

        assertTrue(
                statement.hasTableAlias()
        );

        assertEquals(
                "d",
                statement.getTableAlias()
        );
    }

    @Test
    void shouldParseTableAliasWithoutAs() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT name FROM department d;"
                );

        assertEquals(
                "department",
                statement.getTableName()
        );

        assertEquals(
                "d",
                statement.getTableAlias()
        );
    }

    @Test
    void shouldParseColumnAliasWithAs() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT name AS department_name FROM department;"
                );

        List<SelectItem> items =
                statement.getSelectItems();

        assertEquals(
                1,
                items.size()
        );

        SelectItem item =
                items.get(0);

        assertEquals(
                "name",
                item.getExpression()
        );

        assertTrue(
                item.hasAlias()
        );

        assertEquals(
                "department_name",
                item.getAlias()
        );
    }

    @Test
    void shouldParseQualifiedColumn() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT d.name FROM department d;"
                );

        SelectItem item =
                statement.getSelectItems()
                        .get(0);

        assertEquals(
                "d.name",
                item.getExpression()
        );

        assertEquals(
                "d",
                statement.getTableAlias()
        );
    }

    @Test
    void shouldParseQualifiedColumnAndAliases() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT d.name AS department_name " +
                                "FROM department AS d;"
                );

        SelectItem item =
                statement.getSelectItems()
                        .get(0);

        assertEquals(
                "d.name",
                item.getExpression()
        );

        assertEquals(
                "department_name",
                item.getAlias()
        );

        assertEquals(
                "department",
                statement.getTableName()
        );

        assertEquals(
                "d",
                statement.getTableAlias()
        );
    }

    @Test
    void shouldParseMultipleQualifiedColumns() {

        SelectStatement statement =
                (SelectStatement) parser.parse(
                        "SELECT d.id, d.name " +
                                "FROM department d;"
                );

        List<SelectItem> items =
                statement.getSelectItems();

        assertEquals(
                2,
                items.size()
        );

        assertEquals(
                "d.id",
                items.get(0).getExpression()
        );

        assertEquals(
                "d.name",
                items.get(1).getExpression()
        );
    }
}