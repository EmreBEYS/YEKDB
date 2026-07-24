package com.yekdb.config;

import com.yekdb.exception.ConfigurationException;

import java.nio.charset.Charset;
import java.nio.file.Path;

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

        if (version == null || version.isBlank()) {
            throw new ConfigurationException(
                    "Version cannot be empty."
            );
        }

        if (databaseFileName == null || databaseFileName.isBlank()) {
            throw new ConfigurationException(
                    "Database file name cannot be empty."
            );
        }

        this.dataDirectory = dataDirectory;
        this.logDirectory = logDirectory;
        this.pageSize = pageSize;
        this.charset = charset;
        this.version = version;
        this.databaseFileName = databaseFileName;
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
        return dataDirectory.resolve(databaseFileName);
    }
}