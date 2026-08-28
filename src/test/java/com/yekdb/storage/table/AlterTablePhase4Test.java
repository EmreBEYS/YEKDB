package com.yekdb.storage.table;

import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlterTablePhase4Test {

    @TempDir
    Path tempDirectory;

    private TableManager tableManager;

    @BeforeEach
    void setUp() {
        tableManager = new TableManager(
                tempDirectory.resolve("db")
        );
    }

    @Test
    void shouldRenameTableAndPersistNewMetadata() throws Exception {
        tableManager.createTable(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        TableMetadata metadata =
                tableManager.renameTable(
                        "users",
                        "customers"
                );

        assertEquals("customers", metadata.getTableName());
        assertEquals("customers.tbl", metadata.getFileName());
        assertFalse(tableManager.exists("users"));
        assertTrue(tableManager.exists("customers"));

        Path oldFile =
                tempDirectory.resolve("db/users.tbl");
        Path newFile =
                tempDirectory.resolve("db/customers.tbl");

        assertFalse(Files.exists(oldFile));
        assertTrue(Files.exists(newFile));

        TableHeader header =
                TableHeaderIO.read(newFile);

        assertEquals("customers", header.getTableName());
    }

    @Test
    void shouldSetAndDropExplicitNotNullConstraint() {
        tableManager.createTable(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("email", DataType.STRING)
                )
        );

        tableManager.setColumnNotNull(
                "users",
                "email"
        );

        assertTrue(
                tableManager.getTable("users")
                        .getConstraints()
                        .stream()
                        .anyMatch(constraint ->
                                constraint.type() == ConstraintType.NOT_NULL
                                        && constraint.columns().equals(List.of("email")))
        );

        tableManager.dropColumnNotNull(
                "users",
                "email"
        );

        assertFalse(
                tableManager.getTable("users")
                        .getConstraints()
                        .stream()
                        .anyMatch(constraint ->
                                constraint.type() == ConstraintType.NOT_NULL
                                        && constraint.columns().equals(List.of("email")))
        );
    }

    @Test
    void shouldRejectDroppingNotNullFromPrimaryKeyColumn() {
        tableManager.createTable(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new NotNullConstraint("id")
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> tableManager.dropColumnNotNull(
                                "users",
                                "id"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("PRIMARY KEY")
        );
    }

    @Test
    void shouldRejectRenameWhenTableIsReferencedByForeignKey() {
        tableManager.createTable(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id")
                )
        );

        tableManager.createTable(
                "orders",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new ForeignKeyConstraint(
                                "user_id",
                                "users",
                                "id"
                        )
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> tableManager.renameTable(
                                "users",
                                "customers"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("FOREIGN KEY")
        );
    }
}
