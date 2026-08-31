package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.index.IndexType;
import com.yekdb.index.RecordPointer;
import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 00-29 Phase 14 B+ Tree mutation maintenance testleri.
 */
class IndexMaintenanceIntegrationTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private RecordManager recordManager;

    private Table table;

    private Index<Integer> idIndex;
    private Index<Integer> ageIndex;

    private InsertExecutor insertExecutor;
    private UpdateExecutor updateExecutor;
    private DeleteExecutor deleteExecutor;

    @BeforeEach
    void setUp()
            throws IOException {

        table =
                new Table(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                ),
                                new Column(
                                        "name",
                                        DataType.STRING
                                ),
                                new Column(
                                        "age",
                                        DataType.INT
                                )
                        )
                );

        storageEngine =
                new StorageEngine(
                        tempDir.resolve(
                                "users.data"
                        )
                );

        storageEngine.initialize();

        recordManager =
                new RecordManager(
                        storageEngine.getPageManager(),
                        PageType.DATA
                );

        idIndex =
                new Index<>(
                        new IndexMetadata(
                                1L,
                                "idx_users_id",
                                "test_db",
                                "users",
                                "id",
                                IndexType.UNIQUE
                        )
                );

        ageIndex =
                new Index<>(
                        new IndexMetadata(
                                2L,
                                "idx_users_age",
                                "test_db",
                                "users",
                                "age",
                                IndexType.NON_UNIQUE
                        )
                );

        insertExecutor =
                new InsertExecutor();

        updateExecutor =
                new UpdateExecutor();

        deleteExecutor =
                new DeleteExecutor();
    }

    @AfterEach
    void tearDown()
            throws IOException {

        if (storageEngine != null
                && storageEngine.isInitialized()) {

            storageEngine.shutdown();
        }
    }

    @Test
    void shouldInsertRowIntoUniqueIndex()
            throws IOException {

        Record record =
                insert(
                        1,
                        "Emre",
                        21
                );

        RecordPointer pointer =
                pointerFor(record);

        assertEquals(
                List.of(pointer),
                idIndex.search(1)
        );
    }

    @Test
    void shouldMaintainMultipleIndexesOnInsert()
            throws IOException {

        Record record =
                insert(
                        1,
                        "Emre",
                        21
                );

        RecordPointer pointer =
                pointerFor(record);

        assertEquals(
                List.of(pointer),
                idIndex.search(1)
        );

        assertEquals(
                List.of(pointer),
                ageIndex.search(21)
        );
    }

    @Test
    void shouldAllowDuplicateKeyInNonUniqueIndex()
            throws IOException {

        Record first =
                insert(
                        1,
                        "Ali",
                        21
                );

        Record second =
                insert(
                        2,
                        "Ayse",
                        21
                );

        assertEquals(
                List.of(
                        pointerFor(first),
                        pointerFor(second)
                ),
                ageIndex.search(21)
        );
    }

    @Test
    void shouldRejectDuplicateUniqueKeyBeforePhysicalInsert()
            throws IOException {

        insert(
                1,
                "Ali",
                20
        );

        int rowCountBefore =
                recordManager.getActiveRecordCount();

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> insert(
                        1,
                        "Ayse",
                        30
                )
        );

        assertEquals(
                rowCountBefore,
                recordManager.getActiveRecordCount()
        );

        assertEquals(
                1,
                idIndex.search(1).size()
        );
    }

    @Test
    void shouldMoveUniqueIndexEntryWhenKeyChanges()
            throws IOException {

        Record record =
                insert(
                        1,
                        "Emre",
                        21
                );

        int updated =
                updateExecutor.execute(
                        table,
                        new UpdateCommand(
                                "users",
                                Map.of(
                                        "id",
                                        10
                                ),
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.EQUALS,
                                        1
                                )
                        ),
                        recordManager,
                        indexes()
                );

        assertEquals(
                1,
                updated
        );

        assertTrue(
                idIndex.search(1)
                        .isEmpty()
        );

        assertEquals(
                List.of(
                        pointerFor(record)
                ),
                idIndex.search(10)
        );
    }

    @Test
    void shouldMoveNonUniqueIndexEntryWhenValueChanges()
            throws IOException {

        Record record =
                insert(
                        1,
                        "Emre",
                        21
                );

        updateExecutor.execute(
                table,
                new UpdateCommand(
                        "users",
                        Map.of(
                                "age",
                                30
                        ),
                        new ComparisonExpression(
                                "id",
                                ComparisonOperator.EQUALS,
                                1
                        )
                ),
                recordManager,
                indexes()
        );

        assertTrue(
                ageIndex.search(21)
                        .isEmpty()
        );

        assertEquals(
                List.of(
                        pointerFor(record)
                ),
                ageIndex.search(30)
        );
    }

    @Test
    void shouldKeepIndexKeyWhenNonIndexedColumnChanges()
            throws IOException {

        Record record =
                insert(
                        1,
                        "Emre",
                        21
                );

        RecordPointer pointerBefore =
                pointerFor(record);

        updateExecutor.execute(
                table,
                new UpdateCommand(
                        "users",
                        Map.of(
                                "name",
                                "Yunus"
                        ),
                        new ComparisonExpression(
                                "id",
                                ComparisonOperator.EQUALS,
                                1
                        )
                ),
                recordManager,
                indexes()
        );

        assertEquals(
                List.of(
                        pointerFor(record)
                ),
                idIndex.search(1)
        );

        assertTrue(
                idIndex.search(1)
                        .contains(
                                pointerFor(record)
                        )
        );

        assertNotNull(
                pointerBefore
        );
    }

    @Test
    void shouldRejectUniqueConflictBeforeUpdate()
            throws IOException {

        Record first =
                insert(
                        1,
                        "Ali",
                        20
                );

        insert(
                2,
                "Ayse",
                30
        );

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> updateExecutor.execute(
                        table,
                        new UpdateCommand(
                                "users",
                                Map.of(
                                        "id",
                                        2
                                ),
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.EQUALS,
                                        1
                                )
                        ),
                        recordManager,
                        indexes()
                )
        );

        assertEquals(
                1,
                recordManager
                        .getRow(
                                first.getRecordId()
                        )
                        .getValue(0)
        );

        assertFalse(
                idIndex.search(1)
                        .isEmpty()
        );

        assertFalse(
                idIndex.search(2)
                        .isEmpty()
        );
    }

    @Test
    void shouldRemoveEntriesFromAllIndexesOnDelete()
            throws IOException {

        insert(
                1,
                "Emre",
                21
        );

        int deleted =
                deleteExecutor.execute(
                        table,
                        new DeleteCommand(
                                "users",
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.EQUALS,
                                        1
                                )
                        ),
                        recordManager,
                        indexes()
                );

        assertEquals(
                1,
                deleted
        );

        assertTrue(
                idIndex.search(1)
                        .isEmpty()
        );

        assertTrue(
                ageIndex.search(21)
                        .isEmpty()
        );
    }

    @Test
    void shouldKeepOtherPointerInNonUniqueIndexAfterDelete()
            throws IOException {

        Record first =
                insert(
                        1,
                        "Ali",
                        21
                );

        Record second =
                insert(
                        2,
                        "Ayse",
                        21
                );

        deleteExecutor.execute(
                table,
                new DeleteCommand(
                        "users",
                        new ComparisonExpression(
                                "id",
                                ComparisonOperator.EQUALS,
                                1
                        )
                ),
                recordManager,
                indexes()
        );

        assertEquals(
                List.of(
                        pointerFor(second)
                ),
                ageIndex.search(21)
        );

        assertFalse(
                ageIndex.search(21)
                        .contains(
                                pointerFor(first)
                        )
        );
    }

    @Test
    void shouldIgnoreIndexBelongingToAnotherTable()
            throws IOException {

        Index<Integer> otherTableIndex =
                new Index<>(
                        new IndexMetadata(
                                3L,
                                "idx_orders_id",
                                "test_db",
                                "orders",
                                "id",
                                IndexType.UNIQUE
                        )
                );

        Record record =
                insertExecutor.execute(
                        table,
                        command(
                                1,
                                "Emre",
                                21
                        ),
                        recordManager,
                        List.of(
                                idIndex,
                                ageIndex,
                                otherTableIndex
                        )
                );

        assertFalse(
                idIndex.search(1)
                        .isEmpty()
        );

        assertTrue(
                otherTableIndex.isEmpty()
        );

        assertNotNull(record);
    }

    @Test
    void shouldSkipNullKeyForNullableIndex()
            throws IOException {

        Index<String> nameIndex =
                new Index<>(
                        new IndexMetadata(
                                4L,
                                "idx_users_name",
                                "test_db",
                                "users",
                                "name",
                                IndexType.NON_UNIQUE
                        )
                );

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "name",
                                "age"
                        ),
                        Arrays.asList(
                                7,
                                null,
                                40
                        )
                );

        insertExecutor.execute(
                table,
                command,
                recordManager,
                List.of(
                        idIndex,
                        ageIndex,
                        nameIndex
                )
        );

        assertEquals(
                1,
                idIndex.size()
        );

        assertEquals(
                1,
                ageIndex.size()
        );

        assertTrue(
                nameIndex.isEmpty()
        );
    }

    @Test
    void shouldPreserveLegacyExecutorOverloadWithoutIndexes()
            throws IOException {

        Record record =
                insertExecutor.execute(
                        table,
                        command(
                                1,
                                "Emre",
                                21
                        ),
                        recordManager
                );

        assertNotNull(record);

        /*
         * Eski overload'a index verilmediği için
         * index maintenance uygulanmaz.
         */
        assertTrue(
                idIndex.isEmpty()
        );

        assertTrue(
                ageIndex.isEmpty()
        );
    }

    private Record insert(
            int id,
            String name,
            int age
    ) throws IOException {

        return insertExecutor.execute(
                table,
                command(
                        id,
                        name,
                        age
                ),
                recordManager,
                indexes()
        );
    }

    private InsertCommand command(
            int id,
            String name,
            int age
    ) {

        return new InsertCommand(
                "users",
                List.of(
                        "id",
                        "name",
                        "age"
                ),
                List.of(
                        id,
                        name,
                        age
                )
        );
    }

    private List<Index<?>> indexes() {

        return List.of(
                idIndex,
                ageIndex
        );
    }

    private RecordPointer pointerFor(
            Record record
    ) throws IOException {

        return RecordPointer.fromRecordId(
                recordManager.findPhysicalRecordId(
                        record.getRecordId()
                )
        );
    }
}