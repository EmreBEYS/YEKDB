package com.yekdb.query.mapper;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.ExplainCommand;
import com.yekdb.query.parser.SqlParser;
import com.yekdb.query.statement.ExplainStatement;
import com.yekdb.query.statement.ExplainMode;
import com.yekdb.query.statement.Statement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementCommandMapperExplainTest {

    @Test
    void shouldMapExplainStatementToExplainCommand() {

        Statement statement =
                new SqlParser()
                        .parse(
                                "EXPLAIN SELECT id FROM users WHERE id = 10;"
                        );

        ExplainStatement explainStatement =
                assertInstanceOf(
                        ExplainStatement.class,
                        statement
                );

        Command command =
                StatementCommandMapper.map(
                        explainStatement
                );

        ExplainCommand explainCommand =
                assertInstanceOf(
                        ExplainCommand.class,
                        command
                );

        assertEquals(
                "users",
                explainCommand.getSelectStatement()
                        .getTableName()
        );

        assertEquals(
                "id",
                explainCommand.getSelectStatement()
                        .getSelectedColumns()
                        .get(0)
        );
    }

    @Test
    void shouldPreserveAnalyzeModeWhileMapping() {

        ExplainStatement statement =
                assertInstanceOf(
                        ExplainStatement.class,
                        new SqlParser().parse(
                                "EXPLAIN ANALYZE SELECT id FROM users;"
                        )
                );

        ExplainCommand command =
                assertInstanceOf(
                        ExplainCommand.class,
                        StatementCommandMapper.map(statement)
                );

        assertEquals(ExplainMode.ANALYZE, command.getMode());
        assertTrue(command.isAnalyze());
    }
}
