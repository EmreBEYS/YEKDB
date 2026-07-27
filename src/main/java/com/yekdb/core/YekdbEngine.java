package com.yekdb.core;

import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;

import java.io.IOException;
import java.nio.file.Path;

/**
 * YEKDB sisteminin ana motorudur.
 *
 * Alt bileşenlerin yaşam döngüsünü yönetir:
 * - Configuration
 * - Logger
 * - StorageEngine
 *
 * İleride buraya:
 * - SQL Engine
 * - Transaction Manager
 * - Catalog Manager
 * - Index Manager
 *
 * bileşenleri eklenecektir.
 */
public class YekdbEngine {

    private final StorageEngine storageEngine;
    private boolean running;

    public YekdbEngine(Path dataFilePath) {
        this.storageEngine = new StorageEngine(dataFilePath);
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

        System.out.println("YEKDB başarıyla başlatıldı.");
    }

    /**
     * YEKDB motorunu güvenli şekilde kapatır.
     */
    public void shutdown() throws IOException {
        if (!running) {
            return;
        }

        System.out.println("YEKDB kapatılıyor...");

        storageEngine.shutdown();

        running = false;

        System.out.println("YEKDB başarıyla kapatıldı.");
    }

    /**
     * Yeni kaydı depolama motoruna gönderir.
     *
     * @return Kaydın dosyada başladığı konum
     */
    public long insertRecord(Record record) throws IOException {
        ensureRunning();
        return storageEngine.insertRecord(record);
    }

    /**
     * Belirtilen dosya konumundaki kaydı okur.
     */
    public Record readRecord(
            long position,
            int serializedLength
    ) throws IOException {

        ensureRunning();

        return storageEngine.readRecord(
                position,
                serializedLength
        );
    }

    public long getDataFileSize() throws IOException {
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
                    "YEKDB Engine başlatılmadan işlem yapılamaz."
            );
        }
    }
}