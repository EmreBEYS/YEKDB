package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.constraint.exception.ForeignKeyConstraintViolationException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeignKeyUpdateIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor =
            new InsertExecutor();

    private final UpdateExecutor updateExecutor =
            new UpdateExecutor();

    private final ExpressionParser expressionParser =
            new ExpressionParser();

    @Test
    void shouldAllowUpdateWhenReferencedPrimaryKeyExists()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(2, "Ali")
                ),
                false
        );

        insertOrder(manager, 100, 1);

        int updated = update(
                manager,
                "orders",
                new UpdateCommand(
                        "orders",
                        Map.of("user_id", 2),
                        expressionParser.parse("id = 100")
                ),
                true
        );

        assertEquals(1, updated);
        assertEquals(2, readValue("orders", 0, 1));
    }

    @Test
    void shouldRejectUpdateWhenReferencedRowDoesNotExist()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        insertOrder(manager, 100, 1);

        ForeignKeyConstraintViolationException exception =
                assertThrows(
                        ForeignKeyConstraintViolationException.class,
                        () -> update(
                                manager,
                                "orders",
                                new UpdateCommand(
                                        "orders",
                                        Map.of("user_id", 999),
                                        expressionParser.parse("id = 100")
                                ),
                                true
                        )
                );

        assertEquals(List.of("user_id"), exception.getColumns());
        assertEquals("users", exception.getReferencedTableName());
        assertEquals(List.of("id"), exception.getReferencedColumns());
        assertEquals(List.of(999), exception.getValues());

        // Validation fiziksel update'den önce çalışmalıdır.
        assertEquals(1, readValue("orders", 0, 1));
    }

    @Test
    void shouldAllowNullForeignKeyValueOnUpdate()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        insertOrder(manager, 100, 1);

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("user_id", null);

        assertDoesNotThrow(() ->
                update(
                        manager,
                        "orders",
                        new UpdateCommand(
                                "orders",
                                values,
                                expressionParser.parse("id = 100")
                        ),
                        true
                )
        );

        assertNull(readValue("orders", 0, 1));
    }

    @Test
    void shouldNotRequireTableManagerWhenUpdatingNonForeignKeyColumn()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        insertOrder(manager, 100, 1);

        assertDoesNotThrow(() ->
                update(
                        manager,
                        "orders",
                        new UpdateCommand(
                                "orders",
                                Map.of("id", 101),
                                expressionParser.parse("id = 100")
                        ),
                        false
                )
        );

        assertEquals(101, readValue("orders", 0, 0));
        assertEquals(1, readValue("orders", 0, 1));
    }

    @Test
    void shouldRequireTableManagerForDirectForeignKeyUpdate()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                "users",
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        insertOrder(manager, 100, 1);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> update(
                                manager,
                                "orders",
                                new UpdateCommand(
                                        "orders",
                                        Map.of("user_id", 1),
                                        expressionParser.parse("id = 100")
                                ),
                                false
                        )
                );

        assertTrue(exception.getMessage().contains("TableManager"));
    }

    @Test
    void shouldAllowCompositeForeignKeyUpdateWhenTupleExists()
            throws Exception {

        TableManager manager = createCompositeSchema();

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
                "cities",
                new InsertCommand(
                        "cities",
                        List.of("id", "country_code", "city_code"),
                        List.of(2, "TR", "IST")
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
                "addresses",
                new UpdateCommand(
                        "addresses",
                        Map.of("city_code", "IST"),
                        expressionParser.parse("id = 10")
                ),
                true
        );

        assertEquals(1, updated);
        assertEquals("IST", readValue("addresses", 0, 2));
    }

    @Test
    void shouldRejectCompositeForeignKeyUpdateWhenTupleDoesNotExist()
            throws Exception {

        TableManager manager = createCompositeSchema();

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

        assertThrows(
                ForeignKeyConstraintViolationException.class,
                () -> update(
                        manager,
                        "addresses",
                        new UpdateCommand(
                                "addresses",
                                Map.of("city_code", "IST"),
                                expressionParser.parse("id = 10")
                        ),
                        true
                )
        );

        assertEquals("MLT", readValue("addresses", 0, 2));
    }

    private TableManager createUsersAndOrdersSchema() {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id")
                        )
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
                                        "id"
                                )
                        )
                )
        );

        return manager;
    }

    private TableManager createCompositeSchema() {
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
                                        List.of(
                                                "country_code",
                                                "city_code"
                                        )
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
                )
        );

        return manager;
    }

    private void insertOrder(
            TableManager manager,
            int orderId,
            Integer userId
    ) throws Exception {

        insert(
                manager,
                "orders",
                new InsertCommand(
                        "orders",
                        List.of("id", "user_id"),
                        List.of(orderId, userId)
                ),
                true
        );
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
            RecordManager recordManager =
                    new RecordManager(
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

    private int update(
            TableManager manager,
            String tableName,
            UpdateCommand command,
            boolean foreignKeyAware
    ) throws Exception {

        Table table = manager.getTable(tableName);
        StorageEngine engine = openStorage(tableName);

        try {
            RecordManager recordManager =
                    new RecordManager(
                            engine.getPageManager(),
                            PageType.DATA
                    );

            if (foreignKeyAware) {
                return updateExecutor.execute(
                        table,
                        command,
                        recordManager,
                        manager
                );
            }

            return updateExecutor.execute(
                    table,
                    command,
                    recordManager
            );
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
            RecordManager recordManager =
                    new RecordManager(
                            engine.getPageManager(),
                            PageType.DATA
                    );

            Row row = recordManager.getRow(recordId);
            return row.getValue(columnIndex);
        } finally {
            engine.shutdown();
        }
    }

    private StorageEngine openStorage(
            String tableName
    ) throws Exception {

        StorageEngine engine =
                new StorageEngine(
                        tempDirectory.resolve(
                                tableName.toLowerCase() + ".data"
                        )
                );

        engine.initialize();
        return engine;
    }
}
