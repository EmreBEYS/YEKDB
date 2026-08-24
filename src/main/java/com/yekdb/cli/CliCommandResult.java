package com.yekdb.cli;

import java.util.Objects;

public final class CliCommandResult {

    private final boolean success;
    private final String message;

    private CliCommandResult(boolean success, String message) {
        this.success = success;
        this.message = Objects.requireNonNullElse(message, "");
    }

    public static CliCommandResult success(String message) {
        return new CliCommandResult(true, message);
    }

    public static CliCommandResult error(String message) {
        return new CliCommandResult(false, message);
    }

    public static CliCommandResult success() {
        return new CliCommandResult(true, "");
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}