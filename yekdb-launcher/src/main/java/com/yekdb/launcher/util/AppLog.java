package com.yekdb.launcher.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AppLog {
    private static final long MAX_BYTES = 1024 * 1024;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path DIRECTORY = resolveDirectory();
    private static final Path FILE = DIRECTORY.resolve("launcher.log");

    private AppLog() {
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void error(String message, Throwable error) {
        write("ERROR", message, error);
    }

    public static Path directory() {
        return DIRECTORY;
    }

    public static Path file() {
        return FILE;
    }

    public static String readRecent() {
        try {
            if (!Files.exists(FILE)) return "Henüz log kaydı bulunmuyor.";
            String content = Files.readString(FILE, StandardCharsets.UTF_8);
            int limit = 120_000;
            return content.length() <= limit ? content : "…\n" + content.substring(content.length() - limit);
        } catch (Exception exception) {
            return "Log dosyası okunamadı: " + exception.getMessage();
        }
    }

    private static synchronized void write(String level, String message, Throwable error) {
        try {
            Files.createDirectories(DIRECTORY);
            if (Files.exists(FILE) && Files.size(FILE) >= MAX_BYTES) {
                Files.move(FILE, DIRECTORY.resolve("launcher.log.1"), StandardCopyOption.REPLACE_EXISTING);
            }
            StringBuilder line = new StringBuilder()
                    .append('[').append(TIME.format(LocalDateTime.now())).append("] ")
                    .append(level).append(" - ").append(message == null ? "" : message)
                    .append(System.lineSeparator());
            if (error != null) {
                StringWriter stack = new StringWriter();
                error.printStackTrace(new PrintWriter(stack));
                line.append(stack).append(System.lineSeparator());
            }
            Files.writeString(FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Logging must never prevent the launcher from opening.
        }
    }

    private static Path resolveDirectory() {
        String override = System.getProperty("yekdb.launcher.state-dir");
        if (override != null && !override.isBlank()) return Path.of(override).resolve("logs");
        String localAppData = System.getenv("LOCALAPPDATA");
        Path root = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".yekdb")
                : Path.of(localAppData, "YEKDB");
        return root.resolve("logs");
    }
}
