package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForeignKeyDeleteRestrictIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor = new InsertExecutor();
    private final DeleteExecutor deleteExecutor = new DeleteExecutor();
    private final ExpressionParser expressionParser = new ExpressionParser();

    @Test
    void shouldRejectDeleteWhenParentRowIsReferenced()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(1, "Emre")
        ), false);

        insert(manager, "orders", new InsertCommand(
                "orders",
                List.of("id", "user_id"),
                List.of(100, 1)
        ), true);

        ForeignKeyDeleteRestrictedException exception = assertThrows(
                ForeignKeyDeleteRestrictedException.class,
                () -> delete(
                        manager,
                        "users",
                        new DeleteCommand(
                                "users",
                                expressionParser.parse("id = 1")
                        )
                )
        );

        assertEquals("users", exception.getParentTableName());
        assertEquals(List.of("id"), exception.getParentColumns());
        assertEquals("orders", exception.getReferencingTableName());
        assertEquals(List.of("user_id"), exception.getReferencingColumns());
        assertEquals(List.of(1), exception.getValues());
        assertEquals(1, activeRecordCount("users"));
    }

    @Test
    void shouldAllowDeleteWhenParentRowIsNotReferenced()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(1, "Emre")
        ), false);

        int deleted = assertDoesNotThrow(() ->
                delete(
                        manager,
                        "users",
                        new DeleteCommand(
                                "users",
                                expressionParser.parse("id = 1")
                        )
                )
        );

        assertEquals(1, deleted);
        assertEquals(0, activeRecordCount("users"));
    }

    @Test
    void shouldAllowDeleteWhenChildForeignKeyIsNull()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(1, "Emre")
        ), false);

        List<Object> values = new ArrayList<>();
        values.add(100);
        values.add(null);

        insert(manager, "orders", new InsertCommand(
                "orders",
                List.of("id", "user_id"),
                values
        ), true);

        int deleted = delete(
                manager,
                "users",
                new DeleteCommand(
                        "users",
                        expressionParser.parse("id = 1")
                )
        );

        assertEquals(1, deleted);
        assertEquals(0, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("orders"));
    }

    @Test
    void shouldAllowDeleteAfterReferencingChildWasDeleted()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(1, "Emre")
        ), false);

        insert(manager, "orders", new InsertCommand(
                "orders",
                List.of("id", "user_id"),
                List.of(100, 1)
        ), true);

        delete(
                manager,
                "orders",
                new DeleteCommand(
                        "orders",
                        expressionParser.parse("id = 100")
                )
        );

        int deleted = delete(
                manager,
                "users",
                new DeleteCommand(
                        "users",
                        expressionParser.parse("id = 1")
                )
        );

        assertEquals(1, deleted);
        assertEquals(0, activeRecordCount("users"));
    }

    @Test
    void shouldRejectCompositeParentDeleteWhenTupleIsReferenced()
            throws Exception {

        TableManager manager = createCompositeSchema();

        insert(manager, "cities", new InsertCommand(
                "cities",
                List.of("id", "country_code", "city_code"),
                List.of(1, "TR", "MLT")
        ), false);

        insert(manager, "addresses", new InsertCommand(
                "addresses",
                List.of("id", "country_code", "city_code"),
                List.of(10, "TR", "MLT")
        ), true);

        assertThrows(
                ForeignKeyDeleteRestrictedException.class,
                () -> delete(
                        manager,
                        "cities",
                        new DeleteCommand(
                                "cities",
                                expressionParser.parse("id = 1")
                        )
                )
        );

        assertEquals(1, activeRecordCount("cities"));
    }

    @Test
    void shouldRejectWholeDeleteBeforeAnyRowIsRemoved()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(1, "Referenced")
        ), false);

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(2, "Unreferenced")
        ), false);

        insert(manager, "orders", new InsertCommand(
                "orders",
                List.of("id", "user_id"),
                List.of(100, 1)
        ), true);

        assertThrows(
                ForeignKeyDeleteRestrictedException.class,
                () -> delete(
                        manager,
                        "users",
                        new DeleteCommand("users", null)
                )
        );

        // Statement atomik davranmalı: ikinci satır da silinmemiş olmalı.
        assertEquals(2, activeRecordCount("users"));
    }

    @Test
    void shouldAllowDeleteWhenReferencingTableHasNoRows()
            throws Exception {

        TableManager manager = createUsersAndOrdersSchema();

        insert(manager, "users", new InsertCommand(
                "users",
                List.of("id", "name"),
                List.of(1, "Emre")
        ), false);

        int deleted = delete(
                manager,
                "users",
                new DeleteCommand("users", null)
        );

        assertEquals(1, deleted);
        assertEquals(0, activeRecordCount("users"));
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

    private int delete(
            TableManager manager,
            String tableName,
            DeleteCommand command
    ) throws Exception {

        Table table = manager.getTable(tableName);
        StorageEngine engine = openStorage(tableName);

        try {
            RecordManager recordManager = new RecordManager(
                    engine.getPageManager(),
                    PageType.DATA
            );

            return deleteExecutor.execute(
                    table,
                    command,
                    recordManager,
                    manager
            );
        } finally {
            engine.shutdown();
        }
    }

    private StorageEngine openStorage(String tableName)
            throws Exception {
        StorageEngine engine = new StorageEngine(
                tempDirectory.resolve(tableName + ".data")
        );
        engine.initialize();
        return engine;
    }

    private int activeRecordCount(String tableName)
            throws Exception {
        StorageEngine engine = openStorage(tableName);

        try {
            RecordManager recordManager = new RecordManager(
                    engine.getPageManager(),
                    PageType.DATA
            );

            return recordManager.getActiveRecords().size();
        } finally {
            engine.shutdown();
        }
    }
}
