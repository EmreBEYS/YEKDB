package com.yekdb.query.command;

import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerTiming;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateTriggerCommandTest {

    @Test
    void shouldCreateCreateTriggerCommand() {
        CreateTriggerCommand command =
                new CreateTriggerCommand(
                        "users_insert_log",
                        "users",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT,
                        "INSERT INTO audit_log VALUES (NEW.id)"
                );

        assertEquals("users_insert_log", command.getTriggerName());
        assertEquals("users", command.getTableName());
        assertEquals(TriggerTiming.AFTER, command.getTiming());
        assertEquals(TriggerEvent.INSERT, command.getEvent());
        assertEquals(
                "INSERT INTO audit_log VALUES (NEW.id)",
                command.getBody()
        );
    }

    @Test
    void shouldTrimTextValues() {
        CreateTriggerCommand command =
                new CreateTriggerCommand(
                        "  users_insert_log  ",
                        "  users  ",
                        TriggerTiming.BEFORE,
                        TriggerEvent.UPDATE,
                        "  SET NEW.updated_at = NOW()  "
                );

        assertEquals("users_insert_log", command.getTriggerName());
        assertEquals("users", command.getTableName());
        assertEquals(
                "SET NEW.updated_at = NOW()",
                command.getBody()
        );
    }

    @Test
    void shouldRejectBlankValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateTriggerCommand(
                        "   ",
                        "users",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT,
                        "body"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateTriggerCommand(
                        "users_insert_log",
                        "   ",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT,
                        "body"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateTriggerCommand(
                        "users_insert_log",
                        "users",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT,
                        "   "
                )
        );
    }

    @Test
    void shouldRejectNullTimingAndEvent() {
        assertThrows(
                NullPointerException.class,
                () -> new CreateTriggerCommand(
                        "users_insert_log",
                        "users",
                        null,
                        TriggerEvent.INSERT,
                        "body"
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new CreateTriggerCommand(
                        "users_insert_log",
                        "users",
                        TriggerTiming.AFTER,
                        null,
                        "body"
                )
        );
    }
}
