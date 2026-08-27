package com.yekdb.storage.table;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.constraint.exception.InvalidForeignKeyConstraintException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeignKeySchemaValidationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldAllowForeignKeyReferencingPrimaryKey() {
        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                usersTableWithPrimaryKey()
        );

        assertDoesNotThrow(() ->
                manager.createTable(
                        ordersTable(
                                DataType.INT,
                                "users",
                                "id"
                        )
                )
        );

        assertTrue(manager.exists("orders"));
    }

    @Test
    void shouldAllowForeignKeyReferencingUniqueColumn() {
        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("username", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new UniqueConstraint("username")
                        )
                )
        );

        Table child =
                new Table(
                        "profiles",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("username", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        "username",
                                        "users",
                                        "username"
                                )
                        )
                );

        assertDoesNotThrow(() ->
                manager.createTable(child)
        );
    }

    @Test
    void shouldRejectUnknownReferencedTable() {
        TableManager manager =
                new TableManager(tempDirectory);

        InvalidForeignKeyConstraintException exception =
                assertThrows(
                        InvalidForeignKeyConstraintException.class,
                        () -> manager.createTable(
                                ordersTable(
                                        DataType.INT,
                                        "missing_users",
                                        "id"
                                )
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("unknown table")
        );

        assertFalse(manager.exists("orders"));
    }

    @Test
    void shouldRejectUnknownReferencedColumn() {
        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                usersTableWithPrimaryKey()
        );

        assertThrows(
                InvalidForeignKeyConstraintException.class,
                () -> manager.createTable(
                        ordersTable(
                                DataType.INT,
                                "users",
                                "missing_id"
                        )
                )
        );

        assertFalse(manager.exists("orders"));
    }

    @Test
    void shouldRejectForeignKeyTypeMismatch() {
        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                usersTableWithPrimaryKey()
        );

        InvalidForeignKeyConstraintException exception =
                assertThrows(
                        InvalidForeignKeyConstraintException.class,
                        () -> manager.createTable(
                                ordersTable(
                                        DataType.STRING,
                                        "users",
                                        "id"
                                )
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("type mismatch")
        );
    }

    @Test
    void shouldRejectReferenceToNonUniqueColumn() {
        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("age", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id")
                        )
                )
        );

        assertThrows(
                InvalidForeignKeyConstraintException.class,
                () -> manager.createTable(
                        ordersTable(
                                DataType.INT,
                                "users",
                                "age"
                        )
                )
        );
    }

    @Test
    void shouldAllowCompositeForeignKeyReferencingCompositeUniqueConstraint() {
        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "cities",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("country_code", DataType.STRING),
                                new Column("city_code", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new UniqueConstraint(
                                        List.of(
                                                "country_code",
                                                "city_code"
                                        )
                                )
                        )
                )
        );

        Table addresses =
                new Table(
                        "addresses",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("country_code", DataType.STRING),
                                new Column("city_code", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
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
                                )
                        )
                );

        assertDoesNotThrow(() ->
                manager.createTable(addresses)
        );
    }

    private Table usersTableWithPrimaryKey() {
        return new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                ),
                List.of(
                        new PrimaryKeyConstraint("id")
                )
        );
    }

    private Table ordersTable(
            DataType userIdType,
            String referencedTable,
            String referencedColumn
    ) {
        return new Table(
                "orders",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", userIdType)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id",
                                referencedTable,
                                referencedColumn
                        )
                )
        );
    }
}
