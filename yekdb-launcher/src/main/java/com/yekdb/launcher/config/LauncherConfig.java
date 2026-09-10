package com.yekdb.launcher.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public record LauncherConfig(
        String githubOwner,
        String githubRepository,
        String windowsAssetPattern,
        String macAssetPattern,
        String linuxAssetPattern,
        String executableName,
        String jarName,
        Path installDirectory
) {
    public static LauncherConfig load() {
        Properties properties = new Properties();
        try (InputStream stream = LauncherConfig.class.getResourceAsStream("/launcher-config.properties")) {
            if (stream == null) {
                throw new IllegalStateException("launcher-config.properties bulunamadı");
            }
            properties.load(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Launcher ayarları okunamadı", exception);
        }

        String appData = System.getenv("LOCALAPPDATA");
        Path defaultHome = appData == null || appData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".yekdb", "runtime")
                : Path.of(appData, "YEKDB", "runtime");
        String configuredHome = System.getProperty("yekdb.launcher.install-dir");

        return new LauncherConfig(
                required(properties, "github.owner"),
                required(properties, "github.repository"),
                required(properties, "asset.windows"),
                required(properties, "asset.macos"),
                required(properties, "asset.linux"),
                required(properties, "launch.executable"),
                required(properties, "launch.jar"),
                configuredHome == null || configuredHome.isBlank() ? defaultHome : Path.of(configuredHome)
        );
    }

    public String assetPatternForCurrentOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return windowsAssetPattern;
        if (os.contains("mac")) return macAssetPattern;
        return linuxAssetPattern;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Eksik launcher ayarı: " + key);
        }
        return value.trim();
    }
}
