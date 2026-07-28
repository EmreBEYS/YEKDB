package com.yekdb.core;

import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.page.Page;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * YEKDB sisteminin ana motorudur.
 *
 * Alt bileşenlerin yaşam döngüsünü yönetir:
 * - Configuration
 * - Logger
 * - StorageEngine
 *
 * İleride:
 * - SQL Engine
 * - Transaction Manager
 * - Catalog Manager
 * - Index Manager
 *
 * bileşenleri eklenecektir.
 */
public final class YekdbEngine {

    private final StorageEngine storageEngine;

    private boolean running;

    public YekdbEngine(Path dataFilePath) {

        Objects.requireNonNull(
                dataFilePath,
                "Data file path cannot be null."
        );

        this.storageEngine =
                new StorageEngine(dataFilePath);

        this.running = false;
    }

    /**
     * YEKDB motorunu başlatır.
     */
    public void start() throws IOException {

        if (running) {
            return;
        }

        System.out.println("YEKDB başlatılıyor...");

        storageEngine.initialize();

        running = true;

        System.out.println(
                "YEKDB başarıyla başlatıldı."
        );
    }

    /**
     * YEKDB motorunu güvenli şekilde kapatır.
     */
    public void shutdown() throws IOException {

        if (!running
                && !storageEngine.isInitialized()) {
            return;
        }

        System.out.println("YEKDB kapatılıyor...");

        storageEngine.shutdown();

        running = false;

        System.out.println(
                "YEKDB başarıyla kapatıldı."
        );
    }

    public void writePage(Page page)
            throws IOException {

        ensureRunning();

        storageEngine.writePage(page);
    }

    public Page readPage(int pageId)
            throws IOException {

        ensureRunning();

        return storageEngine.readPage(pageId);
    }

    public boolean pageExists(int pageId)
            throws IOException {

        ensureRunning();

        return storageEngine.pageExists(pageId);
    }

    public int getPageCount()
            throws IOException {

        ensureRunning();

        return storageEngine.getPageCount();
    }

    public void checkpoint()
            throws IOException {

        ensureRunning();

        storageEngine.checkpoint();
    }

    public DatabaseHeader getDatabaseHeader() {

        ensureRunning();

        return storageEngine.getDatabaseHeader();
    }

    public long getDataFileSize()
            throws IOException {

        ensureRunning();

        return storageEngine.getFileSize();
    }

    public boolean isRunning() {
        return running;
    }

    public StorageEngine getStorageEngine() {
        return storageEngine;
    }

    private void ensureRunning() {

        if (!running) {
            throw new IllegalStateException(
                    "YEKDB Engine must be running before use."
            );
        }
    }
}