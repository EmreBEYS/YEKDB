package com.yekdb.logs;

import com.yekdb.config.YekdbConfiguration;

public final class LoggerFactory {

    private static YekdbLogger logger;

    private LoggerFactory() {
    }

    public static synchronized void initialize(
            YekdbConfiguration configuration
    ) {
        if (logger != null) {
            return;
        }

        logger = new YekdbLogger(
                configuration.getLogDirectory()
        );

        logger.initialize();
    }

    public static synchronized YekdbLogger getLogger() {
        if (logger == null) {
            throw new IllegalStateException(
                    "LoggerFactory has not been initialized."
            );
        }

        return logger;
    }

    public static synchronized boolean isInitialized() {
        return logger != null;
    }

    public static synchronized void shutdown() {
        if (logger == null) {
            return;
        }

        logger.close();
        logger = null;
    }
}