package com.yekdb.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TriggerDefinitionTest {

    @Test
    void shouldCreateTriggerDefinitionWithNormalizedNames() {
        TriggerDefinition definition =
                new TriggerDefinition(
                        "  Users_Insert_Log  ",
                        "  Users  ",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT,
                        "INSERT INTO audit_log VALUES (NEW.id)"
                );

        assertEquals("users_insert_log", definition.getTriggerName());
        assertEquals("users", definition.getTableName());
        assertEquals(TriggerTiming.AFTER, definition.getTiming());
        assertEquals(TriggerEvent.INSERT, definition.getEvent());
        assertEquals(
                "INSERT INTO audit_log VALUES (NEW.id)",
                definition.getBody()
        );
    }

    @Test
    void shouldMatchByTableTimingAndEvent() {
        TriggerDefinition definition =
                new TriggerDefinition(
                        "users_before_update",
                        "users",
                        TriggerTiming.BEFORE,
                        TriggerEvent.UPDATE,
                        "SET NEW.updated_at = NOW()"
                );

        assertTrue(
                definition.matches(
                        "USERS",
                        TriggerTiming.BEFORE,
                        TriggerEvent.UPDATE
                )
        );
        assertFalse(
                definition.matches(
                        "users",
                        TriggerTiming.AFTER,
                        TriggerEvent.UPDATE
                )
        );
    }

    @Test
    void shouldRejectInvalidDefinitionValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TriggerDefinition(
                        "123bad",
                        "users",
                        TriggerTiming.BEFORE,
                        TriggerEvent.INSERT,
                        "body"
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new TriggerDefinition(
                        "users_log",
                        "users",
                        null,
                        TriggerEvent.INSERT,
                        "body"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new TriggerDefinition(
                        "users_log",
                        "users",
                        TriggerTiming.BEFORE,
                        TriggerEvent.INSERT,
                        "   "
                )
        );
    }
}
