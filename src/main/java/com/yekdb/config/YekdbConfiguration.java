package com.yekdb.config;

import com.yekdb.exception.ConfigurationException;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/**
 * YEKDB çalışma zamanı yapılandırmasını temsil eden immutable değer nesnesidir.
 */
public final class YekdbConfiguration {

    private final Path dataDirectory;
    private final Path logDirectory;
    private final int pageSize;
    private final Charset charset;
    private final String version;
    private final String databaseFileName;

    public YekdbConfiguration(
            Path dataDirectory,
            Path logDirectory,
            int pageSize,
            Charset charset,
            String version,
            String databaseFileName
    ) {
        if (dataDirectory == null) {
            throw new ConfigurationException(
                    "Data directory cannot be null."
            );
        }

        if (logDirectory == null) {
            throw new ConfigurationException(
                    "Log directory cannot be null."
            );
        }

        if (pageSize <= 0) {
            throw new ConfigurationException(
                    "Page size must be greater than zero."
            );
        }

        if (charset == null) {
            throw new ConfigurationException(
                    "Charset cannot be null."
            );
        }

        String normalizedVersion = normalizeRequiredText(
                version,
                "Version cannot be empty."
        );

        String normalizedDatabaseFileName = normalizeRequiredText(
                databaseFileName,
                "Database file name cannot be empty."
        );

        this.dataDirectory = dataDirectory
                .toAbsolutePath()
                .normalize();

        this.logDirectory = logDirectory
                .toAbsolutePath()
                .normalize();

        this.pageSize = pageSize;
        this.charset = charset;
        this.version = normalizedVersion;
        this.databaseFileName = normalizedDatabaseFileName;
    }

    public static YekdbConfiguration load() {
        return ConfigurationManager.load();
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Path getLogDirectory() {
        return logDirectory;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Charset getCharset() {
        return charset;
    }

    public String getVersion() {
        return version;
    }

    public String getDatabaseFileName() {
        return databaseFileName;
    }

    public Path getDatabaseFilePath() {
        return dataDirectory
                .resolve(databaseFileName)
                .normalize();
    }

    private static String normalizeRequiredText(
            String value,
            String errorMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(errorMessage);
        }

        return value.trim();
    }

    @Override
    public String toString() {
        return "YekdbConfiguration{" +
                "dataDirectory=" + dataDirectory +
                ", logDirectory=" + logDirectory +
                ", pageSize=" + pageSize +
                ", charset=" + charset +
                ", version='" + version + '\'' +
                ", databaseFileName='" + databaseFileName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof YekdbConfiguration that)) {
            return false;
        }

        return pageSize == that.pageSize
                && dataDirectory.equals(that.dataDirectory)
                && logDirectory.equals(that.logDirectory)
                && charset.equals(that.charset)
                && version.equals(that.version)
                && databaseFileName.equals(that.databaseFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                dataDirectory,
                logDirectory,
                pageSize,
                charset,
                version,
                databaseFileName
        );
    }
}
