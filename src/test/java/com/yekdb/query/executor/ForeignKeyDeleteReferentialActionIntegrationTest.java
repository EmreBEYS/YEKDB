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
 * Sprint 00-27 referential action integration tests.
 *
 * Phase 5 executes RESTRICT, CASCADE and SET NULL.
 */
class ForeignKeyDeleteReferentialActionIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor = new InsertExecutor();
    private final DeleteExecutor deleteExecutor = new DeleteExecutor();
    private final ExpressionParser expressionParser = new ExpressionParser();

    @Test
    void shouldEnforceExplicitOnDeleteRestrict() throws Exception {
        TableManager manager = createSchema(ReferentialAction.RESTRICT);
        insertFixture(manager);

        assertThrows(
                ForeignKeyDeleteRestrictedException.class,
                () -> deleteParent(manager)
        );

        assertEquals(1, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("orders"));
    }

    @Test
    void shouldCascadeDeleteReferencingRows()
            throws Exception {
        TableManager manager = createSchema(ReferentialAction.CASCADE);
        insertFixture(manager);

        int deletedRootRows = deleteParent(manager);

        assertEquals(1, deletedRootRows);
        assertEquals(0, activeRecordCount("users"));
        assertEquals(0, activeRecordCount("orders"));
    }

    @Test
    void shouldSetForeignKeyToNullOnDelete()
            throws Exception {
        TableManager manager = createSchema(ReferentialAction.SET_NULL);
        insertFixture(manager);

        assertEquals(1, deleteParent(manager));

        assertEquals(0, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("orders"));
        assertEquals(null, activeRows("orders").getFirst().getValue(1));
    }

    private TableManager createSchema(ReferentialAction onDelete) {
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
                                new ForeignKeyConstraint(
                                        "user_id",
                                        "users",
                                        "id",
                                        onDelete,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        return manager;
    }

    private void insertFixture(TableManager manager) throws Exception {
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
                "orders",
                new InsertCommand(
                        "orders",
                        List.of("id", "user_id"),
                        List.of(100, 1)
                ),
                true
        );
    }

    private int deleteParent(TableManager manager) throws Exception {
        Table table = manager.getTable("users");
        Path dataFile = dataFile("users");
        StorageEngine storageEngine = new StorageEngine(dataFile);

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            return deleteExecutor.execute(
                    table,
                    new DeleteCommand(
                            "users",
                            expressionParser.parse("id = 1")
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

    private void insert(
            TableManager manager,
            String tableName,
            InsertCommand command,
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

            if (validateForeignKey) {
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


    private List<com.yekdb.storage.record.Row> activeRows(String tableName) throws Exception {
        StorageEngine storageEngine = new StorageEngine(dataFile(tableName));

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            java.util.ArrayList<com.yekdb.storage.record.Row> rows = new java.util.ArrayList<>();
            for (com.yekdb.storage.record.Record record : recordManager.getActiveRecords()) {
                rows.add(recordManager.getRow(record.getRecordId()));
            }
            return rows;
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
