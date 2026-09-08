package com.yekdb.query.parser;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.statement.ExplainStatement;
import com.yekdb.query.statement.ExplainMode;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.StatementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlParserExplainTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser =
                new SqlParser();
    }

    @Test
    void shouldParseExplainSelectStatement() {

        Statement statement =
                parser.parse(
                        "EXPLAIN SELECT id, name FROM users WHERE id = 10;"
                );

        ExplainStatement explainStatement =
                assertInstanceOf(
                        ExplainStatement.class,
                        statement
                );

        assertEquals(
                StatementType.EXPLAIN,
                explainStatement.getType()
        );

        assertEquals(
                ExplainMode.PLAN,
                explainStatement.getMode()
        );

        assertFalse(
                explainStatement.isAnalyze()
        );

        SelectStatement selectStatement =
                explainStatement.getSelectStatement();

        assertEquals(
                "users",
                selectStatement.getTableName()
        );

        assertEquals(
                2,
                selectStatement.getSelectItems()
                        .size()
        );

        ComparisonExpression whereExpression =
                assertInstanceOf(
                        ComparisonExpression.class,
                        selectStatement.getWhereExpression()
                );

        assertEquals(
                "id",
                whereExpression.columnName()
        );

        assertEquals(
                ComparisonOperator.EQUALS,
                whereExpression.operator()
        );

        assertEquals(
                10,
                whereExpression.expectedValue()
        );
    }

    @Test
    void shouldParseExplainAnalyzeSelectStatement() {

        ExplainStatement statement =
                assertInstanceOf(
                        ExplainStatement.class,
                        parser.parse(
                                "EXPLAIN ANALYZE SELECT id FROM users WHERE id = 10;"
                        )
                );

        assertEquals(
                ExplainMode.ANALYZE,
                statement.getMode()
        );

        assertTrue(
                statement.isAnalyze()
        );

        assertEquals(
                "users",
                statement.getSelectStatement()
                        .getTableName()
        );
    }

    @Test
    void shouldRejectExplainAnalyzeForNonSelectStatement() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "EXPLAIN ANALYZE UPDATE users SET name = 'x';"
                )
        );
    }

    @Test
    void shouldRejectExplainWithoutSelectStatement() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "EXPLAIN UPDATE users SET name = 'x';"
                )
        );
    }

    @Test
    void shouldRejectExplainForInsertAndDeleteStatements() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "EXPLAIN INSERT INTO users (id) VALUES (1);"
                )
        );

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "EXPLAIN DELETE FROM users WHERE id = 1;"
                )
        );
    }

}
