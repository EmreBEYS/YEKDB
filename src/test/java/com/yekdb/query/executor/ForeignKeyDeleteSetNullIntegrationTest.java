package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.exception.NotNullConstraintViolationException;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sprint 00-27 Phase 5 integration tests for ON DELETE SET NULL.
 */
class ForeignKeyDeleteSetNullIntegrationTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor = new InsertExecutor();
    private final DeleteExecutor deleteExecutor = new DeleteExecutor();
    private final ExpressionParser expressionParser = new ExpressionParser();

    @Test
    void shouldSetMultipleReferencingRowsToNull() throws Exception {
        TableManager manager = createUsersOrdersSchema(false);

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "orders", List.of("id", "user_id"), List.of(11, 1), true);

        assertEquals(1, deleteById(manager, "users", 1));

        assertEquals(0, activeRecordCount("users"));
        List<Row> orders = activeRows("orders");
        assertEquals(2, orders.size());
        assertEquals(null, orders.get(0).getValue(1));
        assertEquals(null, orders.get(1).getValue(1));
    }

    @Test
    void shouldSetAllCompositeForeignKeyColumnsToNull() throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "regions",
                        List.of(
                                new Column("country_id", DataType.INT),
                                new Column("region_id", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint(
                                        List.of("country_id", "region_id")
                                )
                        )
                )
        );

        manager.createTable(
                new Table(
                        "branches",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("country_id", DataType.INT),
                                new Column("region_id", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        List.of("country_id", "region_id"),
                                        "regions",
                                        List.of("country_id", "region_id"),
                                        ReferentialAction.SET_NULL,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        insert(
                manager,
                "regions",
                List.of("country_id", "region_id"),
                List.of(90, 44),
                false
        );
        insert(
                manager,
                "branches",
                List.of("id", "country_id", "region_id"),
                List.of(1, 90, 44),
                true
        );

        assertEquals(
                1,
                deleteWhere(manager, "regions", "country_id = 90 AND region_id = 44")
        );

        Row branch = activeRows("branches").getFirst();
        assertEquals(1, branch.getValue(0));
        assertEquals(null, branch.getValue(1));
        assertEquals(null, branch.getValue(2));
    }

    @Test
    void shouldRejectSetNullWhenForeignKeyColumnIsNotNull() throws Exception {
        TableManager manager = createUsersOrdersSchema(true);

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);

        assertThrows(
                NotNullConstraintViolationException.class,
                () -> deleteById(manager, "users", 1)
        );

        // SET NULL validation happens before any mutation.
        assertEquals(1, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("orders"));
        assertEquals(1, activeRows("orders").getFirst().getValue(1));
    }

    @Test
    void shouldApplySetNullToRowsReferencingCascadeDeletedChildren()
            throws Exception {
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
                                        ReferentialAction.CASCADE,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        manager.createTable(
                new Table(
                        "audit_log",
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
                                        ReferentialAction.SET_NULL,
                                        ReferentialAction.RESTRICT
                                )
                        )
                )
        );

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "audit_log", List.of("id", "order_id"), List.of(100, 10), true);

        assertEquals(1, deleteById(manager, "users", 1));

        assertEquals(0, activeRecordCount("users"));
        assertEquals(0, activeRecordCount("orders"));
        assertEquals(1, activeRecordCount("audit_log"));
        assertEquals(null, activeRows("audit_log").getFirst().getValue(1));
    }

    @Test
    void shouldSkipSetNullForChildAlreadyScheduledForCascadeDelete()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "parents",
                        List.of(new Column("id", DataType.INT)),
                        List.of(new PrimaryKeyConstraint("id"))
                )
        );

        manager.createTable(
                new Table(
                        "children",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("cascade_parent", DataType.INT),
                                new Column("nullable_parent", DataType.INT)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new ForeignKeyConstraint(
                                        "cascade_parent",
                                        "parents",
                                        "id",
                                        ReferentialAction.CASCADE,
                                        ReferentialAction.RESTRICT
                                ),
                                new ForeignKeyConstraint(
                                        "nullable_parent",
                                        "parents",
                                        "id",
                                        ReferentialAction.SET_NULL,
                                        ReferentialAction.RESTRICT
                                ),
                                // Would fail if SET NULL were attempted before recognizing
                                // that this row is already in the cascade delete set.
                                new NotNullConstraint("nullable_parent")
                        )
                )
        );

        insert(manager, "parents", List.of("id"), List.of(1), false);
        insert(
                manager,
                "children",
                List.of("id", "cascade_parent", "nullable_parent"),
                List.of(10, 1, 1),
                true
        );

        assertEquals(1, deleteById(manager, "parents", 1));
        assertEquals(0, activeRecordCount("parents"));
        assertEquals(0, activeRecordCount("children"));
    }

    private TableManager createUsersOrdersSchema(boolean userIdNotNull) {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(new Column("id", DataType.INT)),
                        List.of(new PrimaryKeyConstraint("id"))
                )
        );

        List<com.yekdb.constraint.Constraint> orderConstraints = new ArrayList<>();
        orderConstraints.add(new PrimaryKeyConstraint("id"));
        orderConstraints.add(
                new ForeignKeyConstraint(
                        "user_id",
                        "users",
                        "id",
                        ReferentialAction.SET_NULL,
                        ReferentialAction.RESTRICT
                )
        );
        if (userIdNotNull) {
            orderConstraints.add(new NotNullConstraint("user_id"));
        }

        manager.createTable(
                new Table(
                        "orders",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("user_id", DataType.INT)
                        ),
                        orderConstraints
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

            InsertCommand command = new InsertCommand(tableName, columns, values);

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
        return deleteWhere(manager, tableName, "id = " + id);
    }

    private int deleteWhere(
            TableManager manager,
            String tableName,
            String whereExpression
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
                            expressionParser.parse(whereExpression)
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
        return activeRows(tableName).size();
    }

    private List<Row> activeRows(String tableName) throws Exception {
        StorageEngine storageEngine = new StorageEngine(dataFile(tableName));

        try {
            storageEngine.initialize();
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            List<Row> rows = new ArrayList<>();
            for (Record record : recordManager.getActiveRecords()) {
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
