package com.yekdb.launcher.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppLogTest {
    @TempDir static Path directory;

    @BeforeAll
    static void configureLogDirectory() {
        System.setProperty("yekdb.launcher.state-dir", directory.toString());
    }

    @Test
    void writesAndReadsLauncherLog() {
        AppLog.info("log-test-message");

        assertTrue(Files.exists(AppLog.file()));
        assertTrue(AppLog.readRecent().contains("log-test-message"));
    }
}
