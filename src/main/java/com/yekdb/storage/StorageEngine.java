package com.yekdb.storage;

import com.yekdb.config.YekdbConfiguration;
import com.yekdb.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StorageEngine {

    private final YekdbConfiguration configuration;

    private boolean initialized;

    public StorageEngine(YekdbConfiguration configuration) {

        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Configuration cannot be null."
            );
        }

        this.configuration = configuration;
    }

    public void initialize() {

        if (initialized) {
            return;
        }

        createDirectory(configuration.getDataDirectory());

        createDirectory(configuration.getLogDirectory());

        initialized = true;

        System.out.println(
                "[STORAGE] Storage Engine initialized."
        );
    }

    private void createDirectory(Path directory) {

        try {

            Files.createDirectories(directory);

            if (!Files.isDirectory(directory)) {

                throw new StorageException(
                        "Directory is invalid : "
                                + directory
                );
            }

            if (!Files.isWritable(directory)) {

                throw new StorageException(
                        "Directory is not writable : "
                                + directory
                );
            }

        } catch (IOException exception) {

            throw new StorageException(
                    "Cannot initialize directory : "
                            + directory,
                    exception
            );
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void shutdown() {

        initialized = false;

        System.out.println(
                "[STORAGE] Storage Engine stopped."
        );
    }

}