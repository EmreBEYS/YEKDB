package com.yekdb.query.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateViewCommandTest {

    @Test
    void shouldCreateCreateViewCommand() {
        CreateViewCommand command =
                new CreateViewCommand(
                        "adult_users",
                        "SELECT id, name FROM users WHERE age >= 18"
                );

        assertEquals("adult_users", command.getViewName());
        assertEquals(
                "SELECT id, name FROM users WHERE age >= 18",
                command.getSourceSelect()
        );
    }

    @Test
    void shouldTrimValues() {
        CreateViewCommand command =
                new CreateViewCommand(
                        "  adult_users  ",
                        "  SELECT * FROM users  "
                );

        assertEquals("adult_users", command.getViewName());
        assertEquals(
                "SELECT * FROM users",
                command.getSourceSelect()
        );
    }

    @Test
    void shouldRejectBlankValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateViewCommand(
                        "   ",
                        "SELECT * FROM users"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateViewCommand(
                        "adult_users",
                        "   "
                )
        );
    }
}
