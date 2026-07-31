package com.yekdb.storage.record;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RowTest {

    @Test
    void shouldCreateEmptyRow() {

        Row row = new Row();

        assertTrue(row.isEmpty());
        assertEquals(0, row.size());
    }

    @Test
    void shouldCreateRowWithSupportedValues() {

        Row row = new Row(
                List.of(
                        1,
                        25L,
                        91.50,
                        true,
                        "YEKDB"
                )
        );

        assertEquals(5, row.size());
        assertFalse(row.isEmpty());

        assertEquals(1, row.getValue(0));
        assertEquals(25L, row.getValue(1));
        assertEquals(91.50, row.getValue(2));
        assertEquals(true, row.getValue(3));
        assertEquals("YEKDB", row.getValue(4));
    }

    @Test
    void shouldAddSupportedValues() {

        Row row = new Row();

        row.addValue(10);
        row.addValue(100L);
        row.addValue(42.25);
        row.addValue(false);
        row.addValue("Database");

        assertEquals(5, row.size());
        assertEquals("Database", row.getValue(4));
    }

    @Test
    void shouldReturnTypedValue() {

        Row row = new Row(
                List.of(
                        42,
                        "YEKDB"
                )
        );

        Integer id =
                row.getValue(0, Integer.class);

        String name =
                row.getValue(1, String.class);

        assertEquals(42, id);
        assertEquals("YEKDB", name);
    }

    @Test
    void shouldRejectIncorrectExpectedType() {

        Row row = new Row(
                List.of("YEKDB")
        );

        assertThrows(
                IllegalStateException.class,
                () -> row.getValue(
                        0,
                        Integer.class
                )
        );
    }

    @Test
    void shouldRejectNullExpectedType() {

        Row row = new Row(
                List.of(1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> row.getValue(
                        0,
                        null
                )
        );
    }

    @Test
    void shouldSetExistingValue() {

        Row row = new Row(
                List.of(
                        1,
                        "Old value"
                )
        );

        row.setValue(
                1,
                "New value"
        );

        assertEquals(
                "New value",
                row.getValue(1)
        );
    }

    @Test
    void shouldContainExistingValue() {

        Row row = new Row(
                List.of(
                        1,
                        "YEKDB",
                        true
                )
        );

        assertTrue(row.contains("YEKDB"));
        assertTrue(row.contains(true));
        assertFalse(row.contains("Unknown"));
    }

    @Test
    void shouldClearValues() {

        Row row = new Row(
                List.of(
                        1,
                        "YEKDB"
                )
        );

        row.clear();

        assertTrue(row.isEmpty());
        assertEquals(0, row.size());
    }

    @Test
    void shouldReturnUnmodifiableValues() {

        Row row = new Row(
                List.of(
                        1,
                        "YEKDB"
                )
        );

        List<Object> values =
                row.getValues();

        assertThrows(
                UnsupportedOperationException.class,
                () -> values.add("Invalid")
        );

        assertEquals(2, row.size());
    }

    @Test
    void shouldCreateDefensiveCopyOfInputList() {

        List<Object> originalValues =
                new java.util.ArrayList<>(
                        List.of(
                                1,
                                "YEKDB"
                        )
                );

        Row row =
                new Row(originalValues);

        originalValues.set(
                1,
                "Changed externally"
        );

        assertEquals(
                "YEKDB",
                row.getValue(1)
        );
    }

    @Test
    void shouldRejectNullValueList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Row(null)
        );
    }

    @Test
    void shouldRejectNullColumnValue() {

        Row row = new Row();

        assertThrows(
                IllegalArgumentException.class,
                () -> row.addValue(null)
        );
    }

    @Test
    void shouldRejectUnsupportedValueType() {

        Row row = new Row();

        assertThrows(
                IllegalArgumentException.class,
                () -> row.addValue(
                        new StringBuilder("Unsupported")
                )
        );
    }

    @Test
    void shouldRejectNegativeIndex() {

        Row row = new Row(
                List.of(1)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> row.getValue(-1)
        );
    }

    @Test
    void shouldRejectIndexEqualToSize() {

        Row row = new Row(
                List.of(
                        1,
                        "YEKDB"
                )
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> row.getValue(2)
        );
    }

    @Test
    void shouldRejectInvalidIndexDuringUpdate() {

        Row row = new Row(
                List.of(1)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> row.setValue(
                        4,
                        "Invalid"
                )
        );
    }

    @Test
    void shouldCompareRowsByValues() {

        Row firstRow = new Row(
                List.of(
                        1,
                        "YEKDB",
                        true
                )
        );

        Row secondRow = new Row(
                List.of(
                        1,
                        "YEKDB",
                        true
                )
        );

        assertEquals(firstRow, secondRow);
        assertEquals(
                firstRow.hashCode(),
                secondRow.hashCode()
        );
    }

    @Test
    void shouldNotCompareRowsWithDifferentValues() {

        Row firstRow =
                new Row(List.of(1, "First"));

        Row secondRow =
                new Row(List.of(2, "Second"));

        assertNotEquals(firstRow, secondRow);
    }

    @Test
    void shouldProduceReadableStringRepresentation() {

        Row row = new Row(
                List.of(
                        1,
                        "YEKDB"
                )
        );

        String result =
                row.toString();

        assertTrue(result.contains("Row"));
        assertTrue(result.contains("YEKDB"));
    }
}