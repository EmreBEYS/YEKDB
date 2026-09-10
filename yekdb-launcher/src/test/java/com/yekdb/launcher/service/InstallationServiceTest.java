package com.yekdb.launcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallationServiceTest {
    @TempDir Path temporaryDirectory;
    private final InstallationService service = new InstallationService();

    @Test void installsArchiveAndWritesVersion() throws Exception {
        Path archive = zip("valid.zip", "yekdb.jar", "database");
        Path install = temporaryDirectory.resolve("runtime");

        service.installZip(archive, install, "v1.2.3");

        assertTrue(Files.exists(install.resolve("yekdb.jar")));
        assertEquals("v1.2.3", Files.readString(install.resolve("version.txt")));
    }

    @Test void rejectsEntriesOutsideInstallDirectory() throws Exception {
        Path archive = zip("unsafe.zip", "../outside.txt", "blocked");
        Path install = temporaryDirectory.resolve("runtime");

        assertThrows(IOException.class, () -> service.installZip(archive, install, "v1.0.0"));
        assertTrue(Files.notExists(temporaryDirectory.resolve("outside.txt")));
    }

    private Path zip(String name, String entryName, String content) throws IOException {
        Path archive = temporaryDirectory.resolve(name);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry(entryName));
            output.write(content.getBytes());
            output.closeEntry();
        }
        return archive;
    }
}
