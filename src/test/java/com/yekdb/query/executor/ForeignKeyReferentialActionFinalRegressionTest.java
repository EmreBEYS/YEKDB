package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.constraint.exception.ForeignKeyDeleteRestrictedException;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.UpdateCommand;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sprint 00-27 Phase 7 final regression tests.
 *
 * <p>These tests intentionally combine referential actions and edge cases
 * that cross the Phase 3-6 implementation boundaries.</p>
 */
class ForeignKeyReferentialActionFinalRegressionTest {

    @TempDir
    Path tempDirectory;

    private final InsertExecutor insertExecutor = new InsertExecutor();
    private final UpdateExecutor updateExecutor = new UpdateExecutor();
    private final DeleteExecutor deleteExecutor = new DeleteExecutor();
    private final ExpressionParser expressionParser = new ExpressionParser();

    @Test
    void shouldNotTriggerOnUpdateRestrictWhenReferencedKeyIsUnchanged()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                ),
                List.of(new PrimaryKeyConstraint("id"))
        ));

        manager.createTable(new Table(
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
                                ReferentialAction.RESTRICT,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        insert(manager, "users", List.of("id", "name"), List.of(1, "Emre"), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);

        int updated = update(
                manager,
                "users",
                new UpdateCommand(
                        "users",
                        Map.of("name", "Yunus Emre"),
                        expressionParser.parse("id = 1")
                )
        );

        assertEquals(1, updated);
        assertEquals("Yunus Emre", activeRows("users").getFirst().getValue(1));
        assertEquals(1, activeRows("orders").getFirst().getValue(1));
    }

    @Test
    void shouldAllowRestrictDeleteWhenNoChildReferencesTargetRow()
            throws Exception {
        TableManager manager = createUsersOrdersSchema(
                ReferentialAction.RESTRICT,
                ReferentialAction.RESTRICT
        );

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "users", List.of("id"), List.of(2), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 2), true);

        assertEquals(1, deleteWhere(manager, "users", "id = 1"));
        assertEquals(1, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("orders"));
        assertEquals(2, activeRows("orders").getFirst().getValue(1));
    }

    @Test
    void shouldApplyMixedDeleteCascadeAndSetNullActions()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "users",
                List.of(new Column("id", DataType.INT)),
                List.of(new PrimaryKeyConstraint("id"))
        ));

        manager.createTable(new Table(
                "orders",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.CASCADE,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        manager.createTable(new Table(
                "profiles",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.SET_NULL,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "profiles", List.of("id", "user_id"), List.of(20, 1), true);

        assertEquals(1, deleteWhere(manager, "users", "id = 1"));
        assertEquals(0, activeRecordCount("users"));
        assertEquals(0, activeRecordCount("orders"));
        assertEquals(1, activeRecordCount("profiles"));
        assertNull(activeRows("profiles").getFirst().getValue(1));
    }

    @Test
    void shouldAbortMixedDeleteActionsBeforeAnyMutationWhenRestrictConflicts()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "users",
                List.of(new Column("id", DataType.INT)),
                List.of(new PrimaryKeyConstraint("id"))
        ));

        manager.createTable(new Table(
                "cascade_child",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.CASCADE,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        manager.createTable(new Table(
                "nullable_child",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.SET_NULL,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        manager.createTable(new Table(
                "restrict_child",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.RESTRICT,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "cascade_child", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "nullable_child", List.of("id", "user_id"), List.of(20, 1), true);
        insert(manager, "restrict_child", List.of("id", "user_id"), List.of(30, 1), true);

        assertThrows(
                ForeignKeyDeleteRestrictedException.class,
                () -> deleteWhere(manager, "users", "id = 1")
        );

        assertEquals(1, activeRecordCount("users"));
        assertEquals(1, activeRecordCount("cascade_child"));
        assertEquals(1, activeRecordCount("nullable_child"));
        assertEquals(1, activeRecordCount("restrict_child"));
        assertEquals(1, activeRows("nullable_child").getFirst().getValue(1));
    }

    @Test
    void shouldApplyMixedUpdateCascadeAndSetNullActions()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "users",
                List.of(new Column("id", DataType.INT)),
                List.of(new PrimaryKeyConstraint("id"))
        ));

        manager.createTable(new Table(
                "orders",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.RESTRICT,
                                ReferentialAction.CASCADE
                        )
                )
        ));

        manager.createTable(new Table(
                "profiles",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id",
                                ReferentialAction.RESTRICT,
                                ReferentialAction.SET_NULL
                        )
                )
        ));

        insert(manager, "users", List.of("id"), List.of(1), false);
        insert(manager, "orders", List.of("id", "user_id"), List.of(10, 1), true);
        insert(manager, "profiles", List.of("id", "user_id"), List.of(20, 1), true);

        assertEquals(
                1,
                update(
                        manager,
                        "users",
                        new UpdateCommand(
                                "users",
                                Map.of("id", 2),
                                expressionParser.parse("id = 1")
                        )
                )
        );

        assertEquals(2, activeRows("users").getFirst().getValue(0));
        assertEquals(2, activeRows("orders").getFirst().getValue(1));
        assertNull(activeRows("profiles").getFirst().getValue(1));
    }

    @Test
    void shouldCascadeDeleteThroughSelfReferencingForeignKey()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "categories",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("parent_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "parent_id", "categories", "id",
                                ReferentialAction.CASCADE,
                                ReferentialAction.CASCADE
                        )
                )
        ));

        insert(manager, "categories", List.of("id", "parent_id"), java.util.Arrays.asList(1, null), true);
        insert(manager, "categories", List.of("id", "parent_id"), List.of(2, 1), true);
        insert(manager, "categories", List.of("id", "parent_id"), List.of(3, 2), true);

        assertEquals(1, deleteWhere(manager, "categories", "id = 1"));
        assertEquals(0, activeRecordCount("categories"));
    }

    @Test
    void shouldCascadeUpdateThroughSelfReferencingForeignKey()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "categories",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("parent_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "parent_id", "categories", "id",
                                ReferentialAction.CASCADE,
                                ReferentialAction.CASCADE
                        )
                )
        ));

        insert(manager, "categories", List.of("id", "parent_id"), java.util.Arrays.asList(1, null), true);
        insert(manager, "categories", List.of("id", "parent_id"), List.of(2, 1), true);

        assertEquals(
                1,
                update(
                        manager,
                        "categories",
                        new UpdateCommand(
                                "categories",
                                Map.of("id", 5),
                                expressionParser.parse("id = 1")
                        )
                )
        );

        List<Row> rows = activeRows("categories");
        assertEquals(5, rows.get(0).getValue(0));
        assertNull(rows.get(0).getValue(1));
        assertEquals(2, rows.get(1).getValue(0));
        assertEquals(5, rows.get(1).getValue(1));
    }

    @Test
    void shouldCascadeDeleteCompositeForeignKey()
            throws Exception {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "regions",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("country", DataType.STRING),
                        new Column("code", DataType.STRING)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new UniqueConstraint(List.of("country", "code"))
                )
        ));

        manager.createTable(new Table(
                "addresses",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("country", DataType.STRING),
                        new Column("region_code", DataType.STRING)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                List.of("country", "region_code"),
                                "regions",
                                List.of("country", "code"),
                                ReferentialAction.CASCADE,
                                ReferentialAction.RESTRICT
                        )
                )
        ));

        insert(
                manager,
                "regions",
                List.of("id", "country", "code"),
                List.of(1, "TR", "44"),
                false
        );
        insert(
                manager,
                "addresses",
                List.of("id", "country", "region_code"),
                List.of(10, "TR", "44"),
                true
        );

        assertEquals(1, deleteWhere(manager, "regions", "id = 1"));
        assertEquals(0, activeRecordCount("regions"));
        assertEquals(0, activeRecordCount("addresses"));
    }

    private TableManager createUsersOrdersSchema(
            ReferentialAction onDelete,
            ReferentialAction onUpdate
    ) {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(new Table(
                "users",
                List.of(new Column("id", DataType.INT)),
                List.of(new PrimaryKeyConstraint("id"))
        ));

        manager.createTable(new Table(
                "orders",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("user_id", DataType.INT)
                ),
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new ForeignKeyConstraint(
                                "user_id", "users", "id", onDelete, onUpdate
                        )
                )
        ));

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
        StorageEngine storageEngine = openStorage(tableName);

        try {
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
            storageEngine.shutdown();
        }
    }

    private int update(
            TableManager manager,
            String tableName,
            UpdateCommand command
    ) throws Exception {
        Table table = manager.getTable(tableName);
        StorageEngine storageEngine = openStorage(tableName);

        try {
            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );
            return updateExecutor.execute(table, command, recordManager, manager);
        } finally {
            storageEngine.shutdown();
        }
    }

    private int deleteWhere(
            TableManager manager,
            String tableName,
            String whereExpression
    ) throws Exception {
        Table table = manager.getTable(tableName);
        StorageEngine storageEngine = openStorage(tableName);

        try {
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
            storageEngine.shutdown();
        }
    }

    private int activeRecordCount(String tableName) throws Exception {
        return activeRows(tableName).size();
    }

    private List<Row> activeRows(String tableName) throws Exception {
        StorageEngine storageEngine = openStorage(tableName);

        try {
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
            storageEngine.shutdown();
        }
    }

    private StorageEngine openStorage(String tableName) throws Exception {
        StorageEngine storageEngine = new StorageEngine(
                tempDirectory.resolve(tableName.toLowerCase() + ".data")
        );
        storageEngine.initialize();
        return storageEngine;
    }
}
