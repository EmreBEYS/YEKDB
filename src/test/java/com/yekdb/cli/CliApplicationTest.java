package com.yekdb.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliApplicationTest {

    @Test
    void shouldRegisterBuiltInCommands() {
        CliApplication application =
                new CliApplication();

        assertEquals(
                4,
                application.getRegistry().size()
        );

        assertTrue(
                application.getRegistry()
                        .contains("help")
        );

        assertTrue(
                application.getRegistry()
                        .contains("version")
        );

        assertTrue(
                application.getRegistry()
                        .contains("status")
        );

        assertTrue(
                application.getRegistry()
                        .contains("exit")
        );
    }

    @Test
    void shouldExecuteVersionCommand() {
        CliApplication application =
                new CliApplication();

        CliCommandResult result =
                application.execute("version");

        assertTrue(result.isSuccess());

        assertTrue(
                result.getMessage()
                        .contains("00-22")
        );
    }

    @Test
    void shouldExecuteStatusCommand() {
        CliApplication application =
                new CliApplication();

        CliCommandResult result =
                application.execute("status");

        assertTrue(result.isSuccess());

        assertTrue(
                result.getMessage()
                        .contains("RUNNING")
        );
    }

    @Test
    void shouldReturnErrorForUnknownCommand() {
        CliApplication application =
                new CliApplication();

        CliCommandResult result =
                application.execute("unknown");

        assertTrue(result.isError());

        assertTrue(
                result.getMessage()
                        .contains("unknown")
        );
    }

    @Test
    void shouldIgnoreBlankInput() {
        CliApplication application =
                new CliApplication();

        CliCommandResult result =
                application.execute("   ");

        assertTrue(result.isSuccess());

        assertTrue(
                result.getMessage().isEmpty()
        );
    }

    @Test
    void shouldStopAfterExitCommand() {
        CliApplication application =
                new CliApplication();

        assertTrue(
                application.isRunning()
        );

        CliCommandResult result =
                application.execute("exit");

        assertTrue(result.isSuccess());

        assertFalse(
                application.isRunning()
        );
    }

    @Test
    void shouldBeCaseInsensitive() {
        CliApplication application =
                new CliApplication();

        CliCommandResult result =
                application.execute("VERSION");

        assertTrue(result.isSuccess());
    }
}