package com.yekdb.index;

import com.yekdb.index.exception.DuplicateIndexException;
import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.index.exception.IndexNotFoundException;
import com.yekdb.index.exception.InvalidIndexException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexManagerTest {

    private IndexManager indexManager;

    @BeforeEach
    void setUp() {
        indexManager = new IndexManager();
    }

    @Test
    void shouldCreateIndex() {
        Index<Integer> index = indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        assertNotNull(index);
        assertEquals("idx_students_id",
                index.getMetadata().getIndexName());
        assertEquals(1, indexManager.size());
        assertTrue(indexManager.indexExists("idx_students_id"));
    }

    @Test
    void shouldCreateIndexWithMetadata() {
        IndexMetadata metadata = new IndexMetadata(
                10L,
                "idx_students_email",
                "school_db",
                "students",
                "email",
                IndexType.UNIQUE
        );

        Index<String> index =
                indexManager.createIndex(metadata);

        assertEquals(metadata, index.getMetadata());
        assertTrue(indexManager.indexExists("idx_students_email"));
    }

    @Test
    void shouldRejectDuplicateIndexName() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        assertThrows(
                DuplicateIndexException.class,
                () -> indexManager.createIndex(
                        "idx_students_id",
                        "school_db",
                        "students",
                        "student_id",
                        IndexType.PRIMARY
                )
        );
    }

    @Test
    void shouldRejectDuplicateIndexFromMetadata() {
        IndexMetadata metadata = new IndexMetadata(
                1L,
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        indexManager.createIndex(metadata);

        assertThrows(
                DuplicateIndexException.class,
                () -> indexManager.createIndex(metadata)
        );
    }

    @Test
    void shouldRejectInvalidIndexName() {
        assertThrows(
                InvalidIndexException.class,
                () -> indexManager.createIndex(
                        " ",
                        "school_db",
                        "students",
                        "student_id",
                        IndexType.PRIMARY
                )
        );
    }

    @Test
    void shouldRejectInvalidDatabaseName() {
        assertThrows(
                InvalidIndexException.class,
                () -> indexManager.createIndex(
                        "idx_students_id",
                        null,
                        "students",
                        "student_id",
                        IndexType.PRIMARY
                )
        );
    }

    @Test
    void shouldRejectInvalidTableName() {
        assertThrows(
                InvalidIndexException.class,
                () -> indexManager.createIndex(
                        "idx_students_id",
                        "school_db",
                        "",
                        "student_id",
                        IndexType.PRIMARY
                )
        );
    }

    @Test
    void shouldRejectInvalidColumnName() {
        assertThrows(
                InvalidIndexException.class,
                () -> indexManager.createIndex(
                        "idx_students_id",
                        "school_db",
                        "students",
                        " ",
                        IndexType.PRIMARY
                )
        );
    }

    @Test
    void shouldRejectNullIndexType() {
        assertThrows(
                InvalidIndexException.class,
                () -> indexManager.createIndex(
                        "idx_students_id",
                        "school_db",
                        "students",
                        "student_id",
                        null
                )
        );
    }

    @Test
    void shouldRejectNullMetadata() {
        assertThrows(
                InvalidIndexException.class,
                () -> indexManager.createIndex(
                        (IndexMetadata) null
                )
        );
    }

    @Test
    void shouldGetExistingIndex() {
        Index<Integer> created = indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        Index<?> result =
                indexManager.getIndex("idx_students_id");

        assertSame(created, result);
    }

    @Test
    void shouldGetTypedIndex() {
        Index<Integer> created = indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        Index<Integer> result =
                indexManager.getTypedIndex("idx_students_id");

        assertSame(created, result);
    }

    @Test
    void shouldThrowWhenGettingMissingIndex() {
        assertThrows(
                IndexNotFoundException.class,
                () -> indexManager.getIndex("idx_missing")
        );
    }

    @Test
    void shouldReturnFalseForInvalidIndexExistsInput() {
        assertFalse(indexManager.indexExists(null));
        assertFalse(indexManager.indexExists(""));
        assertFalse(indexManager.indexExists(" "));
    }

    @Test
    void shouldDropExistingIndex() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        Index<?> removed =
                indexManager.dropIndex("idx_students_id");

        assertEquals(
                "idx_students_id",
                removed.getMetadata().getIndexName()
        );

        assertFalse(indexManager.indexExists("idx_students_id"));
        assertTrue(indexManager.isEmpty());
    }

    @Test
    void shouldThrowWhenDroppingMissingIndex() {
        assertThrows(
                IndexNotFoundException.class,
                () -> indexManager.dropIndex("idx_missing")
        );
    }

    @Test
    void shouldListIndexesForTable() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        indexManager.createIndex(
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );

        indexManager.createIndex(
                "idx_teachers_id",
                "school_db",
                "teachers",
                "teacher_id",
                IndexType.PRIMARY
        );

        List<Index<?>> result =
                indexManager.getIndexesForTable(
                        "school_db",
                        "students"
                );

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenTableHasNoIndexes() {
        List<Index<?>> result =
                indexManager.getIndexesForTable(
                        "school_db",
                        "students"
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnUnmodifiableTableIndexList() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        List<Index<?>> result =
                indexManager.getIndexesForTable(
                        "school_db",
                        "students"
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.add(
                        indexManager.getIndex("idx_students_id")
                )
        );
    }

    @Test
    void shouldListIndexesForColumn() {
        indexManager.createIndex(
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );

        indexManager.createIndex(
                "idx_students_city_unique",
                "school_db",
                "students",
                "city",
                IndexType.UNIQUE
        );

        indexManager.createIndex(
                "idx_students_email",
                "school_db",
                "students",
                "email",
                IndexType.UNIQUE
        );

        List<Index<?>> result =
                indexManager.getIndexesForColumn(
                        "school_db",
                        "students",
                        "city"
                );

        assertEquals(2, result.size());
    }

    @Test
    void shouldInsertAndSearchEntry() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        RecordPointer pointer = new RecordPointer(2, 4);

        indexManager.insertEntry(
                "idx_students_id",
                1001,
                pointer
        );

        List<RecordPointer> result =
                indexManager.search(
                        "idx_students_id",
                        1001
                );

        assertEquals(List.of(pointer), result);
    }

    @Test
    void shouldRejectDuplicateKeyThroughManager() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        indexManager.insertEntry(
                "idx_students_id",
                1001,
                new RecordPointer(1, 1)
        );

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> indexManager.insertEntry(
                        "idx_students_id",
                        1001,
                        new RecordPointer(2, 2)
                )
        );
    }

    @Test
    void shouldDeleteEntireEntry() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        indexManager.insertEntry(
                "idx_students_id",
                1001,
                new RecordPointer(1, 1)
        );

        assertTrue(
                indexManager.deleteEntry(
                        "idx_students_id",
                        1001
                )
        );

        assertTrue(
                indexManager.search(
                        "idx_students_id",
                        1001
                ).isEmpty()
        );
    }

    @Test
    void shouldDeleteSinglePointer() {
        indexManager.createIndex(
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );

        RecordPointer first = new RecordPointer(1, 1);
        RecordPointer second = new RecordPointer(1, 2);

        indexManager.insertEntry(
                "idx_students_city",
                "Malatya",
                first
        );

        indexManager.insertEntry(
                "idx_students_city",
                "Malatya",
                second
        );

        assertTrue(
                indexManager.deleteEntry(
                        "idx_students_city",
                        "Malatya",
                        first
                )
        );

        assertEquals(
                List.of(second),
                indexManager.search(
                        "idx_students_city",
                        "Malatya"
                )
        );
    }

    @Test
    void shouldUpdatePointer() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        RecordPointer oldPointer = new RecordPointer(1, 1);
        RecordPointer newPointer = new RecordPointer(5, 8);

        indexManager.insertEntry(
                "idx_students_id",
                1001,
                oldPointer
        );

        assertTrue(
                indexManager.updateEntry(
                        "idx_students_id",
                        1001,
                        oldPointer,
                        newPointer
                )
        );

        assertEquals(
                List.of(newPointer),
                indexManager.search(
                        "idx_students_id",
                        1001
                )
        );
    }

    @Test
    void shouldReturnAllIndexesAsUnmodifiableMap() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        Map<String, Index<?>> result =
                indexManager.getAllIndexes();

        assertEquals(1, result.size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.clear()
        );
    }

    @Test
    void shouldClearAllIndexes() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        indexManager.createIndex(
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );

        indexManager.clear();

        assertTrue(indexManager.isEmpty());
        assertEquals(0, indexManager.size());
    }

    @Test
    void shouldDropAllIndexesForTable() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        indexManager.createIndex(
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );

        indexManager.createIndex(
                "idx_teachers_id",
                "school_db",
                "teachers",
                "teacher_id",
                IndexType.PRIMARY
        );

        int removedCount =
                indexManager.dropIndexesForTable(
                        "school_db",
                        "students"
                );

        assertEquals(2, removedCount);
        assertEquals(1, indexManager.size());
        assertTrue(indexManager.indexExists("idx_teachers_id"));
    }

    @Test
    void shouldReturnZeroWhenTableHasNoIndexesToDrop() {
        int removedCount =
                indexManager.dropIndexesForTable(
                        "school_db",
                        "students"
                );

        assertEquals(0, removedCount);
    }

    @Test
    void shouldContinueGeneratedIdsAfterMetadataIndex() {
        IndexMetadata metadata = new IndexMetadata(
                25L,
                "idx_manual",
                "school_db",
                "students",
                "manual_column",
                IndexType.UNIQUE
        );

        indexManager.createIndex(metadata);

        Index<Integer> generated =
                indexManager.createIndex(
                        "idx_generated",
                        "school_db",
                        "students",
                        "student_id",
                        IndexType.PRIMARY
                );

        assertEquals(
                26L,
                generated.getMetadata().getIndexId()
        );
    }

    @Test
    void shouldReturnExpectedStringRepresentation() {
        indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        String text = indexManager.toString();

        assertTrue(text.contains("indexCount=1"));
        assertTrue(text.contains("idx_students_id"));
    }
}