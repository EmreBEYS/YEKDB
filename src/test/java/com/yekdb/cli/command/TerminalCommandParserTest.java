package com.yekdb.cli.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminalCommandParserTest {

    private final TerminalCommandParser parser =
            new TerminalCommandParser();

    @Test
    void shouldParseHelpCommand() {

        TerminalCommand command =
                parser.parse(
                        "\\help"
                );

        assertEquals(
                TerminalCommandType.HELP,
                command.getType()
        );
    }

    @Test
    void shouldParseHelpAlias() {

        TerminalCommand command =
                parser.parse(
                        "\\h"
                );

        assertEquals(
                TerminalCommandType.HELP,
                command.getType()
        );
    }

    @Test
    void shouldParseQuitAliases() {

        assertEquals(
                TerminalCommandType.QUIT,
                parser.parse("\\q").getType()
        );

        assertEquals(
                TerminalCommandType.QUIT,
                parser.parse("\\quit").getType()
        );
    }

    @Test
    void shouldParseTablesCommand() {

        assertEquals(
                TerminalCommandType.TABLES,
                parser.parse("\\tables").getType()
        );
    }

    @Test
    void shouldParseDescribeCommand() {

        TerminalCommand command =
                parser.parse(
                        "\\describe users"
                );

        assertEquals(
                TerminalCommandType.DESCRIBE,
                command.getType()
        );

        assertEquals(
                "users",
                command.getArgument(0)
        );
    }

    @Test
    void shouldParseDescribeAlias() {

        TerminalCommand command =
                parser.parse(
                        "\\d users"
                );

        assertEquals(
                TerminalCommandType.DESCRIBE,
                command.getType()
        );
    }

    @Test
    void shouldParseHistoryClearArgument() {

        TerminalCommand command =
                parser.parse(
                        "\\history clear"
                );

        assertEquals(
                TerminalCommandType.HISTORY,
                command.getType()
        );

        assertEquals(
                "clear",
                command.getArgument(0)
        );
    }

    @Test
    void shouldReturnUnknownForUnsupportedMetaCommand() {

        TerminalCommand command =
                parser.parse(
                        "\\foo"
                );

        assertEquals(
                TerminalCommandType.UNKNOWN,
                command.getType()
        );
    }
}