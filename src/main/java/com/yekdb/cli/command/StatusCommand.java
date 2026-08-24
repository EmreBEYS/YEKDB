package com.yekdb.cli.command;

import com.yekdb.cli.CliCommand;
import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;

import java.util.List;
import java.util.Objects;

public final class StatusCommand implements CliCommand {

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String description() {
        return "Show current CLI status";
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

        String status = context.isRunning()
                ? "RUNNING"
                : "STOPPED";

        return CliCommandResult.success(
                "CLI status: " + status
        );
    }
}