package com.yekdb.core;

import com.yekdb.config.YekdbConfiguration;
import com.yekdb.logs.LoggerFactory;
import com.yekdb.logs.YekdbLogger;
import com.yekdb.storage.StorageEngine;

public final class YekdbEngine {

    private YekdbConfiguration configuration;
    private StorageEngine storageEngine;
    private YekdbLogger logger;
    private boolean running;

    public void start() {
        if (running) {
            System.out.println(
                    "[YEKDB] Database engine is already running."
            );
            return;
        }

        printBanner();

        System.out.println(
                "[YEKDB] Loading system configuration..."
        );

        configuration = YekdbConfiguration.load();

        LoggerFactory.initialize(configuration);
        logger = LoggerFactory.getLogger();

        logger.info("YEKDB logger initialized.");

        logger.info("Initializing storage layer.");

        storageEngine = new StorageEngine(configuration);
        storageEngine.initialize();

        logger.info("Storage Engine initialized.");

        running = true;

        printSystemInformation();

        logger.info(
                "Database engine started successfully."
        );
    }

    public void shutdown() {
        if (!running) {
            return;
        }

        if (logger != null) {
            logger.info(
                    "Shutting down database engine."
            );
        }

        if (storageEngine != null) {
            storageEngine.shutdown();
        }

        running = false;

        if (logger != null) {
            logger.info(
                    "Database engine stopped successfully."
            );
        }

        LoggerFactory.shutdown();
        logger = null;
    }

    public boolean isRunning() {
        return running;
    }

    public YekdbConfiguration getConfiguration() {
        return configuration;
    }

    public StorageEngine getStorageEngine() {
        return storageEngine;
    }

    private void printSystemInformation() {
        System.out.println(
                "[YEKDB] Version        : "
                        + configuration.getVersion()
        );

        System.out.println(
                "[YEKDB] Data directory : "
                        + configuration
                        .getDataDirectory()
                        .toAbsolutePath()
                        .normalize()
        );

        System.out.println(
                "[YEKDB] Log directory  : "
                        + configuration
                        .getLogDirectory()
                        .toAbsolutePath()
                        .normalize()
        );

        System.out.println(
                "[YEKDB] Page size      : "
                        + configuration.getPageSize()
                        + " bytes"
        );

        System.out.println(
                "[YEKDB] Charset        : "
                        + configuration.getCharset().displayName()
        );

        System.out.println(
                "[YEKDB] Database file  : "
                        + configuration
                        .getDatabaseFilePath()
                        .toAbsolutePath()
                        .normalize()
        );
    }

    private void printBanner() {
        System.out.println("""
                
                ========================================
                            YEKDB DATABASE
                ========================================
                Relational Database Management System
                Developed from scratch with Java
                ========================================
                """);
    }
}