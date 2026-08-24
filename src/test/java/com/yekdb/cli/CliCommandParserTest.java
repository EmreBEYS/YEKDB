package com.yekdb.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliCommandParserTest {

    private final CliCommandParser parser =
            new CliCommandParser();

    @Test
    void shouldParseSimpleCommand() {
        ParsedCliCommand result =
                parser.parse("help");

        assertEquals("help", result.getCommandName());
        assertTrue(result.getArguments().isEmpty());
    }

    @Test
    void shouldNormalizeCommandNameToLowerCase() {
        ParsedCliCommand result =
                parser.parse("VERSION");

        assertEquals(
                "version",
                result.getCommandName()
        );
    }

    @Test
    void shouldParseArguments() {
        ParsedCliCommand result =
                parser.parse("status verbose");

        assertEquals(
                "status",
                result.getCommandName()
        );

        assertEquals(
                1,
                result.argumentCount()
        );

        assertEquals(
                "verbose",
                result.getArguments().get(0)
        );
    }

    @Test
    void shouldHandleMultipleSpaces() {
        ParsedCliCommand result =
                parser.parse("   status     verbose   ");

        assertEquals(
                "status",
                result.getCommandName()
        );

        assertEquals(
                "verbose",
                result.getArguments().get(0)
        );
    }

    @Test
    void shouldHandleBlankInput() {
        ParsedCliCommand result =
                parser.parse("   ");

        assertTrue(
                result.getCommandName().isBlank()
        );

        assertTrue(
                result.getArguments().isEmpty()
        );
    }

    @Test
    void shouldHandleNullInput() {
        ParsedCliCommand result =
                parser.parse(null);

        assertTrue(
                result.getCommandName().isBlank()
        );
    }
}