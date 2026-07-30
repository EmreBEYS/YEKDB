package com.yekdb.table;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TableMetadataTest {

    @Test
    void shouldCreateMetadataSuccessfully() {

        TableMetadata metadata =
                new TableMetadata("users", 3);

        assertEquals("users", metadata.getTableName());
        assertEquals(3, metadata.getColumnCount());
        assertEquals("users.tbl", metadata.getFileName());
        assertEquals(1, metadata.getVersion());

        assertNotNull(metadata.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenTableNameIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TableMetadata(null, 3)
        );
    }

    @Test
    void shouldThrowExceptionWhenTableNameIsBlank() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TableMetadata("   ", 3)
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnCountIsZero() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TableMetadata("users", 0)
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnCountIsNegative() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TableMetadata("users", -5)
        );
    }

    @Test
    void shouldNormalizeTableName() {

        TableMetadata metadata =
                new TableMetadata(" USERS ", 5);

        assertEquals("users", metadata.getTableName());
        assertEquals("users.tbl", metadata.getFileName());
    }

    @Test
    void shouldReturnEqualMetadata() {

        LocalDateTime time =
                LocalDateTime.of(
                        2026,
                        7,
                        30,
                        10,
                        30
                );

        TableMetadata first =
                new TableMetadata(
                        "users",
                        3,
                        time,
                        "users.tbl",
                        1
                );

        TableMetadata second =
                new TableMetadata(
                        "USERS",
                        3,
                        time,
                        "users.tbl",
                        1
                );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldGenerateReadableToString() {

        TableMetadata metadata =
                new TableMetadata("users", 2);

        String result = metadata.toString();

        assertTrue(result.contains("users"));
        assertTrue(result.contains("users.tbl"));
        assertTrue(result.contains("columnCount"));
    }

}