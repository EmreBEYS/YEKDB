package com.yekdb.cli.exception;

public final class UnknownCliCommandException extends CliException {

    public UnknownCliCommandException(String commandName) {
        super("Unknown CLI command: " + commandName);
    }
}