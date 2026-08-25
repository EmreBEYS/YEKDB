package com.yekdb.cli.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlStatementBufferTest {

    @Test
    void shouldStartEmpty() {

        SqlStatementBuffer buffer =
                new SqlStatementBuffer();

        assertTrue(
                buffer.isEmpty()
        );

        assertFalse(
                buffer.isComplete()
        );
    }

    @Test
    void shouldAppendSingleLineStatement() {

        SqlStatementBuffer buffer =
                new SqlStatementBuffer();

        buffer.append(
                "SELECT * FROM users;"
        );

        assertFalse(
                buffer.isEmpty()
        );

        assertTrue(
                buffer.isComplete()
        );
    }

    @Test
    void shouldRemainIncompleteWithoutSemicolon() {

        SqlStatementBuffer buffer =
                new SqlStatementBuffer();

        buffer.append(
                "SELECT * FROM users"
        );

        assertFalse(
                buffer.isComplete()
        );
    }

    @Test
    void shouldSupportMultiLineStatement() {

        SqlStatementBuffer buffer =
                new SqlStatementBuffer();

        buffer.append(
                "SELECT id, name"
        );

        assertFalse(
                buffer.isComplete()
        );

        buffer.append(
                "FROM users;"
        );

        assertTrue(
                buffer.isComplete()
        );
    }

    @Test
    void shouldConsumeAndClearBuffer() {

        SqlStatementBuffer buffer =
                new SqlStatementBuffer();

        buffer.append(
                "SELECT * FROM users;"
        );

        String sql =
                buffer.consume();

        assertEquals(
                "SELECT * FROM users;",
                sql.trim()
        );

        assertTrue(
                buffer.isEmpty()
        );
    }

    @Test
    void shouldClearBuffer() {

        SqlStatementBuffer buffer =
                new SqlStatementBuffer();

        buffer.append(
                "SELECT *"
        );

        buffer.clear();

        assertTrue(
                buffer.isEmpty()
        );

        assertFalse(
                buffer.isComplete()
        );
    }
}