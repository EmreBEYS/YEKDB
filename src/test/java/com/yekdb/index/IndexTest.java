package com.yekdb.index;

import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.index.exception.InvalidIndexException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexTest {

    private IndexMetadata primaryMetadata;
    private IndexMetadata nonUniqueMetadata;

    @BeforeEach
    void setUp() {
        primaryMetadata = new IndexMetadata(
                1L,
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        nonUniqueMetadata = new IndexMetadata(
                2L,
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );
    }

    @Test
    void shouldCreateIndexWithValidMetadata() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertEquals(primaryMetadata, index.getMetadata());
        assertTrue(index.isEmpty());
        assertEquals(0, index.size());
        assertEquals(0, index.pointerCount());
    }

    @Test
    void shouldRejectNullMetadata() {
        assertThrows(
                InvalidIndexException.class,
                () -> new Index<Integer>(null)
        );
    }

    @Test
    void shouldRejectInvalidMetadata() {
        primaryMetadata.setIndexName(" ");

        assertThrows(
                InvalidIndexException.class,
                () -> new Index<Integer>(primaryMetadata)
        );
    }

    @Test
    void shouldInsertEntryIntoPrimaryIndex() {
        Index<Integer> index = new Index<>(primaryMetadata);
        RecordPointer pointer = new RecordPointer(1, 4);

        index.insert(1001, pointer);

        assertTrue(index.containsKey(1001));
        assertEquals(List.of(pointer), index.search(1001));
        assertEquals(1, index.size());
        assertEquals(1, index.pointerCount());
    }

    @Test
    void shouldInsertIndexEntryObject() {
        Index<Integer> index = new Index<>(primaryMetadata);

        IndexEntry<Integer> entry = new IndexEntry<>(
                1001,
                new RecordPointer(2, 5)
        );

        index.insert(entry);

        assertEquals(
                List.of(new RecordPointer(2, 5)),
                index.search(1001)
        );
    }

    @Test
    void shouldRejectNullIndexEntry() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertThrows(
                InvalidIndexException.class,
                () -> index.insert((IndexEntry<Integer>) null)
        );
    }

    @Test
    void shouldRejectInvalidIndexEntry() {
        Index<Integer> index = new Index<>(primaryMetadata);

        IndexEntry<Integer> entry = new IndexEntry<>(
                null,
                new RecordPointer(1, 1)
        );

        assertThrows(
                InvalidIndexException.class,
                () -> index.insert(entry)
        );
    }

    @Test
    void shouldRejectNullKey() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertThrows(
                InvalidIndexException.class,
                () -> index.insert(
                        null,
                        new RecordPointer(1, 1)
                )
        );
    }

    @Test
    void shouldRejectNullPointer() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertThrows(
                InvalidIndexException.class,
                () -> index.insert(1001, null)
        );
    }

    @Test
    void shouldRejectInvalidPointer() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertThrows(
                InvalidIndexException.class,
                () -> index.insert(
                        1001,
                        new RecordPointer(-1, 5)
                )
        );
    }

    @Test
    void shouldRejectDuplicateKeyInPrimaryIndex() {
        Index<Integer> index = new Index<>(primaryMetadata);

        index.insert(
                1001,
                new RecordPointer(1, 1)
        );

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> index.insert(
                        1001,
                        new RecordPointer(2, 2)
                )
        );
    }

    @Test
    void shouldRejectDuplicateKeyInUniqueIndex() {
        IndexMetadata uniqueMetadata = new IndexMetadata(
                3L,
                "idx_students_email",
                "school_db",
                "students",
                "email",
                IndexType.UNIQUE
        );

        Index<String> index = new Index<>(uniqueMetadata);

        index.insert(
                "student@example.com",
                new RecordPointer(1, 1)
        );

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> index.insert(
                        "student@example.com",
                        new RecordPointer(2, 2)
                )
        );
    }

    @Test
    void shouldAllowSameKeyWithDifferentPointersInNonUniqueIndex() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        RecordPointer firstPointer = new RecordPointer(1, 1);
        RecordPointer secondPointer = new RecordPointer(1, 2);

        index.insert("Malatya", firstPointer);
        index.insert("Malatya", secondPointer);

        assertEquals(
                List.of(firstPointer, secondPointer),
                index.search("Malatya")
        );

        assertEquals(1, index.size());
        assertEquals(2, index.pointerCount());
    }

    @Test
    void shouldIgnoreExactDuplicatePointerInNonUniqueIndex() {
        Index<String> index = new Index<>(nonUniqueMetadata);
        RecordPointer pointer = new RecordPointer(1, 1);

        index.insert("Malatya", pointer);
        index.insert("Malatya", pointer);

        assertEquals(1, index.size());
        assertEquals(1, index.pointerCount());
    }

    @Test
    void shouldReturnEmptyListWhenKeyDoesNotExist() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertTrue(index.search(9999).isEmpty());
    }

    @Test
    void shouldReturnUnmodifiableSearchResult() {
        Index<Integer> index = new Index<>(primaryMetadata);

        index.insert(
                1001,
                new RecordPointer(1, 1)
        );

        List<RecordPointer> result = index.search(1001);

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.add(new RecordPointer(2, 2))
        );
    }

    @Test
    void shouldRemoveKeyAndAllPointers() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        index.insert(
                "Malatya",
                new RecordPointer(1, 1)
        );

        index.insert(
                "Malatya",
                new RecordPointer(1, 2)
        );

        assertTrue(index.remove("Malatya"));
        assertFalse(index.containsKey("Malatya"));
        assertTrue(index.isEmpty());
    }

    @Test
    void shouldReturnFalseWhenRemovingMissingKey() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertFalse(index.remove(9999));
    }

    @Test
    void shouldRemoveSinglePointerFromNonUniqueIndex() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        RecordPointer firstPointer = new RecordPointer(1, 1);
        RecordPointer secondPointer = new RecordPointer(1, 2);

        index.insert("Malatya", firstPointer);
        index.insert("Malatya", secondPointer);

        assertTrue(index.remove("Malatya", firstPointer));

        assertEquals(
                List.of(secondPointer),
                index.search("Malatya")
        );
    }

    @Test
    void shouldRemoveKeyWhenLastPointerIsRemoved() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        RecordPointer pointer = new RecordPointer(1, 1);

        index.insert("Malatya", pointer);

        assertTrue(index.remove("Malatya", pointer));
        assertFalse(index.containsKey("Malatya"));
    }

    @Test
    void shouldReturnFalseWhenPointerDoesNotExist() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        index.insert(
                "Malatya",
                new RecordPointer(1, 1)
        );

        assertFalse(
                index.remove(
                        "Malatya",
                        new RecordPointer(9, 9)
                )
        );
    }

    @Test
    void shouldUpdateExistingPointer() {
        Index<Integer> index = new Index<>(primaryMetadata);

        RecordPointer oldPointer = new RecordPointer(1, 1);
        RecordPointer newPointer = new RecordPointer(5, 8);

        index.insert(1001, oldPointer);

        assertTrue(
                index.update(
                        1001,
                        oldPointer,
                        newPointer
                )
        );

        assertEquals(
                List.of(newPointer),
                index.search(1001)
        );
    }

    @Test
    void shouldReturnFalseWhenUpdatingMissingKey() {
        Index<Integer> index = new Index<>(primaryMetadata);

        assertFalse(
                index.update(
                        9999,
                        new RecordPointer(1, 1),
                        new RecordPointer(2, 2)
                )
        );
    }

    @Test
    void shouldReturnFalseWhenUpdatingMissingPointer() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        index.insert(
                "Malatya",
                new RecordPointer(1, 1)
        );

        assertFalse(
                index.update(
                        "Malatya",
                        new RecordPointer(2, 2),
                        new RecordPointer(3, 3)
                )
        );
    }

    @Test
    void shouldRejectUpdateToExistingDifferentPointer() {
        Index<String> index = new Index<>(nonUniqueMetadata);

        RecordPointer firstPointer = new RecordPointer(1, 1);
        RecordPointer secondPointer = new RecordPointer(1, 2);

        index.insert("Malatya", firstPointer);
        index.insert("Malatya", secondPointer);

        assertFalse(
                index.update(
                        "Malatya",
                        firstPointer,
                        secondPointer
                )
        );
    }

    @Test
    void shouldClearAllEntries() {
        Index<Integer> index = new Index<>(primaryMetadata);

        index.insert(1001, new RecordPointer(1, 1));
        index.insert(1002, new RecordPointer(1, 2));

        index.clear();

        assertTrue(index.isEmpty());
        assertEquals(0, index.size());
        assertEquals(0, index.pointerCount());
    }

    @Test
    void shouldReturnUnmodifiableEntriesMap() {
        Index<Integer> index = new Index<>(primaryMetadata);

        index.insert(
                1001,
                new RecordPointer(1, 1)
        );

        Map<Integer, List<RecordPointer>> entries =
                index.getAllEntries();

        assertThrows(
                UnsupportedOperationException.class,
                () -> entries.put(
                        1002,
                        List.of(new RecordPointer(1, 2))
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> entries.get(1001)
                        .add(new RecordPointer(2, 2))
        );
    }

    @Test
    void shouldReturnSortedEntryList() {
        Index<Integer> index = new Index<>(
                new IndexMetadata(
                        4L,
                        "idx_values",
                        "school_db",
                        "values_table",
                        "value",
                        IndexType.NON_UNIQUE
                )
        );

        index.insert(30, new RecordPointer(1, 3));
        index.insert(10, new RecordPointer(1, 1));
        index.insert(20, new RecordPointer(1, 2));

        List<IndexEntry<Integer>> entries =
                index.getEntryList();

        assertEquals(10, entries.get(0).getKey());
        assertEquals(20, entries.get(1).getKey());
        assertEquals(30, entries.get(2).getKey());
    }

    @Test
    void shouldReturnUnmodifiableEntryList() {
        Index<Integer> index = new Index<>(primaryMetadata);

        index.insert(
                1001,
                new RecordPointer(1, 1)
        );

        List<IndexEntry<Integer>> entries =
                index.getEntryList();

        assertThrows(
                UnsupportedOperationException.class,
                () -> entries.add(
                        new IndexEntry<>(
                                1002,
                                new RecordPointer(1, 2)
                        )
                )
        );
    }

    @Test
    void shouldReturnExpectedStringRepresentation() {
        Index<Integer> index = new Index<>(primaryMetadata);

        index.insert(
                1001,
                new RecordPointer(1, 1)
        );

        String text = index.toString();

        assertTrue(text.contains("idx_students_id"));
        assertTrue(text.contains("keyCount=1"));
        assertTrue(text.contains("pointerCount=1"));
    }
}