package com.yekdb.launcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MacUpdateServiceTest {
    @TempDir Path temporary;

    @Test void resolvesPackagedBundleWithSpaces() throws Exception {
        assertEquals(Path.of("/Applications/My Apps/YEKDB.app").toAbsolutePath().normalize(),
                MacUpdateService.bundlePath("/Applications/My Apps/YEKDB.app/Contents/MacOS/YEKDB"));
    }

    @Test void rejectsUnpackagedOrUnrelatedTargets() {
        assertThrows(IOException.class, () -> MacUpdateService.bundlePath(null));
        assertThrows(IOException.class, () -> MacUpdateService.bundlePath("/tmp/YEKDB"));
        assertThrows(IOException.class, () -> MacUpdateService.bundlePath("/Applications/Other.app/Contents/MacOS/YEKDB"));
    }

    @Test void helperRejectsStagingOutsideApplicationParentWithoutChangingApp() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isExecutable(Path.of("/bin/bash")));
        Path app = Files.createDirectories(temporary.resolve("apps/YEKDB.app"));
        Path marker = app.resolve("original");
        Files.writeString(marker, "original");
        Path stage = Files.createDirectories(temporary.resolve("unrelated"));
        Path script = temporary.resolve("helper.sh");
        try (var input = getClass().getResourceAsStream("/update-macos.sh")) {
            Files.copy(input, script);
        }
        Process helper = new ProcessBuilder("/bin/bash", script.toString(), "999999999",
                app.toString(), stage.toString()).redirectErrorStream(true).start();
        assertTrue(helper.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotEquals(0, helper.exitValue());
        assertEquals("original", Files.readString(marker));
    }

    @Test void helperInstallsCandidateAndRetainsBackup() throws Exception {
        exerciseSwap(true);
    }

    @Test void helperRestoresPreviousBundleWhenReopenFails() throws Exception {
        exerciseSwap(false);
    }

    private void exerciseSwap(boolean opens) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isExecutable(Path.of("/bin/bash")));
        Path app = Files.createDirectories(temporary.resolve("YEKDB.app"));
        Files.writeString(app.resolve("marker"), "old");
        Path stage = Files.createTempDirectory(temporary, ".yekdb-update-");
        Path candidate = Files.createDirectory(stage.resolve("YEKDB.app"));
        Files.writeString(candidate.resolve("marker"), "new");
        Path script = stage.resolve("test-helper.sh");
        try (var input = getClass().getResourceAsStream("/update-macos.sh")) {
            // Exercise actual rename/rollback logic without launching any native application.
            Files.writeString(script, new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("/usr/bin/open", opens ? "/usr/bin/true" : "/usr/bin/false"));
        }
        Process helper = new ProcessBuilder("/bin/bash", script.toString(), "999999999",
                app.toString(), stage.toString()).redirectErrorStream(true).start();
        assertTrue(helper.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(opens ? 0 : 1, helper.exitValue());
        assertEquals(opens ? "new" : "old", Files.readString(app.resolve("marker")));
        assertEquals(opens ? "old" : "new", Files.readString(stage.resolve(
                opens ? "previous.app/marker" : "failed.app/marker")));
    }
}
