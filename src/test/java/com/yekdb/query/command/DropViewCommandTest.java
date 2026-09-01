package com.yekdb.query.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropViewCommandTest {

    @Test
    void shouldCreateDropViewCommand() {
        DropViewCommand command =
                new DropViewCommand("  adults  ");

        assertEquals(
                "adults",
                command.getViewName()
        );
    }

    @Test
    void shouldRejectBlankViewName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DropViewCommand("   ")
        );
    }
}
