package com.yekdb.launcher.service;

import com.yekdb.launcher.config.LauncherConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Installs the YEKDB core JAR shipped beside the packaged launcher on first run. */
public final class BundledRuntimeService {
    private final LauncherConfig config;

    public BundledRuntimeService(LauncherConfig config) {
        this.config = config;
    }

    public boolean installIfMissing() throws IOException {
        Path destination = config.installDirectory().resolve(config.jarName());
        if (Files.exists(destination)) return false;

        Path bundledCore = findBundledCore();
        if (bundledCore == null) return false;

        Files.createDirectories(destination.getParent());
        Files.copy(bundledCore, destination, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(config.installDirectory().resolve("version.txt"), bundledVersion());
        return true;
    }

    private Path findBundledCore() {
        List<Path> candidates = new ArrayList<>();
        String override = System.getProperty("yekdb.launcher.bundled-core");
        if (override != null && !override.isBlank()) candidates.add(Path.of(override));

        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path executable = Path.of(appPath).toAbsolutePath().normalize();
            Path executableDirectory = executable.getParent();
            if (executableDirectory != null) {
                candidates.add(executableDirectory.resolve("app").resolve(config.jarName()));
                Path contents = executableDirectory.getParent();
                if (contents != null) candidates.add(contents.resolve("app").resolve(config.jarName()));
            }
        }

        return candidates.stream().filter(Files::isRegularFile).findFirst().orElse(null);
    }

    private String bundledVersion() {
        String version = BundledRuntimeService.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "1.0.0" : version;
    }
}
