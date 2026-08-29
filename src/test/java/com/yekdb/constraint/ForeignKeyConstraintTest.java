package com.yekdb.constraint;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForeignKeyConstraintTest {

    @Test
    void shouldCreateSingleColumnForeignKey() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        "user_id",
                        "users",
                        "id"
                );

        assertEquals(
                ConstraintType.FOREIGN_KEY,
                constraint.type()
        );

        assertEquals(
                List.of("user_id"),
                constraint.columns()
        );

        assertEquals(
                "users",
                constraint.referencedTableName()
        );

        assertEquals(
                List.of("id"),
                constraint.referencedColumnNames()
        );

        assertEquals(
                "id",
                constraint.referencedColumnName()
        );

        assertFalse(constraint.isComposite());
    }

    @Test
    void shouldCreateCompositeForeignKeyMetadata() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        List.of(
                                "country_code",
                                "city_code"
                        ),
                        "cities",
                        List.of(
                                "country_code",
                                "city_code"
                        )
                );

        assertEquals(
                ConstraintType.FOREIGN_KEY,
                constraint.type()
        );

        assertEquals(
                List.of(
                        "country_code",
                        "city_code"
                ),
                constraint.columns()
        );

        assertEquals(
                "cities",
                constraint.referencedTableName()
        );

        assertEquals(
                List.of(
                        "country_code",
                        "city_code"
                ),
                constraint.referencedColumnNames()
        );

        assertTrue(constraint.isComposite());
    }

    @Test
    void shouldRejectNullReferencedTableName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ForeignKeyConstraint(
                        "user_id",
                        null,
                        "id"
                )
        );
    }

    @Test
    void shouldRejectBlankReferencedTableName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ForeignKeyConstraint(
                        "user_id",
                        "   ",
                        "id"
                )
        );
    }

    @Test
    void shouldRejectNullReferencedColumnList() {
        assertThrows(
                NullPointerException.class,
                () -> new ForeignKeyConstraint(
                        List.of("user_id"),
                        "users",
                        null
                )
        );
    }

    @Test
    void shouldRejectEmptyReferencedColumnList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ForeignKeyConstraint(
                        List.of("user_id"),
                        "users",
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectMismatchedColumnCounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ForeignKeyConstraint(
                        List.of(
                                "country_code",
                                "city_code"
                        ),
                        "cities",
                        List.of("id")
                )
        );
    }

    @Test
    void shouldRejectSingleColumnAccessorForCompositeForeignKey() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        List.of(
                                "country_code",
                                "city_code"
                        ),
                        "cities",
                        List.of(
                                "country_code",
                                "city_code"
                        )
                );

        assertThrows(
                IllegalStateException.class,
                constraint::referencedColumnName
        );
    }


    @Test
    void shouldDefaultReferentialActionsToRestrict() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        "user_id",
                        "users",
                        "id"
                );

        assertEquals(
                ReferentialAction.RESTRICT,
                constraint.onDelete()
        );

        assertEquals(
                ReferentialAction.RESTRICT,
                constraint.onUpdate()
        );
    }

    @Test
    void shouldStoreExplicitReferentialActions() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        "user_id",
                        "users",
                        "id",
                        ReferentialAction.CASCADE,
                        ReferentialAction.SET_NULL
                );

        assertEquals(
                ReferentialAction.CASCADE,
                constraint.onDelete()
        );

        assertEquals(
                ReferentialAction.SET_NULL,
                constraint.onUpdate()
        );
    }

    @Test
    void shouldStoreExplicitReferentialActionsForCompositeForeignKey() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        List.of("country_code", "city_code"),
                        "cities",
                        List.of("country_code", "city_code"),
                        ReferentialAction.SET_NULL,
                        ReferentialAction.CASCADE
                );

        assertEquals(
                ReferentialAction.SET_NULL,
                constraint.onDelete()
        );

        assertEquals(
                ReferentialAction.CASCADE,
                constraint.onUpdate()
        );
    }

    @Test
    void shouldFallbackNullReferentialActionsToRestrict() {
        ForeignKeyConstraint constraint =
                new ForeignKeyConstraint(
                        "user_id",
                        "users",
                        "id",
                        null,
                        null
                );

        assertEquals(
                ReferentialAction.RESTRICT,
                constraint.onDelete()
        );

        assertEquals(
                ReferentialAction.RESTRICT,
                constraint.onUpdate()
        );
    }
}