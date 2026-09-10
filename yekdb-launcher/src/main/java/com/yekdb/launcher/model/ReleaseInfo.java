package com.yekdb.launcher.model;

import java.net.URI;
import java.time.Instant;

public record ReleaseInfo(
        String version,
        String name,
        String notes,
        Instant publishedAt,
        URI downloadUri,
        long downloadSize,
        String sha256
) {
    public boolean hasDownload() {
        return downloadUri != null;
    }

    public boolean hasChecksum() {
        return sha256 != null && !sha256.isBlank();
    }
}
