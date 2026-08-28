package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlterTablePhase6Test {

    @TempDir
    Path tempDir;

    @Test
    void namedUniqueConstraint_shouldPersistItsName() {
        TableManager manager = new TableManager(tempDir);
        manager.createTable("users", List.of(
                new Column("id", DataType.INT),
                new Column("email", DataType.STRING)
        ));

        manager.addConstraint(
                "users",
                new UniqueConstraint(
                        "uq_users_email",
                        List.of("email")
                )
        );

        Table recovered = reload().getTable("users");
        Constraint constraint = recovered.getConstraints().getFirst();

        assertEquals("uq_users_email", constraint.name());
        assertEquals(List.of("email"), constraint.columns());
    }

    @Test
    void dropConstraint_shouldRemoveNamedConstraintAndPersistRemoval() {
        TableManager manager = new TableManager(tempDir);
        manager.createTable("users", List.of(
                new Column("id", DataType.INT),
                new Column("email", DataType.STRING)
        ));

        manager.addConstraint(
                "users",
                new UniqueConstraint(
                        "uq_users_email",
                        List.of("email")
                )
        );

        manager.dropConstraint(
                "users",
                "UQ_USERS_EMAIL"
        );

        assertTrue(manager.getTable("users").getConstraints().isEmpty());
        assertTrue(reload().getTable("users").getConstraints().isEmpty());
    }

    @Test
    void dropConstraint_shouldRejectCandidateKeyReferencedByForeignKey() {
        TableManager manager = new TableManager(tempDir);

        manager.createTable("users", List.of(
                new Column("id", DataType.INT)
        ));

        manager.addConstraint(
                "users",
                new PrimaryKeyConstraint(
                        "pk_users",
                        List.of("id")
                )
        );

        manager.createTable("orders", List.of(
                new Column("id", DataType.INT),
                new Column("user_id", DataType.INT)
        ));

        manager.addConstraint(
                "orders",
                new ForeignKeyConstraint(
                        "fk_orders_user",
                        List.of("user_id"),
                        "users",
                        List.of("id")
                )
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> manager.dropConstraint(
                        "users",
                        "pk_users"
                )
        );

        assertTrue(exception.getMessage().contains("referenced by FOREIGN KEY"));
        assertEquals(1, manager.getTable("users").getConstraints().size());
    }

    @Test
    void table_shouldRejectDuplicateExplicitConstraintNames() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("email", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint(
                                        "same_name",
                                        List.of("id")
                                ),
                                new UniqueConstraint(
                                        "SAME_NAME",
                                        List.of("email")
                                )
                        )
                )
        );

        assertTrue(exception.getMessage().contains("Duplicate constraint names"));
    }

    @Test
    void legacyUnnamedConstraintMetadata_shouldRemainReadable() {
        List<Constraint> constraints = ConstraintSchemaCodec.deserialize(
                List.of(
                        "schema-version=1",
                        ConstraintSchemaCodec.CONSTRAINTS_HEADER,
                        "UNIQUE:email",
                        "PRIMARY_KEY:id"
                ),
                tempDir.resolve("legacy.tbl")
        );

        assertEquals(2, constraints.size());
        assertNull(constraints.get(0).name());
        assertNull(constraints.get(1).name());
    }

    private TableManager reload() {
        TableManager manager = new TableManager(tempDir);
        manager.loadCatalog();
        return manager;
    }
}
