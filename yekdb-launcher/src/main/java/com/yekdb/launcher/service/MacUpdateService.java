package com.yekdb.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Stages a verified DMG beside the installed bundle, then hands off after exit. */
public final class MacUpdateService {
    public boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    static Path bundlePath(String executable) throws IOException {
        if (executable == null || executable.isBlank()) throw new IOException("Güncelleme kurulu YEKDB.app içinden başlatılmalıdır.");
        Path path = Path.of(executable).toAbsolutePath().normalize();
        Path macos = path.getParent();
        Path contents = macos == null ? null : macos.getParent();
        Path app = contents == null ? null : contents.getParent();
        if (app == null || !macos.getFileName().toString().equals("MacOS")
                || !contents.getFileName().toString().equals("Contents")
                || !app.getFileName().toString().equals("YEKDB.app")) {
            throw new IOException("Geçersiz YEKDB uygulama konumu.");
        }
        return app;
    }

    public Path prepare(Path dmg, String version) throws IOException, InterruptedException {
        if (!isSupported()) throw new IOException("macOS gerekli.");
        String expected = version.replaceFirst("^[vV]", "");
        if (!expected.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) throw new IOException("Geçersiz sürüm.");
        Path app = bundlePath(System.getProperty("jpackage.app-path")).toRealPath();
        if (!Files.isWritable(app.getParent()) || !Files.isWritable(app)) {
            throw new IOException("Uygulama klasörü yazılabilir değil. YEKDB.app'i yazılabilir Applications klasörüne taşıyın.");
        }
        boolean engineRunning = ProcessHandle.allProcesses().anyMatch(p -> p.pid() != ProcessHandle.current().pid()
                && p.info().command().map(c -> c.startsWith(app + "/")).orElse(false));
        if (engineRunning) throw new IOException("Güncellemeden önce açık YEKDB SQL terminallerini kapatın.");

        Path stage = Files.createTempDirectory(app.getParent(), ".yekdb-update-");
        Path mount = Files.createTempDirectory("yekdb-update-mount-");
        boolean attached = false;
        try {
            run("/usr/bin/hdiutil", "attach", "-readonly", "-nobrowse", "-noautoopen", "-mountpoint", mount.toString(), dmg.toAbsolutePath().toString());
            attached = true;
            Path candidate = mount.resolve("YEKDB.app");
            Path plist = candidate.resolve("Contents/Info.plist");
            if (!run("/usr/libexec/PlistBuddy", "-c", "Print :CFBundleIdentifier", plist.toString()).trim().equals("com.yekdb.app")
                    || !run("/usr/libexec/PlistBuddy", "-c", "Print :CFBundleShortVersionString", plist.toString()).trim().equals(expected)) {
                throw new IOException("Paket kimliği veya sürümü beklenen değerle eşleşmiyor.");
            }
            run("/usr/bin/codesign", "--verify", "--deep", "--strict", candidate.toString());
            String arch = System.getProperty("os.arch").equals("aarch64") ? "arm64" : "x86_64";
            run("/usr/bin/lipo", "-verify_arch", arch, candidate.resolve("Contents/MacOS/YEKDB").toString());
            if (!Files.isRegularFile(candidate.resolve("Contents/app/yekdb.jar"))) throw new IOException("Çekirdek JAR pakette yok.");
            run("/usr/bin/ditto", candidate.toString(), stage.resolve("YEKDB.app").toString());
            run("/usr/bin/codesign", "--verify", "--deep", "--strict", stage.resolve("YEKDB.app").toString());
            try (var script = MacUpdateService.class.getResourceAsStream("/update-macos.sh")) {
                if (script == null) throw new IOException("Güncelleme yardımcısı bulunamadı.");
                Files.copy(script, stage.resolve("update.sh"));
            }
            Files.writeString(stage.resolve("target.txt"), app.toString());
            return stage;
        } finally {
            if (attached) run("/usr/bin/hdiutil", "detach", mount.toString());
            // Failed staging is retained for diagnosis; the installed app is untouched.
        }
    }

    public Process install(Path stage) throws IOException {
        Path target = Path.of(Files.readString(stage.resolve("target.txt")));
        return new ProcessBuilder("/bin/bash", stage.resolve("update.sh").toString(),
                Long.toString(ProcessHandle.current().pid()), target.toString(), stage.toString())
                .redirectErrorStream(true).redirectOutput(stage.resolve("install.log").toFile()).start();
    }

    private String run(String... command) throws IOException, InterruptedException {
        Path log = Files.createTempFile("yekdb-update-command-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Güncelleme komutu zaman aşımına uğradı: " + command[0]);
            }
            String output = Files.readString(log);
            if (process.exitValue() != 0) throw new IOException(command[0] + ": " + output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
    }
}
