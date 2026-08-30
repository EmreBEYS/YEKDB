package com.yekdb.query.parser;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.FunctionCallExpression;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectFunctionProjectionParserTest {

    private final SqlParser parser = new SqlParser();

    @Test
    void shouldParseLowerFunctionProjection() {
        SelectStatement statement = (SelectStatement) parser.parse(
                "SELECT LOWER(name) FROM users;"
        );

        SelectItem item = statement.getSelectItems().get(0);
        assertTrue(item.isFunctionExpression());
        assertEquals("LOWER", item.getFunctionExpression().getFunctionName());
        assertEquals(new ColumnExpression("name"), item.getFunctionExpression().getArguments().get(0));
    }

    @Test
    void shouldParseFunctionAlias() {
        SelectStatement statement = (SelectStatement) parser.parse(
                "SELECT UPPER(city) AS city_upper FROM users;"
        );

        SelectItem item = statement.getSelectItems().get(0);
        assertTrue(item.isFunctionExpression());
        assertEquals("city_upper", item.getAlias());
    }

    @Test
    void shouldParseNestedFunctionProjection() {
        SelectStatement statement = (SelectStatement) parser.parse(
                "SELECT LENGTH(TRIM(name)) FROM users;"
        );

        FunctionCallExpression outer = statement.getSelectItems().get(0).getFunctionExpression();
        assertEquals("LENGTH", outer.getFunctionName());
        assertInstanceOf(FunctionCallExpression.class, outer.getArguments().get(0));
        assertEquals("TRIM", ((FunctionCallExpression) outer.getArguments().get(0)).getFunctionName());
    }

    @Test
    void shouldParseMixedColumnAndFunctionProjection() {
        SelectStatement statement = (SelectStatement) parser.parse(
                "SELECT id, LOWER(name), ABS(balance) FROM users;"
        );

        assertEquals(3, statement.getSelectItems().size());
        assertFalse(statement.getSelectItems().get(0).isFunctionExpression());
        assertTrue(statement.getSelectItems().get(1).isFunctionExpression());
        assertTrue(statement.getSelectItems().get(2).isFunctionExpression());
    }

    @Test
    void shouldParseQualifiedColumnInsideFunction() {
        SelectStatement statement = (SelectStatement) parser.parse(
                "SELECT LOWER(u.name) FROM users u;"
        );

        Object argument = statement.getSelectItems().get(0)
                .getFunctionExpression().getArguments().get(0);

        assertEquals(new ColumnExpression("u", "name"), argument);
    }
}
