package com.yekdb.cli.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalSessionTest {

    @Test
    void shouldStartStopped() {

        TerminalSession session =
                new TerminalSession();

        assertFalse(
                session.isRunning()
        );
    }

    @Test
    void shouldStartSession() {

        TerminalSession session =
                new TerminalSession();

        session.start();

        assertTrue(
                session.isRunning()
        );
    }

    @Test
    void shouldStopSession() {

        TerminalSession session =
                new TerminalSession();

        session.start();
        session.stop();

        assertFalse(
                session.isRunning()
        );
    }

    @Test
    void shouldStoreHistoryEntries() {

        TerminalSession session =
                new TerminalSession();

        session.addHistory(
                "SELECT * FROM users;"
        );

        session.addHistory(
                "\\tables"
        );

        assertEquals(
                2,
                session.getHistory().size()
        );

        assertEquals(
                "SELECT * FROM users;",
                session.getHistory().get(0)
        );
    }

    @Test
    void shouldReportHistoryPresence() {

        TerminalSession session =
                new TerminalSession();

        assertFalse(
                session.hasHistory()
        );

        session.addHistory(
                "SELECT 1;"
        );

        assertTrue(
                session.hasHistory()
        );
    }

    @Test
    void shouldClearHistory() {

        TerminalSession session =
                new TerminalSession();

        session.addHistory(
                "SELECT 1;"
        );

        session.clearHistory();

        assertFalse(
                session.hasHistory()
        );

        assertTrue(
                session.getHistory().isEmpty()
        );
    }
}