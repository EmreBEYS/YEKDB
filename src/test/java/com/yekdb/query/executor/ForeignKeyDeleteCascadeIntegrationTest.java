package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.exception.ForeignKeyDeleteRestrictedException;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sprint 00-27 Phase 4 integration tests for recursive ON DELETE CASCADE.
 */
class ForeignKeyDeleteCascadeIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor = new InsertExecutor();
    private final DeleteExecutor deleteExecutor = new DeleteExecutor();
    private final ExpressionParser expressionParser = new ExpressionParser();

    @Test
    void shouldCascadeDeleteMultipleChildren() throws Exception {
        TableManager manager = createUsersOrdersSchema(ReferentialAction.CASCADE);

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "orders", List.of("id", "user_id"), List.of(11, 1), true);

        assertEquals(1, deleteById(manager, "users", 1));
        assertEquals(0, activeRecordCount("users"));
        assertEquals(0, activeRecordCount("orders"));
    }

    @Test
    void shouldCascadeRecursivelyAcrossThreeTables() throws Exception {
        TableManager manager = createUsersOrdersSchema(ReferentialAction.CASCADE);

        manager.createTable(
                new Table(
                        "order_items",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("order_id", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        "order_id",
                                        "orders",
                                        "id",
                                        ReferentialAction.CASCADE,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "order_items", List.of("id", "order_id"), List.of(100, 10), true);
        insert(manager, "order_items", List.of("id", "order_id"), List.of(101, 10), true);

        assertEquals(1, deleteById(manager, "users", 1));

        assertEquals(0, activeRecordCount("users"));
        assertEquals(0, activeRecordCount("orders"));
        assertEquals(0, activeRecordCount("order_items"));
    }

    @Test
    void shouldRejectEntireCascadeWhenDescendantIsRestrictReferenced()
            throws Exception {
        TableManager manager = createUsersOrdersSchema(ReferentialAction.CASCADE);

        manager.createTable(
                new Table(
                        "payments",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("order_id", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        "order_id",
                                        "orders",
                                        "id",
                                        ReferentialAction.RESTRICT,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "payments", List.of("id", "order_id"), List.of(500, 10), true);

        assertThrows(
                ForeignKeyDeleteRestrictedException.class,
                () -> deleteById(manager, "users", 1)
        );

        // Validation occurs before cascade mutation: no partial delete.
        assertEquals(1, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("orders"));
        assertEquals(1, activeRecordCount("payments"));
    }

    private TableManager createUsersOrdersSchema(
            ReferentialAction orderDeleteAction
    ) {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(new Column("id", DataType.INT)),
                        List.of(new PrimaryKeyConstraint("id"))
                )
        );

        manager.createTable(
                new Table(
                        "orders",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("user_id", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        "user_id",
                                        "users",
                                        "id",
                                        orderDeleteAction,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        return manager;
    }

    private void insert(
            TableManager manager,
            String tableName,
            List<String> columns,
            List<Object> values,
            boolean validateForeignKey
    ) throws Exception {
        Table table = manager.getTable(tableName);
        StorageEngine storageEngine = new StorageEngine(dataFile(tableName));

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            InsertCommand command = new InsertCommand(
                    tableName,
                    columns,
                    values
            );

            if (validateForeignKey) {
                insertExecutor.execute(table, command, recordManager, manager);
            } else {
                insertExecutor.execute(table, command, recordManager);
            }
        } finally {
            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }

    private int deleteById(
            TableManager manager,
            String tableName,
            int id
    ) throws Exception {
        Table table = manager.getTable(tableName);
        StorageEngine storageEngine = new StorageEngine(dataFile(tableName));

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            return deleteExecutor.execute(
                    table,
                    new DeleteCommand(
                            tableName,
                            expressionParser.parse("id = " + id)
                    ),
                    recordManager,
                    manager
            );
        } finally {
            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }

    private int activeRecordCount(String tableName) throws Exception {
        StorageEngine storageEngine = new StorageEngine(dataFile(tableName));

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );
            return recordManager.getActiveRecords().size();
        } finally {
            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }

    private Path dataFile(String tableName) {
        return tempDirectory.resolve(tableName + ".data");
    }
}
