package com.yekdb.launcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadServiceTest {
    @TempDir Path directory;

    @Test
    void acceptsMatchingSizeAndSha256() throws Exception {
        Path file = directory.resolve("update.exe");
        Files.writeString(file, "yekdb-update");

        assertDoesNotThrow(() -> DownloadService.verify(
                file, 12, "d4ff0f5f9d5b95e9174c003c934325a7d92461826f5013b10ebdbb2194bf5e0c"));
    }

    @Test
    void deletesFileWhenChecksumDoesNotMatch() throws Exception {
        Path file = directory.resolve("unsafe.exe");
        Files.writeString(file, "tampered");

        assertThrows(Exception.class, () -> DownloadService.verify(file, 8, "00"));
        assertFalse(Files.exists(file));
    }

    @Test
    void deletesFileWhenSizeDoesNotMatch() throws Exception {
        Path file = directory.resolve("truncated.exe");
        Files.writeString(file, "short");

        assertThrows(Exception.class, () -> DownloadService.verify(file, 500, ""));
        assertFalse(Files.exists(file));
    }
}
