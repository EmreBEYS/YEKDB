package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.constraint.exception.ForeignKeyUpdateRestrictedException;
import com.yekdb.constraint.exception.NotNullConstraintViolationException;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForeignKeyUpdateReferentialActionIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor = new InsertExecutor();
    private final UpdateExecutor updateExecutor = new UpdateExecutor();
    private final ExpressionParser expressionParser = new ExpressionParser();

    @Test
    void shouldRestrictReferencedKeyUpdate() throws Exception {
        TableManager manager = createSimpleSchema(
                ReferentialAction.RESTRICT,
                false
        );

        insertUser(manager, 1, "Emre");
        insertOrder(manager, 100, 1);

        assertThrows(
                ForeignKeyUpdateRestrictedException.class,
                () -> updateUserId(manager, 1, 2)
        );

        assertEquals(1, readValue("users", 0, 0));
        assertEquals(1, readValue("orders", 0, 1));
    }

    @Test
    void shouldCascadeReferencedKeyUpdateToChildren() throws Exception {
        TableManager manager = createSimpleSchema(
                ReferentialAction.CASCADE,
                false
        );

        insertUser(manager, 1, "Emre");
        insertOrder(manager, 100, 1);
        insertOrder(manager, 101, 1);

        assertEquals(1, updateUserId(manager, 1, 2));

        assertEquals(2, readValue("users", 0, 0));
        assertEquals(2, readValue("orders", 0, 1));
        assertEquals(2, readValue("orders", 1, 1));
    }

    @Test
    void shouldSetNullOnReferencedKeyUpdate() throws Exception {
        TableManager manager = createSimpleSchema(
                ReferentialAction.SET_NULL,
                false
        );

        insertUser(manager, 1, "Emre");
        insertOrder(manager, 100, 1);

        assertEquals(1, updateUserId(manager, 1, 2));

        assertEquals(2, readValue("users", 0, 0));
        assertNull(readValue("orders", 0, 1));
    }

    @Test
    void shouldRejectSetNullWhenChildColumnIsNotNull() throws Exception {
        TableManager manager = createSimpleSchema(
                ReferentialAction.SET_NULL,
                true
        );

        insertUser(manager, 1, "Emre");
        insertOrder(manager, 100, 1);

        assertThrows(
                NotNullConstraintViolationException.class,
                () -> updateUserId(manager, 1, 2)
        );

        assertEquals(1, readValue("users", 0, 0));
        assertEquals(1, readValue("orders", 0, 1));
    }

    @Test
    void shouldCascadeCompositeReferencedKeyUpdate() throws Exception {
        TableManager manager = new TableManager(tempDirectory);

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
                                        List.of("country_code", "city_code")
                                )
                        )
                )
        );

        manager.createTable(
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
                                        List.of("country_code", "city_code"),
                                        "cities",
                                        List.of("country_code", "city_code"),
                                        ReferentialAction.RESTRICT,
                                        ReferentialAction.CASCADE
                                )
                        )
                )
        );

        insert(
                manager,
                "cities",
                new InsertCommand(
                        "cities",
                        List.of("id", "country_code", "city_code"),
                        List.of(1, "TR", "MLT")
                ),
                false
        );

        insert(
                manager,
                "addresses",
                new InsertCommand(
                        "addresses",
                        List.of("id", "country_code", "city_code"),
                        List.of(10, "TR", "MLT")
                ),
                true
        );

        int updated = update(
                manager,
                "cities",
                new UpdateCommand(
                        "cities",
                        Map.of("city_code", "IST"),
                        expressionParser.parse("id = 1")
                )
        );

        assertEquals(1, updated);
        assertEquals("IST", readValue("cities", 0, 2));
        assertEquals("TR", readValue("addresses", 0, 1));
        assertEquals("IST", readValue("addresses", 0, 2));
    }

    @Test
    void shouldCascadeReferencedKeyUpdateRecursively() throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        ),
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
                                new UniqueConstraint("user_id"),
                                new ForeignKeyConstraint(
                                        "user_id",
                                        "users",
                                        "id",
                                        ReferentialAction.RESTRICT,
                                        ReferentialAction.CASCADE
                                )
                        )
                )
        );

        manager.createTable(
                new Table(
                        "audit_log",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("order_user_id", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        "order_user_id",
                                        "orders",
                                        "user_id",
                                        ReferentialAction.RESTRICT,
                                        ReferentialAction.CASCADE
                                )
                        )
                )
        );

        insertUser(manager, 1, "Emre");
        insertOrder(manager, 100, 1);
        insert(
                manager,
                "audit_log",
                new InsertCommand(
                        "audit_log",
                        List.of("id", "order_user_id"),
                        List.of(500, 1)
                ),
                true
        );

        assertEquals(1, updateUserId(manager, 1, 2));
        assertEquals(2, readValue("orders", 0, 1));
        assertEquals(2, readValue("audit_log", 0, 1));
    }

    private TableManager createSimpleSchema(
            ReferentialAction onUpdate,
            boolean childNotNull
    ) {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        ),
                        List.of(new PrimaryKeyConstraint("id"))
                )
        );

        java.util.ArrayList<com.yekdb.constraint.Constraint> constraints =
                new java.util.ArrayList<>();
        constraints.add(new PrimaryKeyConstraint("id"));
        if (childNotNull) {
            constraints.add(new NotNullConstraint("user_id"));
        }
        constraints.add(
                new ForeignKeyConstraint(
                        "user_id",
                        "users",
                        "id",
                        ReferentialAction.RESTRICT,
                        onUpdate
                )
        );

        manager.createTable(
                new Table(
                        "orders",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("user_id", DataType.INT)
                        ),
                        constraints
                )
        );

        return manager;
    }

    private void insertUser(
            TableManager manager,
            int id,
            String name
    ) throws Exception {
        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(id, name)
                ),
                false
        );
    }

    private void insertOrder(
            TableManager manager,
            int id,
            Integer userId
    ) throws Exception {
        insert(
                manager,
                "orders",
                new InsertCommand(
                        "orders",
                        List.of("id", "user_id"),
                        List.of(id, userId)
                ),
                true
        );
    }

    private int updateUserId(
            TableManager manager,
            int oldId,
            int newId
    ) throws Exception {
        return update(
                manager,
                "users",
                new UpdateCommand(
                        "users",
                        Map.of("id", newId),
                        expressionParser.parse("id = " + oldId)
                )
        );
    }

    private int update(
            TableManager manager,
            String tableName,
            UpdateCommand command
    ) throws Exception {
        Table table = manager.getTable(tableName);
        StorageEngine engine = openStorage(tableName);

        try {
            RecordManager recordManager = new RecordManager(
                    engine.getPageManager(),
                    PageType.DATA
            );
            return updateExecutor.execute(
                    table,
                    command,
                    recordManager,
                    manager
            );
        } finally {
            engine.shutdown();
        }
    }

    private void insert(
            TableManager manager,
            String tableName,
            InsertCommand command,
            boolean foreignKeyAware
    ) throws Exception {
        Table table = manager.getTable(tableName);
        StorageEngine engine = openStorage(tableName);

        try {
            RecordManager recordManager = new RecordManager(
                    engine.getPageManager(),
                    PageType.DATA
            );

            if (foreignKeyAware) {
                insertExecutor.execute(
                        table,
                        command,
                        recordManager,
                        manager
                );
            } else {
                insertExecutor.execute(
                        table,
                        command,
                        recordManager
                );
            }
        } finally {
            engine.shutdown();
        }
    }

    private Object readValue(
            String tableName,
            long recordId,
            int columnIndex
    ) throws Exception {
        StorageEngine engine = openStorage(tableName);

        try {
            RecordManager recordManager = new RecordManager(
                    engine.getPageManager(),
                    PageType.DATA
            );
            Row row = recordManager.getRow(recordId);
            return row.getValue(columnIndex);
        } finally {
            engine.shutdown();
        }
    }

    private StorageEngine openStorage(String tableName) throws Exception {
        StorageEngine engine = new StorageEngine(
                tempDirectory.resolve(tableName.toLowerCase() + ".data")
        );
        engine.initialize();
        return engine;
    }
}
