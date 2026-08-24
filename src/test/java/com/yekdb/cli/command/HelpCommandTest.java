package com.yekdb.cli.command;

import com.yekdb.cli.CliCommandRegistry;
import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HelpCommandTest {

    @Test
    void shouldShowRegisteredCommands() {
        CliCommandRegistry registry =
                new CliCommandRegistry();

        HelpCommand help =
                new HelpCommand(registry);

        registry.register(help);
        registry.register(new VersionCommand());

        CliCommandResult result =
                help.execute(
                        new CliContext(),
                        List.of()
                );

        assertTrue(result.isSuccess());

        assertTrue(
                result.getMessage()
                        .contains("help")
        );

        assertTrue(
                result.getMessage()
                        .contains("version")
        );
    }
}