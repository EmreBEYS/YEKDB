package com.yekdb.launcher.controller;

import com.yekdb.launcher.config.LauncherConfig;
import com.yekdb.launcher.model.ReleaseInfo;
import com.yekdb.launcher.service.DownloadService;
import com.yekdb.launcher.service.BundledRuntimeService;
import com.yekdb.launcher.service.GitHubReleaseService;
import com.yekdb.launcher.service.VersionService;
import com.yekdb.launcher.service.WindowsUpdateService;
import com.yekdb.launcher.service.YekdbRunnerService;
import com.yekdb.launcher.util.AppLog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public final class LauncherController {
    @FXML private Label statusLabel;
    @FXML private Label currentVersionLabel;
    @FXML private Label latestVersionLabel;
    @FXML private Label releaseDateLabel;
    @FXML private Label updateBadge;
    @FXML private Label activityLabel;
    @FXML private TextArea releaseNotes;
    @FXML private TextArea logArea;
    @FXML private ProgressBar progressBar;
    @FXML private Button launchButton;
    @FXML private Button updateButton;
    @FXML private Button checkButton;
    @FXML private VBox homePage;
    @FXML private VBox releasesPage;
    @FXML private VBox settingsPage;
    @FXML private VBox logsPage;

    private final LauncherConfig config = LauncherConfig.load();
    private final VersionService versions = new VersionService();
    private final WindowsUpdateService windowsUpdater = new WindowsUpdateService();
    private final YekdbRunnerService runner = new YekdbRunnerService(config);
    private ReleaseInfo latestRelease;

    @FXML
    private void initialize() {
        try {
            new BundledRuntimeService(config).installIfMissing();
        } catch (Exception exception) {
            AppLog.error("Birlikte gelen YEKDB kurulamadı", exception);
            activityLabel.setText("Birlikte gelen YEKDB kurulamadı: " + exception.getMessage());
        }
        AppLog.info("Launcher arayüzü hazır. Veri dizini: " + config.installDirectory().resolveSibling("data"));
        refreshInstalledState();
        showPage(homePage);
        checkForUpdates();
    }

    @FXML private void showHome() { showPage(homePage); }
    @FXML private void showReleases() { showPage(releasesPage); }
    @FXML private void showSettings() { showPage(settingsPage); }
    @FXML private void showLogs() { refreshLogs(); showPage(logsPage); }

    @FXML
    private void refreshLogs() {
        logArea.setText(AppLog.readRecent());
        logArea.positionCaret(logArea.getLength());
    }

    @FXML
    private void openLogFolder() {
        try {
            java.nio.file.Files.createDirectories(AppLog.directory());
            new ProcessBuilder("explorer.exe", AppLog.directory().toString()).start();
            AppLog.info("Log klasörü açıldı");
        } catch (Exception exception) {
            AppLog.error("Log klasörü açılamadı", exception);
            activityLabel.setText("Log klasörü açılamadı: " + exception.getMessage());
        }
    }

    @FXML
    private void checkForUpdates() {
        AppLog.info("GitHub Releases güncelleme kontrolü başlatıldı");
        setBusy(true, "GitHub Releases kontrol ediliyor…");
        CompletableFuture.supplyAsync(() -> {
            try { return new GitHubReleaseService(config).fetchLatest(); }
            catch (Exception exception) { throw new RuntimeException(exception); }
        }).whenComplete((release, error) -> Platform.runLater(() -> {
            setBusy(false, error == null ? "Sürüm bilgisi güncellendi" : readableError(error));
            if (error != null) {
                AppLog.error("Güncelleme kontrolü tamamlanamadı", error);
                updateBadge.setText("YEREL SÜRÜM");
                updateBadge.getStyleClass().setAll("status-badge", "badge-current");
                return;
            }
            latestRelease = release;
            AppLog.info("GitHub sürümü bulundu: " + release.version());
            latestVersionLabel.setText(release.version());
            releaseDateLabel.setText(release.publishedAt().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            releaseNotes.setText(release.notes());
            boolean newer = versions.isNewer(release.version(), runner.installedVersion());
            updateBadge.setText(newer ? "GÜNCELLEME HAZIR" : "GÜNCEL");
            updateBadge.getStyleClass().setAll("status-badge", newer ? "badge-update" : "badge-current");
            updateButton.setDisable(!newer || !release.hasDownload());
            if (!release.hasDownload()) activityLabel.setText("Bu işletim sistemi için uygun paket bulunamadı.");
        }));
    }

    @FXML
    private void downloadAndInstall() {
        if (latestRelease == null || !latestRelease.hasDownload()) return;
        setBusy(true, "Güncelleme indiriliyor…");
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        updateButton.setDisable(true);
        AppLog.info("Güncelleme indiriliyor: " + latestRelease.version());
        CompletableFuture.supplyAsync(() -> {
            try {
                Path cache = config.installDirectory().resolveSibling("downloads");
                String safeVersion = latestRelease.version().replaceAll("[^A-Za-z0-9._-]", "_");
                Path setup = cache.resolve("YEKDB-Setup-" + safeVersion + ".exe");
                new DownloadService().download(
                        latestRelease.downloadUri(), setup, latestRelease.downloadSize(), latestRelease.sha256(),
                        value -> Platform.runLater(() -> progressBar.setProgress(value)));
                return setup;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((setup, error) -> Platform.runLater(() -> {
            progressBar.setVisible(false);
            if (error != null) {
                AppLog.error("Güncelleme indirilemedi veya doğrulanamadı", error);
                setBusy(false, readableError(error));
                updateButton.setDisable(false);
                return;
            }
            try {
                AppLog.info("Güncelleme doğrulandı; yükleyici başlatılıyor: " + setup);
                setBusy(false, "Güncelleme doğrulandı. Kurulum açılıyor…");
                windowsUpdater.launchInstaller(setup);
                Platform.exit();
            } catch (Exception exception) {
                AppLog.error("Güncelleme yükleyicisi başlatılamadı", exception);
                setBusy(false, "Güncelleme başlatılamadı: " + exception.getMessage());
                updateButton.setDisable(false);
            }
        }));
    }

    @FXML
    private void launchYekdb() {
        try {
            runner.launch();
            AppLog.info("YEKDB SQL terminali başlatıldı");
            activityLabel.setText("YEKDB SQL terminali ayrı pencerede açıldı");
        } catch (Exception exception) {
            AppLog.error("YEKDB SQL terminali başlatılamadı", exception);
            activityLabel.setText(exception.getMessage());
        }
    }

    private void refreshInstalledState() {
        boolean installed = runner.isInstalled();
        statusLabel.setText(installed ? "Kurulu ve çalışmaya hazır" : "Henüz kurulmadı");
        currentVersionLabel.setText(installed ? runner.installedVersion() : "—");
        launchButton.setDisable(!installed);
    }

    private void setBusy(boolean busy, String message) {
        checkButton.setDisable(busy);
        activityLabel.setText(message);
    }

    private void showPage(VBox visiblePage) {
        for (VBox page : new VBox[]{homePage, releasesPage, settingsPage, logsPage}) {
            boolean visible = page == visiblePage;
            page.setVisible(visible);
            page.setManaged(visible);
        }
    }

    private String readableError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message != null && message.startsWith("GitHub üzerinde henüz")) return message;
        return "İşlem tamamlanamadı: " + message;
    }
}
