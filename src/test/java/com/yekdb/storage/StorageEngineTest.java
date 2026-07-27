package com.yekdb.storage;

import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageEngineTest {

    private final Path testFile =
            Path.of("data", "storage-test.ydb");

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(testFile);
    }

    @Test
    void shouldInitializeStorageEngine() throws Exception {

        StorageEngine engine =
                new StorageEngine(testFile);

        engine.initialize();

        assertTrue(engine.isInitialized());

        engine.shutdown();
    }

    @Test
    void shouldInsertAndReadRecord() throws Exception {

        StorageEngine engine =
                new StorageEngine(testFile);

        engine.initialize();

        Record record = new Record(
                1L,
                "Storage"
                        .getBytes(StandardCharsets.UTF_8)
        );

        long position =
                engine.insertRecord(record);

        Record loaded =
                engine.readRecord(
                        position,
                        RecordSerializer.calculateSerializedSize(record)
                );

        assertEquals(record, loaded);

        engine.shutdown();
    }

    @Test
    void shouldReturnCorrectFileSize() throws Exception {

        StorageEngine engine =
                new StorageEngine(testFile);

        engine.initialize();

        Record record =
                new Record(
                        1L,
                        "ABC".getBytes()
                );

        engine.insertRecord(record);

        assertEquals(
                RecordSerializer.calculateSerializedSize(record),
                engine.getFileSize()
        );

        engine.shutdown();
    }

    @Test
    void shouldShutdownEngine() throws Exception {

        StorageEngine engine =
                new StorageEngine(testFile);

        engine.initialize();

        engine.shutdown();

        assertFalse(engine.isInitialized());
    }
}