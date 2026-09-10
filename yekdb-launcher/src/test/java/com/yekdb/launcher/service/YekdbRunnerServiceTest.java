package com.yekdb.launcher.service;

import com.yekdb.launcher.config.LauncherConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class YekdbRunnerServiceTest {
    @Test
    void packagedEngineLauncherTakesPriority(@TempDir Path directory) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path launcher = Files.createFile(directory.resolve(windows ? "YEKDB Launcher.exe" : "YEKDB Launcher"));
        Path engine = Files.createFile(directory.resolve(windows ? "YEKDB Engine.exe" : "YEKDB Engine"));
        String previousAppPath = System.getProperty("jpackage.app-path");

        try {
            System.setProperty("jpackage.app-path", launcher.toString());
            LauncherConfig config = new LauncherConfig(
                    "owner", "repo", ".*", ".*", ".*", "bin/yekdb.exe", "yekdb.jar",
                    directory.resolve("runtime"));
            assertEquals(engine, new YekdbRunnerService(config).resolveLaunchTarget());
        } finally {
            if (previousAppPath == null) System.clearProperty("jpackage.app-path");
            else System.setProperty("jpackage.app-path", previousAppPath);
        }
    }

    @Test
    void windowsEngineUsesAVisibleTerminalCommand(@TempDir Path directory) throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"));
        Path engine = Files.createFile(directory.resolve("YEKDB Engine.exe"));
        LauncherConfig config = new LauncherConfig(
                "owner", "repo", ".*", ".*", ".*", "bin/yekdb.exe", "yekdb.jar", directory);

        assertEquals(
                java.util.List.of("cmd.exe", "/c", "start", "YEKDB SQL Terminal", engine.toString()),
                new YekdbRunnerService(config).launchCommand(engine));
    }

    @Test
    void migratesLegacyDataWithoutDeletingTheOriginal(@TempDir Path directory) throws Exception {
        Path runtime = directory.resolve("runtime");
        Path legacyDatabase = runtime.resolve("data/database.ydb");
        Files.createDirectories(legacyDatabase.getParent());
        Files.writeString(legacyDatabase, "preserved");
        LauncherConfig config = new LauncherConfig(
                "owner", "repo", ".*", ".*", ".*", "bin/yekdb.exe", "yekdb.jar", runtime);

        Path workingDirectory = new YekdbRunnerService(config).prepareWorkingDirectory();

        assertEquals(directory, workingDirectory);
        assertEquals("preserved", Files.readString(directory.resolve("data/database.ydb")));
        assertEquals("preserved", Files.readString(legacyDatabase));
    }
}
