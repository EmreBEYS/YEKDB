package com.yekdb.core;

import com.yekdb.config.YekdbConfiguration;
import com.yekdb.storage.StorageEngine;

public final class YekdbEngine {

    private YekdbConfiguration configuration;
    private StorageEngine storageEngine;
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

        System.out.println(
                "[YEKDB] Initializing storage layer..."
        );

        storageEngine = new StorageEngine(configuration);
        storageEngine.initialize();

        running = true;

        printSystemInformation();

        System.out.println(
                "[YEKDB] Database engine started successfully."
        );
    }

    public void shutdown() {
        if (!running) {
            return;
        }

        System.out.println(
                "[YEKDB] Shutting down database engine..."
        );

        if (storageEngine != null) {
            storageEngine.shutdown();
        }

        running = false;

        System.out.println(
                "[YEKDB] Database engine stopped successfully."
        );
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