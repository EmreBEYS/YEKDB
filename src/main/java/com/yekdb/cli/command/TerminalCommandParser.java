package com.yekdb.cli.command;

import java.util.Arrays;
import java.util.List;

public final class TerminalCommandParser {

    public TerminalCommandParser() {
    }

    public TerminalCommand parse(String input) {

        if (input == null || input.isBlank()) {
            return unknownCommand();
        }

        String trimmedInput = input.trim();

        if (!trimmedInput.startsWith("\\")) {
            return unknownCommand();
        }

        String[] parts = trimmedInput.split("\\s+");

        String command = parts[0].toLowerCase();

        List<String> arguments = parts.length > 1
                ? Arrays.asList(
                Arrays.copyOfRange(parts, 1, parts.length)
        )
                : List.of();

        return switch (command) {

            case "\\help", "\\h" ->
                    new TerminalCommand(
                            TerminalCommandType.HELP,
                            arguments
                    );

            case "\\q", "\\quit" ->
                    new TerminalCommand(
                            TerminalCommandType.QUIT,
                            arguments
                    );

            case "\\clear" ->
                    new TerminalCommand(
                            TerminalCommandType.CLEAR,
                            arguments
                    );

            case "\\history" ->
                    new TerminalCommand(
                            TerminalCommandType.HISTORY,
                            arguments
                    );

            case "\\tables" ->
                    new TerminalCommand(
                            TerminalCommandType.TABLES,
                            arguments
                    );

            case "\\describe", "\\d" ->
                    new TerminalCommand(
                            TerminalCommandType.DESCRIBE,
                            arguments
                    );

            default ->
                    unknownCommand();
        };
    }

    private TerminalCommand unknownCommand() {
        return new TerminalCommand(
                TerminalCommandType.UNKNOWN,
                List.of()
        );
    }
}