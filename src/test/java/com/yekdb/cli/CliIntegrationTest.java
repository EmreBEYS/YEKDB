package com.yekdb.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliIntegrationTest {

    @Test
    void shouldExecuteBuiltInCommandsInSingleLifecycle() {
        CliApplication application = new CliApplication();

        assertTrue(application.isRunning());

        CliCommandResult helpResult =
                application.execute("help");

        assertTrue(helpResult.isSuccess());
        assertTrue(helpResult.getMessage().contains("help"));
        assertTrue(helpResult.getMessage().contains("version"));
        assertTrue(helpResult.getMessage().contains("status"));
        assertTrue(helpResult.getMessage().contains("exit"));

        CliCommandResult versionResult =
                application.execute("version");

        assertTrue(versionResult.isSuccess());
        assertTrue(versionResult.getMessage().contains("1.0.0"));

        CliCommandResult statusResult =
                application.execute("status");

        assertTrue(statusResult.isSuccess());
        assertTrue(statusResult.getMessage().contains("RUNNING"));

        CliCommandResult exitResult =
                application.execute("exit");

        assertTrue(exitResult.isSuccess());
        assertFalse(application.isRunning());
    }

    @Test
    void shouldKeepContextStoppedAfterExit() {
        CliApplication application = new CliApplication();

        application.execute("exit");

        assertFalse(application.isRunning());
        assertFalse(application.getContext().isRunning());

        CliCommandResult statusResult =
                application.execute("status");

        assertTrue(statusResult.isSuccess());
        assertTrue(statusResult.getMessage().contains("STOPPED"));
        assertFalse(application.isRunning());
    }

    @Test
    void shouldRecoverFromUnknownCommandWithoutStoppingCli() {
        CliApplication application = new CliApplication();

        CliCommandResult result =
                application.execute("not-a-command");

        assertTrue(result.isError());
        assertTrue(
                result.getMessage()
                        .contains("not-a-command")
        );

        assertTrue(application.isRunning());

        CliCommandResult statusResult =
                application.execute("status");

        assertTrue(statusResult.isSuccess());
        assertTrue(statusResult.getMessage().contains("RUNNING"));
    }

    @Test
    void shouldHandleBlankInputWithoutChangingLifecycle() {
        CliApplication application = new CliApplication();

        CliCommandResult result =
                application.execute("     ");

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().isEmpty());
        assertTrue(application.isRunning());
    }
}
