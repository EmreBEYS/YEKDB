package com.yekdb.launcher.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class InstallationService {
    public void installZip(Path archive, Path installDirectory, String version) throws IOException {
        Path parent = installDirectory.toAbsolutePath().normalize().getParent();
        if (parent == null) throw new IOException("Geçersiz kurulum dizini");
        Files.createDirectories(parent);
        Path staging = parent.resolve(".yekdb-install-" + UUID.randomUUID());
        Path backup = parent.resolve(".yekdb-backup-" + UUID.randomUUID());
        Files.createDirectories(staging);

        try {
            extractSafely(archive, staging);
            Files.writeString(staging.resolve("version.txt"), version);
            if (Files.exists(installDirectory)) Files.move(installDirectory, backup);
            try {
                Files.move(staging, installDirectory);
                deleteTree(backup);
            } catch (IOException installFailure) {
                if (Files.exists(backup) && !Files.exists(installDirectory)) Files.move(backup, installDirectory);
                throw installFailure;
            }
        } finally {
            deleteTree(staging);
        }
    }

    private void extractSafely(Path archive, Path destination) throws IOException {
        try (InputStream input = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) throw new IOException("Güvensiz ZIP girdisi: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
        }
    }
}
