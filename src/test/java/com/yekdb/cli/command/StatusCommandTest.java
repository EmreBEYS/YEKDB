package com.yekdb.cli.command;

import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatusCommandTest {

    @Test
    void shouldReportRunningStatus() {
        CliContext context =
                new CliContext();

        StatusCommand command =
                new StatusCommand();

        CliCommandResult result =
                command.execute(
                        context,
                        List.of()
                );

        assertTrue(result.isSuccess());

        assertTrue(
                result.getMessage()
                        .contains("RUNNING")
        );
    }

    @Test
    void shouldReportStoppedStatus() {
        CliContext context =
                new CliContext();

        context.requestExit();

        StatusCommand command =
                new StatusCommand();

        CliCommandResult result =
                command.execute(
                        context,
                        List.of()
                );

        assertTrue(
                result.getMessage()
                        .contains("STOPPED")
        );
    }
}