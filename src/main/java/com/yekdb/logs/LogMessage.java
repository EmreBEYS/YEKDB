package com.yekdb.logs;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Tek bir log kaydını temsil eden immutable değer nesnesidir.
 */
public final class LogMessage {

    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String message;

    public LogMessage(
            LocalDateTime timestamp,
            LogLevel level,
            String message
    ) {
        this.timestamp = Objects.requireNonNull(
                timestamp,
                "Log timestamp cannot be null."
        );

        this.level = Objects.requireNonNull(
                level,
                "Log level cannot be null."
        );

        this.message = Objects.requireNonNull(
                message,
                "Log message cannot be null."
        );
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "LogMessage{" +
                "timestamp=" + timestamp +
                ", level=" + level +
                ", message='" + message + '\'' +
                '}';
    }
}
