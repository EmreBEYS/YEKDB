package com.yekdb.config;

import com.yekdb.core.YekdbConstants;
import com.yekdb.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/**
 * YEKDB yapılandırmasını classpath üzerindeki yekdb.properties
 * dosyasından yükler. Dosya bulunamazsa güvenli varsayılanlar kullanılır.
 */
public final class ConfigurationManager {

    private static final String CONFIGURATION_FILE = "yekdb.properties";

    private static final String DEFAULT_DATA_DIRECTORY = "data";
    private static final String DEFAULT_LOG_DIRECTORY = "logs";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String DEFAULT_VERSION = "0.1.0";
    private static final String DEFAULT_DATABASE_FILE = "yekdb.data";

    private ConfigurationManager() {
    }

    public static YekdbConfiguration load() {
        Properties properties = loadProperties();

        Path dataDirectory = Path.of(
                readText(
                        properties,
                        "yekdb.data.directory",
                        DEFAULT_DATA_DIRECTORY
                )
        );

        Path logDirectory = Path.of(
                readText(
                        properties,
                        "yekdb.log.directory",
                        DEFAULT_LOG_DIRECTORY
                )
        );

        int pageSize = readPageSize(properties);
        Charset charset = readCharset(properties);

        String version = readText(
                properties,
                "yekdb.version",
                DEFAULT_VERSION
        );

        String databaseFileName = readText(
                properties,
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
                     classLoader.getResourceAsStream(CONFIGURATION_FILE)) {

            if (inputStream == null) {
                return properties;
            }

            properties.load(inputStream);
            return properties;

        } catch (IOException exception) {
            throw new ConfigurationException(
                    "Configuration file could not be loaded.",
                    exception
            );
        }
    }

    private static int readPageSize(Properties properties) {
        String rawPageSize = readText(
                properties,
                "yekdb.page.size",
                String.valueOf(YekdbConstants.PAGE_SIZE)
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

    private static Charset readCharset(Properties properties) {
        String charsetName = readText(
                properties,
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

    private static String readText(
            Properties properties,
            String key,
            String defaultValue
    ) {
        String value = properties.getProperty(key, defaultValue);

        if (value == null) {
            return defaultValue;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? defaultValue
                : normalizedValue;
    }
}
