package com.yekdb.storage;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordSerializer;

import java.io.IOException;
import java.nio.file.Path;

/**
 * YEKDB depolama işlemlerini yöneten ana sınıftır.
 *
 * Record nesnelerinin:
 * - diske yazılması
 * - diskten okunması
 * - veri dosyasının açılması ve kapatılması
 *
 * işlemlerini yönetir.
 */
public class StorageEngine {

    private final DataFile dataFile;
    private boolean initialized;

    public StorageEngine(Path dataFilePath) {
        this.dataFile = new DataFile(dataFilePath);
        this.initialized = false;
    }

    public void initialize() throws IOException {
        if (initialized) {
            return;
        }

        dataFile.open();
        initialized = true;
    }

    public void shutdown() throws IOException {
        if (!initialized) {
            return;
        }

        dataFile.sync();
        dataFile.close();
        initialized = false;
    }

    public long insertRecord(Record record) throws IOException {
        ensureInitialized();

        byte[] serializedRecord =
                RecordSerializer.serialize(record);

        long position =
                dataFile.append(serializedRecord);

        dataFile.sync();

        return position;
    }

    public Record readRecord(
            long position,
            int serializedLength
    ) throws IOException {

        ensureInitialized();

        byte[] recordBytes = dataFile.read(
                position,
                serializedLength
        );

        return RecordSerializer.deserialize(recordBytes);
    }

    public long getFileSize() throws IOException {
        ensureInitialized();
        return dataFile.size();
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "StorageEngine başlatılmadan işlem yapılamaz."
            );
        }
    }
}