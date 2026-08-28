package com.yekdb.storage.table;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.constraint.exception.ForeignKeyConstraintViolationException;
import com.yekdb.constraint.exception.UniqueConstraintViolationException;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlterTablePhase5Test {

    @TempDir
    Path tempDir;

    @Test
    void addUniqueConstraint_shouldPersistOnEmptyTable() {
        TableManager manager = new TableManager(tempDir);
        manager.createTable("users", List.of(
                new Column("id", DataType.INT),
                new Column("email", DataType.STRING)
        ));

        manager.addConstraint(
                "users",
                new UniqueConstraint("email")
        );

        Table recovered = reload().getTable("users");

        assertTrue(recovered.getConstraints().stream()
                .anyMatch(c -> c instanceof UniqueConstraint
                        && c.columns().equals(List.of("email"))));
    }

    @Test
    void addPrimaryKey_shouldPersistOnEmptyTable() {
        TableManager manager = new TableManager(tempDir);
        manager.createTable("users", List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING)
        ));

        manager.addConstraint(
                "users",
                new PrimaryKeyConstraint("id")
        );

        Table recovered = reload().getTable("users");

        assertTrue(recovered.getConstraints().stream()
                .anyMatch(c -> c instanceof PrimaryKeyConstraint
                        && c.columns().equals(List.of("id"))));
    }

    @Test
    void addUniqueConstraint_shouldRejectExistingDuplicates() throws Exception {
        TableManager manager = new TableManager(tempDir);
        manager.createTable("users", List.of(
                new Column("id", DataType.INT),
                new Column("email", DataType.STRING)
        ));

        insert("users", new Row(List.of(1, "same@example.com")));
        insert("users", new Row(List.of(2, "same@example.com")));

        assertThrows(
                UniqueConstraintViolationException.class,
                () -> manager.addConstraint(
                        "users",
                        new UniqueConstraint("email")
                )
        );

        assertTrue(manager.getTable("users").getConstraints().isEmpty());
    }

    @Test
    void addForeignKey_shouldPersistWhenExistingRowsAreValid() throws Exception {
        TableManager manager = new TableManager(tempDir);
        manager.createTable(
                "users",
                List.of(new Column("id", DataType.INT)),
                List.of(new PrimaryKeyConstraint("id"))
        );
        manager.createTable("orders", List.of(
                new Column("id", DataType.INT),
                new Column("user_id", DataType.INT)
        ));

        insert("users", new Row(List.of(10)));
        insert("orders", new Row(List.of(1, 10)));

        manager.addConstraint(
                "orders",
                new ForeignKeyConstraint(
                        List.of("user_id"),
                        "users",
                        List.of("id")
                )
        );

        assertEquals(1, manager.getTable("orders").getConstraints().size());
    }

    @Test
    void addForeignKey_shouldRejectOrphanExistingRows() throws Exception {
        TableManager manager = new TableManager(tempDir);
        manager.createTable(
                "users",
                List.of(new Column("id", DataType.INT)),
                List.of(new PrimaryKeyConstraint("id"))
        );
        manager.createTable("orders", List.of(
                new Column("id", DataType.INT),
                new Column("user_id", DataType.INT)
        ));

        insert("orders", new Row(List.of(1, 999)));

        assertThrows(
                ForeignKeyConstraintViolationException.class,
                () -> manager.addConstraint(
                        "orders",
                        new ForeignKeyConstraint(
                                List.of("user_id"),
                                "users",
                                List.of("id")
                        )
                )
        );
    }

    private TableManager reload() {
        TableManager manager = new TableManager(tempDir);
        manager.loadCatalog();
        return manager;
    }

    private void insert(
            String tableName,
            Row row
    ) throws Exception {
        Path dataFile = tempDir.resolve(
                tableName + ".data"
        );

        StorageEngine storageEngine = new StorageEngine(dataFile);
        storageEngine.initialize();

        try {
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );
            recordManager.insert(row);
        } finally {
            storageEngine.shutdown();
        }
    }
}
