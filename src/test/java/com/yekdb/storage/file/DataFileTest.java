package com.yekdb.storage.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DataFileTest {

    private final Path testFile =
            Path.of("data", "test-datafile.ydb");

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(testFile);
    }

    @Test
    void shouldCreateDataFile() throws Exception {

        DataFile dataFile = new DataFile(testFile);

        dataFile.open();

        assertTrue(Files.exists(testFile));

        dataFile.close();
    }

    @Test
    void shouldAppendAndReadData() throws Exception {

        DataFile dataFile = new DataFile(testFile);

        dataFile.open();

        byte[] data =
                "YEKDB".getBytes(StandardCharsets.UTF_8);

        long position =
                dataFile.append(data);

        byte[] read =
                dataFile.read(position, data.length);

        assertArrayEquals(data, read);

        dataFile.close();
    }

    @Test
    void shouldReturnCorrectFileSize() throws Exception {

        DataFile dataFile = new DataFile(testFile);

        dataFile.open();

        byte[] data =
                "ABCDE".getBytes(StandardCharsets.UTF_8);

        dataFile.append(data);

        assertEquals(
                data.length,
                dataFile.size()
        );

        dataFile.close();
    }

    @Test
    void shouldAppendAtEndOfFile() throws Exception {

        DataFile dataFile = new DataFile(testFile);

        dataFile.open();

        long first =
                dataFile.append(new byte[5]);

        long second =
                dataFile.append(new byte[10]);

        assertEquals(0, first);
        assertEquals(5, second);

        dataFile.close();
    }

    @Test
    void shouldSyncWithoutException() throws Exception {

        DataFile dataFile = new DataFile(testFile);

        dataFile.open();

        dataFile.append(new byte[10]);

        assertDoesNotThrow(
                dataFile::sync
        );

        dataFile.close();
    }
}