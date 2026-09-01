package com.yekdb.view;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ViewMetadataTest {

    @Test
    void shouldCreateViewMetadataWithDefaults() {
        ViewMetadata metadata =
                new ViewMetadata("Adult_Users");

        assertEquals("adult_users", metadata.getViewName());
        assertNotNull(metadata.getCreatedAt());
        assertEquals(1, metadata.getVersion());
    }

    @Test
    void shouldCreateViewMetadataFromExistingValues() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 1, 10, 30);

        ViewMetadata metadata =
                new ViewMetadata(
                        "adult_users",
                        createdAt,
                        2
                );

        assertEquals("adult_users", metadata.getViewName());
        assertEquals(createdAt, metadata.getCreatedAt());
        assertEquals(2, metadata.getVersion());
    }

    @Test
    void shouldRejectInvalidVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ViewMetadata(
                        "adult_users",
                        LocalDateTime.now(),
                        0
                )
        );
    }
}
