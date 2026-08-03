package com.yekdb.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordPointerTest {

    @Test
    void shouldCreateRecordPointerWithConstructor() {
        RecordPointer pointer = new RecordPointer(5, 12);

        assertEquals(5, pointer.getPageId());
        assertEquals(12, pointer.getSlotId());
    }

    @Test
    void shouldCreateRecordPointerWithDefaultConstructor() {
        RecordPointer pointer = new RecordPointer();

        assertEquals(0, pointer.getPageId());
        assertEquals(0, pointer.getSlotId());
    }

    @Test
    void shouldUpdatePageIdAndSlotId() {
        RecordPointer pointer = new RecordPointer();

        pointer.setPageId(8);
        pointer.setSlotId(21);

        assertEquals(8, pointer.getPageId());
        assertEquals(21, pointer.getSlotId());
    }

    @Test
    void shouldBeValidWhenPageAndSlotAreNonNegative() {
        RecordPointer pointer = new RecordPointer(0, 0);

        assertTrue(pointer.isValid());
    }

    @Test
    void shouldBeInvalidWhenPageIdIsNegative() {
        RecordPointer pointer = new RecordPointer(-1, 4);

        assertFalse(pointer.isValid());
    }

    @Test
    void shouldBeInvalidWhenSlotIdIsNegative() {
        RecordPointer pointer = new RecordPointer(3, -1);

        assertFalse(pointer.isValid());
    }

    @Test
    void shouldBeInvalidWhenPageAndSlotAreNegative() {
        RecordPointer pointer = new RecordPointer(-1, -1);

        assertFalse(pointer.isValid());
    }

    @Test
    void shouldBeEqualWhenPageAndSlotAreEqual() {
        RecordPointer firstPointer = new RecordPointer(2, 7);
        RecordPointer secondPointer = new RecordPointer(2, 7);

        assertEquals(firstPointer, secondPointer);
        assertEquals(firstPointer.hashCode(), secondPointer.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenPageIdsAreDifferent() {
        RecordPointer firstPointer = new RecordPointer(2, 7);
        RecordPointer secondPointer = new RecordPointer(3, 7);

        assertNotEquals(firstPointer, secondPointer);
    }

    @Test
    void shouldNotBeEqualWhenSlotIdsAreDifferent() {
        RecordPointer firstPointer = new RecordPointer(2, 7);
        RecordPointer secondPointer = new RecordPointer(2, 8);

        assertNotEquals(firstPointer, secondPointer);
    }

    @Test
    void shouldNotBeEqualToNull() {
        RecordPointer pointer = new RecordPointer(2, 7);

        assertNotEquals(null, pointer);
    }

    @Test
    void shouldReturnExpectedStringRepresentation() {
        RecordPointer pointer = new RecordPointer(4, 9);

        assertEquals(
                "RecordPointer{pageId=4, slotId=9}",
                pointer.toString()
        );
    }
}