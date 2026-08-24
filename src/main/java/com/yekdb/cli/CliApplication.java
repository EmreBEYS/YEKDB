package com.yekdb.cli;

import com.yekdb.cli.command.ExitCommand;
import com.yekdb.cli.command.HelpCommand;
import com.yekdb.cli.command.StatusCommand;
import com.yekdb.cli.command.VersionCommand;
import com.yekdb.cli.exception.CliException;

import java.util.Objects;

public final class CliApplication {

    private final CliContext context;
    private final CliCommandParser parser;
    private final CliCommandRegistry registry;

    public CliApplication() {
        this.context = new CliContext();
        this.parser = new CliCommandParser();
        this.registry = new CliCommandRegistry();

        registerBuiltInCommands();
    }

    CliApplication(
            CliContext context,
            CliCommandParser parser,
            CliCommandRegistry registry
    ) {
        this.context = Objects.requireNonNull(
                context,
                "context cannot be null"
        );

        this.parser = Objects.requireNonNull(
                parser,
                "parser cannot be null"
        );

        this.registry = Objects.requireNonNull(
                registry,
                "registry cannot be null"
        );

        registerBuiltInCommands();
    }

    public CliCommandResult execute(String input) {

        ParsedCliCommand parsedCommand = parser.parse(input);

        if (parsedCommand.getCommandName().isBlank()) {
            return CliCommandResult.success();
        }

        try {
            CliCommand command = registry.require(
                    parsedCommand.getCommandName()
            );

            return command.execute(
                    context,
                    parsedCommand.getArguments()
            );

        } catch (CliException exception) {
            return CliCommandResult.error(
                    exception.getMessage()
            );
        }
    }

    public boolean isRunning() {
        return context.isRunning();
    }

    public CliContext getContext() {
        return context;
    }

    public CliCommandRegistry getRegistry() {
        return registry;
    }

    private void registerBuiltInCommands() {

        registry.register(
                new HelpCommand(registry)
        );

        registry.register(
                new VersionCommand()
        );

        registry.register(
                new StatusCommand()
        );

        registry.register(
                new ExitCommand()
        );
    }
}