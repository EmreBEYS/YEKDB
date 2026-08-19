package com.yekdb.storage.table.header;

import com.yekdb.storage.exception.CorruptedTableHeaderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderIOTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldWriteTableHeaderToDisk()
            throws IOException {

        Path file =
                tempDirectory.resolve("users.yekt");

        TableHeader header =
                createHeader();

        TableHeaderIO.write(
                file,
                header
        );

        assertTrue(
                Files.exists(file)
        );

        assertEquals(
                TableHeaderConstants.HEADER_SIZE,
                Files.size(file)
        );
    }

    @Test
    void shouldReadTableHeaderFromDisk()
            throws IOException {

        Path file =
                tempDirectory.resolve("users.yekt");

        TableHeader original =
                createHeader();

        TableHeaderIO.write(
                file,
                original
        );

        TableHeader restored =
                TableHeaderIO.read(file);

        assertEquals(
                original,
                restored
        );
    }

    @Test
    void shouldOverwriteExistingHeader()
            throws IOException {

        Path file =
                tempDirectory.resolve("users.yekt");

        TableHeader first =
                new TableHeader(
                        1L,
                        "users",
                        4,
                        10L,
                        5L,
                        5L,
                        512L,
                        0
                );

        TableHeader second =
                new TableHeader(
                        1L,
                        "users",
                        4,
                        500L,
                        5L,
                        20L,
                        512L,
                        0
                );

        TableHeaderIO.write(
                file,
                first
        );

        TableHeaderIO.write(
                file,
                second
        );

        TableHeader restored =
                TableHeaderIO.read(file);

        assertEquals(
                second,
                restored
        );

        assertEquals(
                500L,
                restored.getRowCount()
        );
    }

    @Test
    void shouldPreserveUtf8TableNameOnDisk()
            throws IOException {

        Path file =
                tempDirectory.resolve("customers.yekt");

        TableHeader original =
                new TableHeader(
                        2L,
                        "müşteriler",
                        6,
                        15L,
                        10L,
                        12L,
                        1024L,
                        0
                );

        TableHeaderIO.write(
                file,
                original
        );

        TableHeader restored =
                TableHeaderIO.read(file);

        assertEquals(
                "müşteriler",
                restored.getTableName()
        );
    }

    @Test
    void shouldRejectTruncatedHeaderFile()
            throws IOException {

        Path file =
                tempDirectory.resolve("broken.yekt");

        Files.write(
                file,
                new byte[100]
        );

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderIO.read(file)
        );
    }

    @Test
    void shouldRejectCorruptedHeaderFile()
            throws IOException {

        Path file =
                tempDirectory.resolve("corrupted.yekt");

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        data[0] = 0;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;

        Files.write(
                file,
                data
        );

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderIO.read(file)
        );
    }

    @Test
    void shouldFailWhenFileDoesNotExist() {

        Path file =
                tempDirectory.resolve("missing.yekt");

        assertThrows(
                IOException.class,
                () -> TableHeaderIO.read(file)
        );
    }

    @Test
    void shouldRejectNullWritePath() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TableHeaderIO.write(
                        null,
                        createHeader()
                )
        );
    }

    @Test
    void shouldRejectNullReadPath() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TableHeaderIO.read(null)
        );
    }

    private TableHeader createHeader() {

        return new TableHeader(
                1L,
                "users",
                4,
                100L,
                5L,
                8L,
                512L,
                TableHeaderConstants.FLAG_NONE
        );
    }
}