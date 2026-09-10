package com.yekdb.launcher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yekdb.launcher.config.LauncherConfig;
import com.yekdb.launcher.model.ReleaseInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

public final class GitHubReleaseService {
    private final LauncherConfig config;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public GitHubReleaseService(LauncherConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), new ObjectMapper());
    }

    GitHubReleaseService(LauncherConfig config, HttpClient client, ObjectMapper objectMapper) {
        this.config = config;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public ReleaseInfo fetchLatest() throws IOException, InterruptedException {
        URI endpoint = URI.create("https://api.github.com/repos/%s/%s/releases/latest"
                .formatted(config.githubOwner(), config.githubRepository()));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "YEKDB-Launcher")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new IOException("GitHub üzerinde henüz bir YEKDB sürümü yayınlanmadı.");
        }
        if (response.statusCode() != 200) {
            throw new IOException("GitHub Releases yanıtı başarısız: HTTP " + response.statusCode());
        }

        JsonNode release = objectMapper.readTree(response.body());
        Optional<JsonNode> asset = findAsset(release.path("assets"), config.assetPatternForCurrentOs());
        JsonNode selectedAsset = asset.orElse(null);
        return new ReleaseInfo(
                release.path("tag_name").asText("0.0.0"),
                release.path("name").asText("YEKDB Release"),
                release.path("body").asText("Sürüm notu bulunmuyor."),
                parseInstant(release.path("published_at").asText()),
                selectedAsset == null ? null : URI.create(selectedAsset.path("browser_download_url").asText()),
                selectedAsset == null ? 0L : selectedAsset.path("size").asLong(),
                selectedAsset == null ? "" : parseSha256(selectedAsset.path("digest").asText(""))
        );
    }

    private Optional<JsonNode> findAsset(JsonNode assets, String expression) {
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        for (JsonNode asset : assets) {
            if (pattern.matcher(asset.path("name").asText()).matches()) return Optional.of(asset);
        }
        return Optional.empty();
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return Instant.EPOCH;
        }
    }

    private String parseSha256(String digest) {
        String prefix = "sha256:";
        return digest.regionMatches(true, 0, prefix, 0, prefix.length())
                ? digest.substring(prefix.length()).trim()
                : "";
    }
}
