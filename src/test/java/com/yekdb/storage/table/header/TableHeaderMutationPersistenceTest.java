package com.yekdb.storage.table.header;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderMutationPersistenceTest {

    @TempDir
    Path tempDirectory;

    private Path createTableFile(
            long rowCount,
            long firstPageId,
            long lastPageId
    ) throws IOException {

        Path tableFile =
                tempDirectory.resolve(
                        "users.tbl"
                );

        TableHeader header =
                new TableHeader(
                        1L,
                        "users",
                        3,
                        rowCount,
                        firstPageId,
                        lastPageId,
                        TableHeaderConstants.HEADER_SIZE,
                        TableHeaderConstants.FLAG_NONE
                );

        String schema =
                """
                        YEKDB_TABLE
                        version=1
                        tableName=users
                        columnCount=3
                        createdAt=2026-08-20T10:00:00
                        columns=
                        id:INT
                        name:STRING
                        age:INT
                        """;

        byte[] headerBytes =
                TableHeaderSerializer.serialize(
                        header
                );

        byte[] schemaBytes =
                schema.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] fileBytes =
                new byte[
                        headerBytes.length
                                + schemaBytes.length
                        ];

        System.arraycopy(
                headerBytes,
                0,
                fileBytes,
                0,
                headerBytes.length
        );

        System.arraycopy(
                schemaBytes,
                0,
                fileBytes,
                headerBytes.length,
                schemaBytes.length
        );

        Files.write(
                tableFile,
                fileBytes
        );

        return tableFile;
    }

    @Test
    void shouldPersistRowCount()
            throws IOException {

        Path tableFile =
                createTableFile(
                        0L,
                        -1L,
                        -1L
                );

        TableHeaderUpdater.persistRowCount(
                tableFile,
                25L
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                25L,
                persisted.getRowCount()
        );
    }

    @Test
    void shouldPersistIncrementedRowCount()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        TableHeader updated =
                TableHeaderUpdater
                        .persistIncrementRowCount(
                                tableFile
                        );

        assertEquals(
                11L,
                updated.getRowCount()
        );

        assertEquals(
                11L,
                TableHeaderIO.read(tableFile)
                        .getRowCount()
        );
    }

    @Test
    void shouldPersistDecrementedRowCount()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        TableHeader updated =
                TableHeaderUpdater
                        .persistDecrementRowCount(
                                tableFile
                        );

        assertEquals(
                9L,
                updated.getRowCount()
        );
    }

    @Test
    void shouldPersistDataPageRange()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        TableHeaderUpdater.persistDataPageRange(
                tableFile,
                100L,
                105L
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                100L,
                persisted.getFirstDataPageId()
        );

        assertEquals(
                105L,
                persisted.getLastDataPageId()
        );
    }

    @Test
    void shouldPreserveSchemaAfterHeaderMutation()
            throws IOException {

        Path tableFile =
                createTableFile(
                        0L,
                        -1L,
                        -1L
                );

        byte[] before =
                Files.readAllBytes(
                        tableFile
                );

        byte[] schemaBefore =
                java.util.Arrays.copyOfRange(
                        before,
                        TableHeaderConstants.HEADER_SIZE,
                        before.length
                );

        TableHeaderUpdater.persistRowCount(
                tableFile,
                50L
        );

        byte[] after =
                Files.readAllBytes(
                        tableFile
                );

        byte[] schemaAfter =
                java.util.Arrays.copyOfRange(
                        after,
                        TableHeaderConstants.HEADER_SIZE,
                        after.length
                );

        assertArrayEquals(
                schemaBefore,
                schemaAfter
        );
    }

    @Test
    void shouldNotChangeFileSizeWhenUpdatingHeader()
            throws IOException {

        Path tableFile =
                createTableFile(
                        0L,
                        -1L,
                        -1L
                );

        long sizeBefore =
                Files.size(
                        tableFile
                );

        TableHeaderUpdater.persistRowCount(
                tableFile,
                100L
        );

        long sizeAfter =
                Files.size(
                        tableFile
                );

        assertEquals(
                sizeBefore,
                sizeAfter
        );
    }

    @Test
    void shouldPreserveImmutableMetadataAfterPersistence()
            throws IOException {

        Path tableFile =
                createTableFile(
                        5L,
                        -1L,
                        -1L
                );

        TableHeader before =
                TableHeaderIO.read(
                        tableFile
                );

        TableHeader after =
                TableHeaderUpdater.persistRowCount(
                        tableFile,
                        20L
                );

        assertEquals(
                before.getTableId(),
                after.getTableId()
        );

        assertEquals(
                before.getTableName(),
                after.getTableName()
        );

        assertEquals(
                before.getColumnCount(),
                after.getColumnCount()
        );

        assertEquals(
                before.getSchemaOffset(),
                after.getSchemaOffset()
        );

        assertEquals(
                before.getFlags(),
                after.getFlags()
        );
    }

    @Test
    void shouldRecoverPersistedMetadataAfterReopen()
            throws IOException {

        Path tableFile =
                createTableFile(
                        0L,
                        -1L,
                        -1L
                );

        TableHeaderUpdater.persistRowCount(
                tableFile,
                42L
        );

        TableHeaderUpdater.persistDataPageRange(
                tableFile,
                8L,
                12L
        );

        /*
         * Yeni bir read işlemi restart / reopen
         * davranışını simüle eder.
         */
        TableHeader recovered =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                42L,
                recovered.getRowCount()
        );

        assertEquals(
                8L,
                recovered.getFirstDataPageId()
        );

        assertEquals(
                12L,
                recovered.getLastDataPageId()
        );
    }
    @Test
    void shouldRejectMissingTableFile() {

        Path missingFile =
                tempDirectory.resolve(
                        "missing.tbl"
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater.persistRowCount(
                        missingFile,
                        10L
                )
        );
    }
    @Test
    void shouldRejectPersistentDecrementBelowZero()
            throws IOException {

        Path tableFile =
                createTableFile(
                        0L,
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .persistDecrementRowCount(
                                tableFile
                        )
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                0L,
                persisted.getRowCount()
        );
    }
    @Test
    void shouldRejectPersistentRowCountOverflow()
            throws IOException {

        Path tableFile =
                createTableFile(
                        Long.MAX_VALUE,
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .persistIncrementRowCount(
                                tableFile
                        )
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                Long.MAX_VALUE,
                persisted.getRowCount()
        );
    }
    @Test
    void shouldRejectNegativePersistentRowCount()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater.persistRowCount(
                        tableFile,
                        -1L
                )
        );

        assertEquals(
                10L,
                TableHeaderIO.read(tableFile)
                        .getRowCount()
        );
    }
    @Test
    void shouldRejectInvalidPersistentPageRange()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .persistDataPageRange(
                                tableFile,
                                20L,
                                10L
                        )
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                -1L,
                persisted.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                persisted.getLastDataPageId()
        );
    }
    @Test
    void shouldRejectPartialPersistentPageRange()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .persistDataPageRange(
                                tableFile,
                                5L,
                                -1L
                        )
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                -1L,
                persisted.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                persisted.getLastDataPageId()
        );
    }
    @Test
    void shouldRejectNullTableFile() {

        assertThrows(
                NullPointerException.class,
                () -> TableHeaderUpdater.persistRowCount(
                        null,
                        10L
                )
        );
    }
    @Test
    void shouldRejectCorruptedHeaderMutation()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        byte[] bytes =
                Files.readAllBytes(
                        tableFile
                );

        bytes[0] =
                (byte) (bytes[0] ^ 0xFF);

        Files.write(
                tableFile,
                bytes
        );

        byte[] corruptedBefore =
                Files.readAllBytes(
                        tableFile
                );

        assertThrows(
                RuntimeException.class,
                () -> TableHeaderUpdater.persistRowCount(
                        tableFile,
                        20L
                )
        );

        byte[] corruptedAfter =
                Files.readAllBytes(
                        tableFile
                );

        assertArrayEquals(
                corruptedBefore,
                corruptedAfter
        );
    }
    @Test
    void shouldRejectTruncatedHeaderMutation()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        byte[] truncated =
                new byte[100];

        Files.write(
                tableFile,
                truncated
        );

        long sizeBefore =
                Files.size(
                        tableFile
                );

        assertThrows(
                RuntimeException.class,
                () -> TableHeaderUpdater.persistRowCount(
                        tableFile,
                        20L
                )
        );

        assertEquals(
                sizeBefore,
                Files.size(tableFile)
        );
    }
    @Test
    void shouldPersistMultipleSequentialMutations()
            throws IOException {

        Path tableFile =
                createTableFile(
                        0L,
                        -1L,
                        -1L
                );

        TableHeaderUpdater.persistIncrementRowCount(
                tableFile
        );

        TableHeaderUpdater.persistIncrementRowCount(
                tableFile
        );

        TableHeaderUpdater.persistIncrementRowCount(
                tableFile
        );

        TableHeaderUpdater.persistDataPageRange(
                tableFile,
                100L,
                103L
        );

        TableHeaderUpdater.persistDecrementRowCount(
                tableFile
        );

        TableHeader persisted =
                TableHeaderIO.read(
                        tableFile
                );

        assertEquals(
                2L,
                persisted.getRowCount()
        );

        assertEquals(
                100L,
                persisted.getFirstDataPageId()
        );

        assertEquals(
                103L,
                persisted.getLastDataPageId()
        );
    }
    @Test
    void shouldLeaveFileUnchangedAfterRejectedMutation()
            throws IOException {

        Path tableFile =
                createTableFile(
                        10L,
                        -1L,
                        -1L
                );

        byte[] before =
                Files.readAllBytes(
                        tableFile
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .persistDataPageRange(
                                tableFile,
                                50L,
                                20L
                        )
        );

        byte[] after =
                Files.readAllBytes(
                        tableFile
                );

        assertArrayEquals(
                before,
                after
        );
    }
}
