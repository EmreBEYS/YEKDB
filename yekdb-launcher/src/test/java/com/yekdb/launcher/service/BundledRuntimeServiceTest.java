package com.yekdb.launcher.service;

import com.yekdb.launcher.config.LauncherConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledRuntimeServiceTest {
    @TempDir Path temporaryDirectory;

    @Test void installsBundledCoreOnlyOnce() throws Exception {
        Path bundled = temporaryDirectory.resolve("bundled-yekdb.jar");
        Files.writeString(bundled, "core");
        Path runtime = temporaryDirectory.resolve("runtime");
        LauncherConfig config = new LauncherConfig(
                "owner", "repo", ".*", ".*", ".*", "bin/yekdb.exe", "yekdb.jar", runtime);

        String previous = System.getProperty("yekdb.launcher.bundled-core");
        System.setProperty("yekdb.launcher.bundled-core", bundled.toString());
        try {
            BundledRuntimeService service = new BundledRuntimeService(config);
            assertTrue(service.installIfMissing());
            assertEquals("core", Files.readString(runtime.resolve("yekdb.jar")));
            assertFalse(service.installIfMissing());
        } finally {
            if (previous == null) System.clearProperty("yekdb.launcher.bundled-core");
            else System.setProperty("yekdb.launcher.bundled-core", previous);
        }
    }
}
