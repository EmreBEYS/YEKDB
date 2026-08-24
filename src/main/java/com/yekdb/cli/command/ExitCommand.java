package com.yekdb.cli.command;

import com.yekdb.cli.CliCommand;
import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;

import java.util.List;
import java.util.Objects;

public final class ExitCommand implements CliCommand {

    @Override
    public String name() {
        return "exit";
    }

    @Override
    public String description() {
        return "Exit the YEKDB CLI";
    }

    @Override
    public CliCommandResult execute(
            CliContext context,
            List<String> arguments
    ) {
        Objects.requireNonNull(
                context,
                "context cannot be null"
        );

        context.requestExit();

        return CliCommandResult.success(
                "Exiting YEKDB CLI"
        );
    }
}