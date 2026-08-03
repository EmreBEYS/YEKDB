package com.yekdb.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexEntryTest {

    @Test
    void shouldCreateIndexEntryWithConstructor() {

        RecordPointer pointer = new RecordPointer(5, 10);

        IndexEntry<Integer> entry =
                new IndexEntry<>(1001, pointer);

        assertEquals(1001, entry.getKey());
        assertEquals(pointer, entry.getPointer());
    }

    @Test
    void shouldCreateIndexEntryWithDefaultConstructor() {

        IndexEntry<Integer> entry =
                new IndexEntry<>();

        assertNull(entry.getKey());
        assertNull(entry.getPointer());
    }

    @Test
    void shouldUpdateKeyAndPointer() {

        IndexEntry<Integer> entry =
                new IndexEntry<>();

        RecordPointer pointer =
                new RecordPointer(2, 8);

        entry.setKey(500);

        entry.setPointer(pointer);

        assertEquals(500, entry.getKey());

        assertEquals(pointer, entry.getPointer());
    }

    @Test
    void shouldBeValid() {

        IndexEntry<Integer> entry =
                new IndexEntry<>(
                        100,
                        new RecordPointer(1, 1)
                );

        assertTrue(entry.isValid());
    }

    @Test
    void shouldBeInvalidWhenKeyIsNull() {

        IndexEntry<Integer> entry =
                new IndexEntry<>(
                        null,
                        new RecordPointer(1, 1)
                );

        assertFalse(entry.isValid());
    }

    @Test
    void shouldBeInvalidWhenPointerIsNull() {

        IndexEntry<Integer> entry =
                new IndexEntry<>(
                        100,
                        null
                );

        assertFalse(entry.isValid());
    }

    @Test
    void shouldBeInvalidWhenPointerIsInvalid() {

        IndexEntry<Integer> entry =
                new IndexEntry<>(
                        100,
                        new RecordPointer(-1, 5)
                );

        assertFalse(entry.isValid());
    }

    @Test
    void shouldCompareEntriesCorrectly() {

        IndexEntry<Integer> first =
                new IndexEntry<>(
                        10,
                        new RecordPointer(1, 1)
                );

        IndexEntry<Integer> second =
                new IndexEntry<>(
                        20,
                        new RecordPointer(2, 2)
                );

        assertTrue(first.compareTo(second) < 0);

        assertTrue(second.compareTo(first) > 0);

        assertEquals(
                0,
                first.compareTo(
                        new IndexEntry<>(
                                10,
                                new RecordPointer(3, 3)
                        )
                )
        );
    }

    @Test
    void shouldBeEqual() {

        IndexEntry<Integer> first =
                new IndexEntry<>(
                        100,
                        new RecordPointer(5, 5)
                );

        IndexEntry<Integer> second =
                new IndexEntry<>(
                        100,
                        new RecordPointer(5, 5)
                );

        assertEquals(first, second);

        assertEquals(
                first.hashCode(),
                second.hashCode()
        );
    }

    @Test
    void shouldNotBeEqual() {

        IndexEntry<Integer> first =
                new IndexEntry<>(
                        100,
                        new RecordPointer(1, 1)
                );

        IndexEntry<Integer> second =
                new IndexEntry<>(
                        101,
                        new RecordPointer(1, 1)
                );

        assertNotEquals(first, second);
    }

    @Test
    void shouldReturnExpectedStringRepresentation() {

        IndexEntry<Integer> entry =
                new IndexEntry<>(
                        50,
                        new RecordPointer(3, 7)
                );

        String text = entry.toString();

        assertTrue(text.contains("50"));

        assertTrue(text.contains("pageId=3"));

        assertTrue(text.contains("slotId=7"));
    }

}