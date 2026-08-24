package com.yekdb.cli;

import com.yekdb.cli.exception.UnknownCliCommandException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliCommandRegistryTest {

    @Test
    void shouldRegisterCommand() {
        CliCommandRegistry registry =
                new CliCommandRegistry();

        registry.register(new TestCommand());

        assertTrue(
                registry.contains("test")
        );

        assertEquals(
                1,
                registry.size()
        );
    }

    @Test
    void shouldFindCommandCaseInsensitively() {
        CliCommandRegistry registry =
                new CliCommandRegistry();

        registry.register(new TestCommand());

        assertTrue(
                registry.find("TEST").isPresent()
        );
    }

    @Test
    void shouldRequireRegisteredCommand() {
        CliCommandRegistry registry =
                new CliCommandRegistry();

        registry.register(new TestCommand());

        CliCommand command =
                registry.require("test");

        assertEquals(
                "test",
                command.name()
        );
    }

    @Test
    void shouldThrowForUnknownCommand() {
        CliCommandRegistry registry =
                new CliCommandRegistry();

        assertThrows(
                UnknownCliCommandException.class,
                () -> registry.require("unknown")
        );
    }

    @Test
    void shouldReturnEmptyForBlankCommand() {
        CliCommandRegistry registry =
                new CliCommandRegistry();

        assertTrue(
                registry.find("").isEmpty()
        );
    }

    private static final class TestCommand
            implements CliCommand {

        @Override
        public String name() {
            return "test";
        }

        @Override
        public String description() {
            return "Test command";
        }

        @Override
        public CliCommandResult execute(
                CliContext context,
                List<String> arguments
        ) {
            return CliCommandResult.success();
        }
    }
}