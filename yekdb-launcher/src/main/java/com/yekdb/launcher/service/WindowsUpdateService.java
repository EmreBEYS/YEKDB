package com.yekdb.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class WindowsUpdateService {
    public boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public Process launchInstaller(Path installer) throws IOException {
        if (!isSupported()) throw new IOException("Otomatik kurulum şu anda yalnızca Windows için kullanılabilir.");
        Path executable = installer.toAbsolutePath().normalize();
        if (!Files.isRegularFile(executable)) throw new IOException("Güncelleme yükleyicisi bulunamadı: " + executable);
        return new ProcessBuilder(executable.toString())
                .directory(executable.getParent().toFile())
                .start();
    }
}
