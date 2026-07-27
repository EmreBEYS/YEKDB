package com.yekdb.core;

import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YekdbEngineTest {

    private final Path testFile =
            Path.of("data", "engine-test.ydb");

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(testFile);
    }

    @Test
    void shouldStartEngine() throws Exception {

        YekdbEngine engine =
                new YekdbEngine(testFile);

        engine.start();

        assertTrue(engine.isRunning());

        engine.shutdown();
    }

    @Test
    void shouldInsertAndReadRecord() throws Exception {

        YekdbEngine engine =
                new YekdbEngine(testFile);

        engine.start();

        Record record =
                new Record(
                        1L,
                        "YEKDB".getBytes(StandardCharsets.UTF_8)
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
    void shouldShutdownEngine() throws Exception {

        YekdbEngine engine =
                new YekdbEngine(testFile);

        engine.start();

        engine.shutdown();

        assertFalse(engine.isRunning());
    }
}