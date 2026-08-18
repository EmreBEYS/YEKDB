package com.yekdb.table;

import com.yekdb.table.exception.CorruptedTableFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TableFileMetadataReaderTest {

    @TempDir
    Path tempDirectory;

    private final TableFileMetadataReader reader =
            new TableFileMetadataReader();

    @Test
    void shouldReadValidTableFile() throws IOException {

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        Files.writeString(
                tableFile,
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
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
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
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:15:00
                columns=
                id:INT
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
                """
                INVALID_HEADER
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=2
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:00:00
                columns=
                id:INVALID_TYPE
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                columnCount=1
                createdAt=2026-08-18T08:00:00
                columns=
                id:INT
                """,
                StandardCharsets.UTF_8
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

        Files.writeString(
                tableFile,
                """
                YEKDB_TABLE
                version=1
                tableName=users
                """,
                StandardCharsets.UTF_8
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