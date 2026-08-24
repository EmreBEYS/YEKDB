package com.yekdb.cli;

import java.util.List;

public interface CliCommand {
    String name();

    String description();

    CliCommandResult execute(CliContext context,List<String> arguments);
}
