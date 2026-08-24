package com.yekdb.cli.command;

import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExitCommandTest {

    @Test
    void shouldStopCliContext() {
        CliContext context =
                new CliContext();

        assertTrue(
                context.isRunning()
        );

        ExitCommand command =
                new ExitCommand();

        CliCommandResult result =
                command.execute(
                        context,
                        List.of()
                );

        assertTrue(result.isSuccess());

        assertFalse(
                context.isRunning()
        );
    }
}