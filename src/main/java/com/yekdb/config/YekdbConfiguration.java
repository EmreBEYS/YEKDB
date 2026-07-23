package com.yekdb.config;

import com.yekdb.exception.ConfigurationException;

import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class YekdbConfiguration{

    /*--------------------Default Values---------------------------- */
    private static final String DEFAULT_DATA_DIRECTORY = "data";
    private static final String DEFAULT_LOG_DIRECTORY = "logs";

    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static final Charset DEFAULT_CHARSET =
            StandardCharsets.UTF_8;

    private static final String VERSION = "0.1.0";
    /* ---------------- Configuration ---------------- */
    private final Path dataDirectory;

    private final Path logDirectory;

    private final int pageSize;

    private final Charset charset;

    private final String version;

    private YekdbConfiguration(
            Path dataDirectory,
            Path logDirectory,
            int pageSize,
            Charset charset,
            String version
    ){
        if (dataDirectory == null){
            throw new ConfigurationException(
                    "Data directory cannot be null"
            );
        }
        if (logDirectory == null){
            throw new ConfigurationException(
                    "Log directory cannot be null."
            );
        }
        if (pageSize <=0){
            throw new ConfigurationException(
                    "Page size must be greater than zero."
            );
        }
        this.dataDirectory=dataDirectory;
        this.logDirectory=logDirectory;
        this.pageSize=pageSize;
        this.charset=charset;
        this.version=version;
    }

    public static YekdbConfiguration load() {

        Path dataDirectory =
                Path.of(DEFAULT_DATA_DIRECTORY);

        Path logDirectory =
                Path.of(DEFAULT_LOG_DIRECTORY);

        return new YekdbConfiguration(
                dataDirectory,
                logDirectory,
                DEFAULT_PAGE_SIZE,
                DEFAULT_CHARSET,
                VERSION
        );
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
}
