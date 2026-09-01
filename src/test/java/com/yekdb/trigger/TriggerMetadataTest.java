package com.yekdb.trigger;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TriggerMetadataTest {

    @Test
    void shouldCreateTriggerMetadataWithDefaults() {
        TriggerMetadata metadata =
                new TriggerMetadata(
                        "Users_Insert_Log",
                        "Users"
                );

        assertEquals("users_insert_log", metadata.getTriggerName());
        assertEquals("users", metadata.getTableName());
        assertNotNull(metadata.getCreatedAt());
        assertEquals(1, metadata.getVersion());
    }

    @Test
    void shouldCreateTriggerMetadataFromExistingValues() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 1, 11, 0);

        TriggerMetadata metadata =
                new TriggerMetadata(
                        "users_insert_log",
                        "users",
                        createdAt,
                        2
                );

        assertEquals("users_insert_log", metadata.getTriggerName());
        assertEquals("users", metadata.getTableName());
        assertEquals(createdAt, metadata.getCreatedAt());
        assertEquals(2, metadata.getVersion());
    }

    @Test
    void shouldRejectInvalidVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TriggerMetadata(
                        "users_insert_log",
                        "users",
                        LocalDateTime.now(),
                        0
                )
        );
    }
}
