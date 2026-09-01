package com.yekdb.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViewDefinitionTest {

    @Test
    void shouldCreateViewDefinitionWithNormalizedName() {
        ViewDefinition definition =
                new ViewDefinition(
                        "  Adult_Users  ",
                        "  SELECT id, name FROM users WHERE age >= 18  "
                );

        assertEquals("adult_users", definition.getViewName());
        assertEquals(
                "SELECT id, name FROM users WHERE age >= 18",
                definition.getSourceSelect()
        );
    }

    @Test
    void shouldRejectInvalidViewName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ViewDefinition(
                        "123users",
                        "SELECT * FROM users"
                )
        );
    }

    @Test
    void shouldRejectBlankSourceSelect() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ViewDefinition(
                        "adult_users",
                        "   "
                )
        );
    }
}
