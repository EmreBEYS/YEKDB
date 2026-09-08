package com.yekdb.query.statement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplainStatementTest {

    @Test
    void legacyConstructorShouldKeepPlanOnlyMode() {

        ExplainStatement statement =
                new ExplainStatement(
                        new SelectStatement(
                                "users",
                                List.of("id")
                        )
                );

        assertEquals(ExplainMode.PLAN, statement.getMode());
        assertFalse(statement.isAnalyze());
    }

    @Test
    void analyzeModeShouldDeclareThatItExecutesTheQuery() {

        ExplainStatement statement =
                new ExplainStatement(
                        new SelectStatement(
                                "users",
                                List.of("id")
                        ),
                        ExplainMode.ANALYZE
                );

        assertEquals(ExplainMode.ANALYZE, statement.getMode());
        assertTrue(statement.isAnalyze());
        assertTrue(statement.getMode().executesQuery());
    }
}
