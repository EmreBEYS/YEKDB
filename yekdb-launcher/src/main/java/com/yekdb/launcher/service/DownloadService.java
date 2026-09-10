package com.yekdb.launcher.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.DoubleConsumer;

public final class DownloadService {
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public Path download(URI uri, Path destination, long expectedSize, String expectedSha256,
                         DoubleConsumer progress) throws IOException, InterruptedException {
        Files.createDirectories(destination.getParent());
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "YEKDB-Launcher")
                .GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("İndirme başarısız: HTTP " + response.statusCode());
        }

        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(partial)) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
                downloaded += read;
                progress.accept(total > 0 ? (double) downloaded / total : -1);
            }
        } catch (IOException exception) {
            Files.deleteIfExists(partial);
            throw exception;
        }
        verify(partial, expectedSize, expectedSha256);
        try {
            Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        progress.accept(1.0);
        return destination;
    }

    static void verify(Path file, long expectedSize, String expectedSha256) throws IOException {
        long actualSize = Files.size(file);
        if (expectedSize > 0 && actualSize != expectedSize) {
            Files.deleteIfExists(file);
            throw new IOException("İndirilen güncellemenin boyutu doğrulanamadı.");
        }
        if (expectedSha256 == null || expectedSha256.isBlank()) return;

        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(expectedSha256.trim())) {
                Files.deleteIfExists(file);
                throw new IOException("Güncelleme güvenlik doğrulamasından geçemedi (SHA-256 uyuşmuyor).");
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 doğrulaması kullanılamıyor.", impossible);
        }
    }
}
