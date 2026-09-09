package com.yekdb.cli.command;

import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionCommandTest {

    @Test
    void shouldReturnVersion() {
        VersionCommand command =
                new VersionCommand();

        CliCommandResult result =
                command.execute(
                        new CliContext(),
                        List.of()
                );

        assertTrue(result.isSuccess());

        assertTrue(
                result.getMessage()
                        .contains("1.0.0")
        );
    }
}
