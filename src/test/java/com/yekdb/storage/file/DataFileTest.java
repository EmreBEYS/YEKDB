package com.yekdb.storage.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DataFileTest {

    private Path testDirectory;
    private Path testFilePath;
    private DataFile dataFile;

    @BeforeEach
    void setUp() throws Exception {

        testDirectory = Path.of(
                "target",
                "test-data",
                "data-file"
        );

        testFilePath = testDirectory.resolve("test.data");

        Files.createDirectories(testDirectory);
        Files.deleteIfExists(testFilePath);

        dataFile = new DataFile(testFilePath);
    }

    @AfterEach
    void tearDown() throws Exception {

        if (dataFile != null && dataFile.isOpen()) {
            dataFile.close();
        }

        Files.deleteIfExists(testFilePath);
    }

    @Test
    void shouldCreateAndOpenDataFile() throws Exception {

        dataFile.open();

        assertTrue(dataFile.exists());
        assertTrue(dataFile.isOpen());
        assertEquals(0, dataFile.size());
    }

    @Test
    void shouldAppendBytesToFile() throws Exception {

        dataFile.open();

        byte[] bytes = {10, 20, 30, 40};

        long position = dataFile.append(bytes);

        assertEquals(0, position);
        assertEquals(4, dataFile.size());
        assertArrayEquals(
                bytes,
                dataFile.read(0, bytes.length)
        );
    }

    @Test
    void shouldWriteBytesAtSpecifiedPosition() throws Exception {

        dataFile.open();

        dataFile.append(new byte[]{0, 0, 0, 0, 0});
        dataFile.write(1, new byte[]{10, 20, 30});

        assertArrayEquals(
                new byte[]{0, 10, 20, 30, 0},
                dataFile.read(0, 5)
        );
    }

    @Test
    void shouldCloseDataFile() throws Exception {

        dataFile.open();
        dataFile.close();

        assertFalse(dataFile.isOpen());
    }

    @Test
    void shouldRejectOperationsWhenFileIsClosed() {

        assertThrows(
                IllegalStateException.class,
                () -> dataFile.size()
        );
    }

    @Test
    void shouldRejectNegativePosition() throws Exception {

        dataFile.open();

        assertThrows(
                IllegalArgumentException.class,
                () -> dataFile.read(-1, 1)
        );
    }

    @Test
    void shouldRejectReadBeyondFileSize() throws Exception {

        dataFile.open();
        dataFile.append(new byte[]{1, 2, 3});

        assertThrows(
                java.io.EOFException.class,
                () -> dataFile.read(0, 4)
        );
    }
}