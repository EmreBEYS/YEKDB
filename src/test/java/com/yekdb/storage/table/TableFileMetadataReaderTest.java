package com.yekdb.storage.table;

import com.yekdb.storage.table.*;
import com.yekdb.storage.exception.CorruptedTableFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderConstants;
import com.yekdb.storage.table.header.TableHeaderSerializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TableFileMetadataReaderTest {

    private void writeTableFile(
            Path file,
            String tableName,
            int columnCount,
            String schemaContent
    ) throws IOException {

        TableHeader header =
                new TableHeader(
                        1L,
                        tableName,
                        columnCount,
                        0L,
                        -1L,
                        -1L,
                        TableHeaderConstants.HEADER_SIZE,
                        TableHeaderConstants.FLAG_NONE
                );

        byte[] headerBytes =
                TableHeaderSerializer.serialize(header);

        byte[] schemaBytes =
                schemaContent.getBytes(
                        StandardCharsets.UTF_8
                );

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        headerBytes.length
                                + schemaBytes.length
                );

        buffer.put(headerBytes);
        buffer.put(schemaBytes);

        Files.write(
                file,
                buffer.array()
        );
    }

    @TempDir
    Path tempDirectory;

    private final TableFileMetadataReader reader =
            new TableFileMetadataReader();

    @Test
    void shouldReadValidTableFile() throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        writeTableFile(
                tableFile,
                "users",
                3,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=3
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                username:STRING
                active:BOOLEAN
                """
        );

        TableRecoveryEntry entry =
                reader.read(tableFile);

        assertNotNull(entry);
        assertNotNull(entry.table());
        assertNotNull(entry.metadata());

        assertEquals(
                "users",
                entry.table().getTableName()
        );

        assertEquals(
                3,
                entry.table().getColumnCount()
        );

        assertEquals(
                "users",
                entry.metadata().getTableName()
        );

        assertEquals(
                3,
                entry.metadata().getColumnCount()
        );
    }

    @Test
    void shouldRecoverColumnDefinitions()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        writeTableFile(
                tableFile,
                "users",
                3,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=3
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                username:STRING
                active:BOOLEAN
                """
        );

        Table table =
                reader.read(tableFile).table();

        assertEquals(
                DataType.INT,
                table.getColumn("id")
                        .getDataType()
        );

        assertEquals(
                DataType.STRING,
                table.getColumn("username")
                        .getDataType()
        );

        assertEquals(
                DataType.BOOLEAN,
                table.getColumn("active")
                        .getDataType()
        );
    }

    @Test
    void shouldPreserveCreatedAt()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        LocalDateTime createdAt =
                LocalDateTime.of(
                        2026,
                        8,
                        18,
                        8,
                        15
                );

        writeTableFile(
                tableFile,
                "users",
                1,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:15:00
                columns=
                id:INT
                """
        );

        TableMetadata metadata =
                reader.read(tableFile)
                        .metadata();

        assertEquals(
                createdAt,
                metadata.getCreatedAt()
        );

        assertEquals(
                1,
                metadata.getVersion()
        );

        assertEquals(
                "users.tbl",
                metadata.getFileName()
        );
    }

    @Test
    void shouldRejectInvalidMagicHeader()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        writeTableFile(
                tableFile,
                "users",
                1,
                """
                INVALID_HEADER
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                """
        );

        assertThrows(
                CorruptedTableFileException.class,
                () -> reader.read(tableFile)
        );
    }

    @Test
    void shouldRejectWrongColumnCount()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        writeTableFile(
                tableFile,
                "users",
                2,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=2
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                """
        );

        assertThrows(
                CorruptedTableFileException.class,
                () -> reader.read(tableFile)
        );
    }

    @Test
    void shouldRejectInvalidDataType()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        writeTableFile(
                tableFile,
                "users",
                1,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:00:00
                columns=
                id:INVALID_TYPE
                """
        );

        assertThrows(
                CorruptedTableFileException.class,
                () -> reader.read(tableFile)
        );
    }

    @Test
    void shouldRejectFileNameMismatch()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("wrong.tbl");

        writeTableFile(
                tableFile,
                "users",
                1,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                """
        );

        assertThrows(
                CorruptedTableFileException.class,
                () -> reader.read(tableFile)
        );
    }

    @Test
    void shouldRejectIncompleteTableFile()
            throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        writeTableFile(
                tableFile,
                "users",
                1,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                """
        );

        assertThrows(
                CorruptedTableFileException.class,
                () -> reader.read(tableFile)
        );
    }

    @Test
    void shouldRejectMissingTableFile() {

        Path tableFile =
                tempDirectory.resolve("missing.tbl");

        assertThrows(
                CorruptedTableFileException.class,
                () -> reader.read(tableFile)
        );
    }

    @Test
    void shouldRejectNullPath() {

        assertThrows(
                IllegalArgumentException.class,
                () -> reader.read(null)
        );
    }
}