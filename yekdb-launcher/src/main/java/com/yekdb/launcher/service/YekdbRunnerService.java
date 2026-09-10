package com.yekdb.launcher.service;

import com.yekdb.launcher.config.LauncherConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public final class YekdbRunnerService {
    private final LauncherConfig config;

    public YekdbRunnerService(LauncherConfig config) {
        this.config = config;
    }

    public boolean isInstalled() {
        return Files.exists(resolveLaunchTarget());
    }

    public String installedVersion() {
        String packagedVersion = System.getProperty("jpackage.app-version");
        if (packagedVersion != null && !packagedVersion.isBlank()) return packagedVersion.trim();
        Path versionFile = config.installDirectory().resolve("version.txt");
        try {
            return Files.exists(versionFile) ? Files.readString(versionFile).trim() : "0.0.0";
        } catch (IOException exception) {
            return "0.0.0";
        }
    }

    public Process launch() throws IOException {
        Path target = resolveLaunchTarget();
        if (!Files.exists(target)) throw new IOException("YEKDB çalıştırma dosyası bulunamadı: " + target);
        List<String> command = launchCommand(target);
        Path workingDirectory = prepareWorkingDirectory();
        return new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .inheritIO()
                .start();
    }

    Path prepareWorkingDirectory() throws IOException {
        Path runtimeDirectory = config.installDirectory().toAbsolutePath().normalize();
        Path workingDirectory = runtimeDirectory.getParent();
        if (workingDirectory == null) workingDirectory = runtimeDirectory;
        copyLegacyDirectory(runtimeDirectory.resolve("data"), workingDirectory.resolve("data"));
        copyLegacyDirectory(runtimeDirectory.resolve("logs"), workingDirectory.resolve("logs"));
        Files.createDirectories(workingDirectory.resolve("data"));
        Files.createDirectories(workingDirectory.resolve("logs"));
        return workingDirectory;
    }

    private void copyLegacyDirectory(Path source, Path destination) throws IOException {
        if (!Files.isDirectory(source) || source.equals(destination)) return;
        try (var paths = Files.walk(source)) {
            for (Path item : paths.toList()) {
                Path target = destination.resolve(source.relativize(item));
                if (Files.isDirectory(item)) Files.createDirectories(target);
                else if (Files.notExists(target)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(item, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    List<String> launchCommand(Path target) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean jar = target.toString().toLowerCase(Locale.ROOT).endsWith(".jar");
        if (!jar && os.contains("win")) {
            return List.of("cmd.exe", "/c", "start", "YEKDB SQL Terminal", target.toString());
        }
        if (!jar && os.contains("mac")) {
            String appleScript = "tell application \"Terminal\" to do script "
                    + appleScriptQuote(shellQuote(target.toString()));
            return List.of("osascript", "-e", appleScript);
        }
        if (!jar) return List.of(target.toString());

        String java = javaExecutable();
        if (os.contains("win")) {
            return List.of("cmd.exe", "/c", "start", "", java, "-jar", target.toString());
        }
        if (os.contains("mac")) {
            String shellCommand = shellQuote(java) + " -jar " + shellQuote(target.toString());
            String appleScript = "tell application \"Terminal\" to do script " + appleScriptQuote(shellCommand);
            return List.of("osascript", "-e", appleScript);
        }
        if (Files.isExecutable(Path.of("/usr/bin/x-terminal-emulator"))) {
            return List.of("/usr/bin/x-terminal-emulator", "-e", java, "-jar", target.toString());
        }
        return List.of(java, "-jar", target.toString());
    }

    public Path resolveLaunchTarget() {
        Path packagedLauncher = packagedEngineLauncher();
        if (packagedLauncher != null) return packagedLauncher;
        Path executable = config.installDirectory().resolve(config.executableName());
        return Files.exists(executable) ? executable : config.installDirectory().resolve(config.jarName());
    }

    private Path packagedEngineLauncher() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null || appPath.isBlank()) return null;

        Path launcherDirectory = Path.of(appPath).toAbsolutePath().normalize().getParent();
        if (launcherDirectory == null) return null;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String launcherName = os.contains("win") ? "YEKDB Engine.exe" : "YEKDB Engine";
        Path candidate = launcherDirectory.resolve(launcherName);
        return Files.isRegularFile(candidate) ? candidate : null;
    }

    private String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private String appleScriptQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
