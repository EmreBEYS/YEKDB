package com.yekdb.query.executor;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JoinedRowTest {

    @Test
    void shouldCreateEmptyJoinedRow() {

        JoinedRow row = new JoinedRow();

        assertTrue(row.isEmpty());
        assertEquals(0, row.size());
    }

    @Test
    void shouldPutAndGetQualifiedColumn() {

        JoinedRow row = new JoinedRow();

        row.put("e.id", 1);
        row.put("e.name", "Yunus Emre");

        assertEquals(1, row.get("e.id"));
        assertEquals("Yunus Emre", row.get("e.name"));
    }

    @Test
    void shouldContainQualifiedColumn() {

        JoinedRow row = new JoinedRow();

        row.put("d.name", "Software");

        assertTrue(row.contains("d.name"));
        assertFalse(row.contains("d.id"));
    }

    @Test
    void shouldReturnCorrectSize() {

        JoinedRow row = new JoinedRow();

        row.put("e.id", 1);
        row.put("e.name", "Yunus Emre");
        row.put("d.id", 10);
        row.put("d.name", "Software");

        assertEquals(4, row.size());
    }

    @Test
    void shouldCreateJoinedRowFromMap() {

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("e.id", 1);
        values.put("e.name", "Yunus Emre");

        JoinedRow row = new JoinedRow(values);

        assertEquals(2, row.size());
        assertEquals(1, row.get("e.id"));
        assertEquals("Yunus Emre", row.get("e.name"));
    }

    @Test
    void valuesShouldBeUnmodifiable() {

        JoinedRow row = new JoinedRow();

        row.put("e.id", 1);

        /*
         * getValues() üzerinden JoinedRow'un iç yapısının
         * değiştirilmesine izin verilmemelidir.
         */
        assertThrows(
                UnsupportedOperationException.class,
                () -> row.getValues().put("e.name", "Test")
        );
    }

    @Test
    void nullColumnNameShouldThrow() {

        JoinedRow row = new JoinedRow();

        assertThrows(
                NullPointerException.class,
                () -> row.put(null, 1)
        );
    }

    @Test
    void blankColumnNameShouldThrow() {

        JoinedRow row = new JoinedRow();

        assertThrows(
                IllegalArgumentException.class,
                () -> row.put("   ", 1)
        );
    }
}