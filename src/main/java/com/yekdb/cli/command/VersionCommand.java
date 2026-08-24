package com.yekdb.cli.command;

import com.yekdb.cli.CliCommand;
import com.yekdb.cli.CliCommandResult;
import com.yekdb.cli.CliContext;

import java.util.List;

public final class VersionCommand implements CliCommand {

    private static final String VERSION = "00-22";

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Show YEKDB CLI version";
    }

    @Override
    public CliCommandResult execute(
            CliContext context,
            List<String> arguments
    ) {
        return CliCommandResult.success(
                "YEKDB CLI " + VERSION
        );
    }
}