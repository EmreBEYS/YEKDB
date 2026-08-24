package com.yekdb.cli.command;

import com.yekdb.cli.CliCommand;
import com.yekdb.cli.CliCommandRegistry;
import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;

import java.util.List;
import java.util.Objects;

public final class HelpCommand implements CliCommand {

    private final CliCommandRegistry registry;

    public HelpCommand(CliCommandRegistry registry) {
        this.registry = Objects.requireNonNull(
                registry,
                "registry cannot be null"
        );
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Show available CLI commands";
    }

    @Override
    public CliCommandResult execute(
            CliContext context,
            List<String> arguments
    ) {
        StringBuilder output = new StringBuilder();

        output.append("Available commands:")
                .append(System.lineSeparator());

        for (CliCommand command : registry.commands()) {
            output.append("  ")
                    .append(command.name())
                    .append(" - ")
                    .append(command.description())
                    .append(System.lineSeparator());
        }

        return CliCommandResult.success(
                output.toString().stripTrailing()
        );
    }
}