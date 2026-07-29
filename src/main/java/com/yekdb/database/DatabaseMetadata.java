package com.yekdb.database;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Bir YEKDB veritabanına ait meta veri bilgilerini saklar.
 *
 * <p>Bu nesne daha sonra database.meta dosyasına serileştirilir.</p>
 */
public final class DatabaseMetadata {

    public static final String CURRENT_VERSION = "0.0.6";
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final int DEFAULT_PAGE_SIZE = 4096;

    private final String databaseName;
    private final String version;
    private final LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private final String encoding;
    private final int pageSize;

    public DatabaseMetadata(String databaseName) {
        this(
                databaseName,
                CURRENT_VERSION,
                LocalDateTime.now(),
                LocalDateTime.now(),
                DEFAULT_ENCODING,
                DEFAULT_PAGE_SIZE
        );
    }

    public DatabaseMetadata(
            String databaseName,
            String version,
            LocalDateTime createdAt,
            LocalDateTime lastModifiedAt,
            String encoding,
            int pageSize
    ) {
        this.databaseName = validateDatabaseName(databaseName);
        this.version = requireText(version, "Version");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time cannot be null.");
        this.lastModifiedAt = Objects.requireNonNull(lastModifiedAt, "Last modified time cannot be null.");
        this.encoding = requireText(encoding, "Encoding");

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero.");
        }

        this.pageSize = pageSize;
    }

    public void updateLastModifiedAt() {
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public String getEncoding() {
        return encoding;
    }

    public int getPageSize() {
        return pageSize;
    }

    private static String validateDatabaseName(String databaseName) {
        String validatedName = requireText(databaseName, "Database name");

        if (!validatedName.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Database name must begin with a letter and contain " +
                            "only letters, numbers, and underscores."
            );
        }

        return validatedName;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be null or blank."
            );
        }

        return value.trim();
    }

    @Override
    public String toString() {
        return "DatabaseMetadata{" +
                "databaseName='" + databaseName + '\'' +
                ", version='" + version + '\'' +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                ", encoding='" + encoding + '\'' +
                ", pageSize=" + pageSize +
                '}';
    }
}