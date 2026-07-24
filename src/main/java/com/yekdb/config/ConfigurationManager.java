package com.yekdb.config;

import com.yekdb.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigurationManager {

    private static final String CONFIGURATION_FILE = "yekdb.properties";

    private static final String DEFAULT_DATA_DIRECTORY = "data";

    private static final String DEFAULT_LOG_DIRECTORY = "logs";

    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final String DEFAULT_VERSION = "0.1.0";

    private static final String DEFAULT_DATABASE_FILE = "yekdb.data";

    private ConfigurationManager() {
    }

    public static YekdbConfiguration load() {
        Properties properties = loadProperties();

        Path dataDirectory = Path.of(
                properties.getProperty(
                        "yekdb.data.directory",
                        DEFAULT_DATA_DIRECTORY
                )
        );

        Path logDirectory = Path.of(
                properties.getProperty(
                        "yekdb.log.directory",
                        DEFAULT_LOG_DIRECTORY
                )
        );

        int pageSize = readPageSize(properties);

        Charset charset = readCharset(properties);

        String version = properties.getProperty(
                "yekdb.version",
                DEFAULT_VERSION
        );

        String databaseFileName = properties.getProperty(
                "yekdb.database.file",
                DEFAULT_DATABASE_FILE
        );

        return new YekdbConfiguration(
                dataDirectory,
                logDirectory,
                pageSize,
                charset,
                version,
                databaseFileName
        );
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        ClassLoader classLoader =
                ConfigurationManager.class.getClassLoader();

        try (InputStream inputStream =
                     classLoader.getResourceAsStream(
                             CONFIGURATION_FILE
                     )) {

            if (inputStream == null) {
                System.out.println(
                        "[CONFIG] Configuration file was not found. "
                                + "Default values will be used."
                );

                return properties;
            }

            properties.load(inputStream);

            System.out.println(
                    "[CONFIG] Configuration loaded from "
                            + CONFIGURATION_FILE
            );

            return properties;

        } catch (IOException exception) {
            throw new ConfigurationException(
                    "Configuration file could not be loaded.",
                    exception
            );
        }
    }

    private static int readPageSize(
            Properties properties
    ) {
        String rawPageSize = properties.getProperty(
                "yekdb.page.size",
                String.valueOf(DEFAULT_PAGE_SIZE)
        );

        try {
            int pageSize = Integer.parseInt(rawPageSize);

            if (pageSize <= 0) {
                throw new ConfigurationException(
                        "Page size must be greater than zero."
                );
            }

            return pageSize;

        } catch (NumberFormatException exception) {
            throw new ConfigurationException(
                    "Invalid page size: " + rawPageSize,
                    exception
            );
        }
    }

    private static Charset readCharset(
            Properties properties
    ) {
        String charsetName = properties.getProperty(
                "yekdb.charset",
                DEFAULT_CHARSET.name()
        );

        try {
            return Charset.forName(charsetName);

        } catch (RuntimeException exception) {
            throw new ConfigurationException(
                    "Invalid charset: " + charsetName,
                    exception
            );
        }
    }
}