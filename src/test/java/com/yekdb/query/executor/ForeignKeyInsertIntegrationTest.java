package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.constraint.exception.ForeignKeyConstraintViolationException;
import com.yekdb.query.command.InsertCommand;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeignKeyInsertIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor =
            new InsertExecutor();

    @Test
    void shouldAllowInsertWhenReferencedPrimaryKeyExists()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                manager.getTable("users"),
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        assertDoesNotThrow(() ->
                insert(
                        manager,
                        manager.getTable("orders"),
                        new InsertCommand(
                                "orders",
                                List.of("id", "user_id"),
                                List.of(100, 1)
                        ),
                        true
                )
        );

        assertEquals(
                1,
                activeRecordCount("orders")
        );
    }

    @Test
    void shouldRejectInsertWhenReferencedRowDoesNotExist()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        ForeignKeyConstraintViolationException exception =
                assertThrows(
                        ForeignKeyConstraintViolationException.class,
                        () -> insert(
                                manager,
                                manager.getTable("orders"),
                                new InsertCommand(
                                        "orders",
                                        List.of("id", "user_id"),
                                        List.of(100, 999)
                                ),
                                true
                        )
                );

        assertEquals(
                List.of("user_id"),
                exception.getColumns()
        );

        assertEquals(
                "users",
                exception.getReferencedTableName()
        );

        assertEquals(
                List.of("id"),
                exception.getReferencedColumns()
        );

        assertEquals(
                List.of(999),
                exception.getValues()
        );

        assertEquals(
                0,
                activeRecordCount("orders")
        );
    }

    @Test
    void shouldAllowNullForeignKeyValue()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        List<Object> values = new ArrayList<>();
        values.add(100);
        values.add(null);

        assertDoesNotThrow(() ->
                insert(
                        manager,
                        manager.getTable("orders"),
                        new InsertCommand(
                                "orders",
                                List.of("id", "user_id"),
                                values
                        ),
                        true
                )
        );

        assertEquals(
                1,
                activeRecordCount("orders")
        );
    }

    @Test
    void shouldAllowMultipleChildrenReferencingSameParent()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(
                manager,
                manager.getTable("users"),
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                ),
                false
        );

        insert(
                manager,
                manager.getTable("orders"),
                new InsertCommand(
                        "orders",
                        List.of("id", "user_id"),
                        List.of(100, 1)
                ),
                true
        );

        insert(
                manager,
                manager.getTable("orders"),
                new InsertCommand(
                        "orders",
                        List.of("id", "user_id"),
                        List.of(101, 1)
                ),
                true
        );

        assertEquals(
                2,
                activeRecordCount("orders")
        );
    }

    @Test
    void shouldAllowCompositeForeignKeyWhenReferencedTupleExists()
            throws Exception {

        TableManager manager = createCompositeSchema();

        insert(
                manager,
                manager.getTable("cities"),
                new InsertCommand(
                        "cities",
                        List.of("id", "country_code", "city_code"),
                        List.of(1, "TR", "MLT")
                ),
                false
        );

        assertDoesNotThrow(() ->
                insert(
                        manager,
                        manager.getTable("addresses"),
                        new InsertCommand(
                                "addresses",
                                List.of("id", "country_code", "city_code"),
                                List.of(10, "TR", "MLT")
                        ),
                        true
                )
        );
    }

    @Test
    void shouldRejectCompositeForeignKeyWhenReferencedTupleDoesNotExist()
            throws Exception {

        TableManager manager = createCompositeSchema();

        insert(
                manager,
                manager.getTable("cities"),
                new InsertCommand(
                        "cities",
                        List.of("id", "country_code", "city_code"),
                        List.of(1, "TR", "MLT")
                ),
                false
        );

        assertThrows(
                ForeignKeyConstraintViolationException.class,
                () -> insert(
                        manager,
                        manager.getTable("addresses"),
                        new InsertCommand(
                                "addresses",
                                List.of("id", "country_code", "city_code"),
                                List.of(10, "TR", "IST")
                        ),
                        true
                )
        );

        assertEquals(
                0,
                activeRecordCount("addresses")
        );
    }

    @Test
    void shouldRequireTableManagerForDirectForeignKeyInsert()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();
        Table orders = manager.getTable("orders");

        Path dataFile = tempDirectory.resolve("orders.data");
        StorageEngine engine = new StorageEngine(dataFile);
        engine.initialize();

        try {
            RecordManager recordManager =
                    new RecordManager(
                            engine.getPageManager(),
                            PageType.DATA
                    );

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> insertExecutor.execute(
                                    orders,
                                    new InsertCommand(
                                            "orders",
                                            List.of("id", "user_id"),
                                            List.of(100, 1)
                                    ),
                                    recordManager
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("TableManager")
            );

        } finally {
            engine.shutdown();
        }
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

    private void insert(
            TableManager manager,
            Table table,
            InsertCommand command,
            boolean foreignKeyAware
    ) throws Exception {

        Path dataFile =
                tempDirectory.resolve(
                        table.getTableName().toLowerCase()
                                + ".data"
                );

        StorageEngine engine =
                new StorageEngine(dataFile);

        engine.initialize();

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

    private int activeRecordCount(
            String tableName
    ) throws Exception {

        Path dataFile =
                tempDirectory.resolve(
                        tableName.toLowerCase()
                                + ".data"
                );

        if (!java.nio.file.Files.isRegularFile(dataFile)) {
            return 0;
        }

        StorageEngine engine =
                new StorageEngine(dataFile);

        engine.initialize();

        try {
            RecordManager recordManager =
                    new RecordManager(
                            engine.getPageManager(),
                            PageType.DATA
                    );

            return recordManager
                    .getActiveRecords()
                    .size();

        } finally {
            engine.shutdown();
        }
    }
}
