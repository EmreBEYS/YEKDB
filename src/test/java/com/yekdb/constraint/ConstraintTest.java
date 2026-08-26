package com.yekdb.constraint;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintTest {

    @Test
    void shouldCreateNotNullConstraint() {
        NotNullConstraint constraint =
                new NotNullConstraint("email");

        assertEquals(
                ConstraintType.NOT_NULL,
                constraint.type()
        );

        assertEquals(
                List.of("email"),
                constraint.columns()
        );
    }

    @Test
    void shouldCreateSingleColumnUniqueConstraint() {
        UniqueConstraint constraint =
                new UniqueConstraint("username");

        assertEquals(
                ConstraintType.UNIQUE,
                constraint.type()
        );

        assertEquals(
                List.of("username"),
                constraint.columns()
        );
    }

    @Test
    void shouldCreateCompositeUniqueConstraint() {
        UniqueConstraint constraint =
                new UniqueConstraint(
                        List.of("first_name", "last_name")
                );

        assertEquals(
                List.of("first_name", "last_name"),
                constraint.columns()
        );
    }

    @Test
    void shouldCreateSingleColumnPrimaryKey() {
        PrimaryKeyConstraint constraint =
                new PrimaryKeyConstraint("id");

        assertEquals(
                ConstraintType.PRIMARY_KEY,
                constraint.type()
        );

        assertEquals(
                List.of("id"),
                constraint.columns()
        );
    }

    @Test
    void shouldCreateCompositePrimaryKey() {
        PrimaryKeyConstraint constraint =
                new PrimaryKeyConstraint(
                        List.of("student_id", "course_id")
                );

        assertEquals(
                List.of("student_id", "course_id"),
                constraint.columns()
        );
    }

    @Test
    void shouldRejectEmptyPrimaryKeyColumns() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PrimaryKeyConstraint(List.of())
        );
    }

    @Test
    void shouldRejectDuplicatePrimaryKeyColumns() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PrimaryKeyConstraint(
                        List.of("id", "id")
                )
        );
    }

    @Test
    void shouldRejectDuplicateUniqueColumns() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UniqueConstraint(
                        List.of("email", "email")
                )
        );
    }
}