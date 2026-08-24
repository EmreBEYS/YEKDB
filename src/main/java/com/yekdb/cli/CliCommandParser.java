package com.yekdb.cli;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CliCommandParser {

    public ParsedCliCommand parse(String input) {

        if (input == null || input.isBlank()) {
            return new ParsedCliCommand("", List.of());
        }

        String normalized = input.trim();

        String[] parts = normalized.split("\\s+");

        String commandName = parts[0].toLowerCase(Locale.ROOT);

        List<String> arguments = parts.length <= 1
                ? List.of()
                : Arrays.asList(
                Arrays.copyOfRange(parts, 1, parts.length)
        );

        return new ParsedCliCommand(
                commandName,
                arguments
        );
    }
}