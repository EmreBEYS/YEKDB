package com.yekdb.launcher;

import javafx.application.Application;
import com.yekdb.launcher.util.AppLog;

/**
 * Plain Java entry point used by native packages. Keeping the entry point
 * separate prevents the Java launcher from treating the main class as a
 * module-path-only JavaFX application.
 */
public final class LauncherMain {
    private LauncherMain() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                AppLog.error("Beklenmeyen Launcher hatası [" + thread.getName() + "]", error));
        AppLog.info("YEKDB Launcher başlatılıyor. Sürüm: "
                + System.getProperty("jpackage.app-version", "geliştirme"));
        Application.launch(YekdbLauncherApp.class, args);
    }
}
