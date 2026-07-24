package com.yekdb.logs;

import com.yekdb.exception.YekdbException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class YekdbLogger implements AutoCloseable {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Path logFilePath;

    private BufferedWriter writer;

    private boolean initialized;

    private boolean debugEnabled;

    public YekdbLogger(Path logDirectory) {
        Objects.requireNonNull(
                logDirectory,
                "Log directory cannot be null."
        );

        this.logFilePath = logDirectory.resolve("yekdb.log");
        this.debugEnabled = true;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Path parentDirectory = logFilePath.getParent();

            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            writer = Files.newBufferedWriter(
                    logFilePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

            initialized = true;

        } catch (IOException exception) {
            throw new YekdbException(
                    "Logger could not be initialized: "
                            + logFilePath,
                    exception
            );
        }
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public void error(
            String message,
            Throwable throwable
    ) {
        log(LogLevel.ERROR, message, throwable);
    }

    public void debug(String message) {
        if (!debugEnabled) {
            return;
        }

        log(LogLevel.DEBUG, message, null);
    }

    public synchronized void setDebugEnabled(
            boolean debugEnabled
    ) {
        this.debugEnabled = debugEnabled;
    }

    public synchronized boolean isDebugEnabled() {
        return debugEnabled;
    }

    public Path getLogFilePath() {
        return logFilePath;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    private synchronized void log(
            LogLevel level,
            String message,
            Throwable throwable
    ) {
        if (!initialized) {
            initialize();
        }

        String safeMessage =
                message == null ? "null" : message;

        LogMessage logMessage = new LogMessage(
                LocalDateTime.now(),
                level,
                safeMessage
        );

        String formattedMessage =
                format(logMessage);

        writeToConsole(level, formattedMessage);
        writeToFile(formattedMessage);

        if (throwable != null) {
            String stackTrace = stackTraceToString(throwable);

            writeStackTraceToConsole(stackTrace);
            writeToFile(stackTrace);
        }
    }

    private String format(LogMessage logMessage) {
        String timestamp = logMessage
                .getTimestamp()
                .format(DATE_TIME_FORMATTER);

        String level = String.format(
                "%-5s",
                logMessage.getLevel().name()
        );

        return timestamp
                + " ["
                + level
                + "] "
                + logMessage.getMessage();
    }

    private void writeToConsole(
            LogLevel level,
            String formattedMessage
    ) {
        if (level == LogLevel.ERROR) {
            System.err.println(formattedMessage);
            return;
        }

        System.out.println(formattedMessage);
    }

    private void writeStackTraceToConsole(
            String stackTrace
    ) {
        System.err.print(stackTrace);
    }

    private void writeToFile(String content) {
        try {
            writer.write(content);

            if (!content.endsWith(System.lineSeparator())) {
                writer.newLine();
            }

            writer.flush();

        } catch (IOException exception) {
            throw new YekdbException(
                    "Log message could not be written to file.",
                    exception
            );
        }
    }

    private String stackTraceToString(
            Throwable throwable
    ) {
        StringWriter stringWriter = new StringWriter();

        try (PrintWriter printWriter =
                     new PrintWriter(stringWriter)) {

            throwable.printStackTrace(printWriter);
        }

        return stringWriter.toString();
    }

    @Override
    public synchronized void close() {
        if (!initialized) {
            return;
        }

        try {
            writer.flush();
            writer.close();

        } catch (IOException exception) {
            throw new YekdbException(
                    "Logger could not be closed.",
                    exception
            );

        } finally {
            writer = null;
            initialized = false;
        }
    }
}