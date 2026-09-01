package com.yekdb.query.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropTriggerCommandTest {

    @Test
    void shouldCreateDropTriggerCommand() {
        DropTriggerCommand command =
                new DropTriggerCommand(
                        "  users_insert_log  "
                );

        assertEquals(
                "users_insert_log",
                command.getTriggerName()
        );
    }

    @Test
    void shouldRejectBlankTriggerName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DropTriggerCommand("   ")
        );
    }
}
