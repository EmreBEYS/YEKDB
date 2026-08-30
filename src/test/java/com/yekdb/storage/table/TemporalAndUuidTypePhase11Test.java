package com.yekdb.storage.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemporalAndUuidTypePhase11Test {

    @Test
    void shouldParseUuidType() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("UUID");
        assertEquals(DataType.UUID, type.dataType());
        assertEquals("UUID", type.declaration());
    }

    @Test
    void shouldParseDateTimeTimestampAndIntervalTypes() {
        assertEquals(DataType.DATE, ColumnTypeDefinition.parse("DATE").dataType());
        assertEquals(DataType.TIME, ColumnTypeDefinition.parse("TIME").dataType());
        assertEquals(DataType.TIMESTAMP, ColumnTypeDefinition.parse("TIMESTAMP").dataType());
        assertEquals(DataType.INTERVAL, ColumnTypeDefinition.parse("INTERVAL").dataType());
    }

    @Test
    void shouldAcceptValidUuid() {
        Column column = new Column("id", DataType.UUID);
        assertTrue(column.acceptsTypedStringValue("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void shouldRejectInvalidUuid() {
        Column column = new Column("id", DataType.UUID);
        assertFalse(column.acceptsTypedStringValue("not-a-uuid"));
    }

    @Test
    void shouldValidateDate() {
        Column column = new Column("event_date", DataType.DATE);
        assertTrue(column.acceptsTypedStringValue("2026-08-30"));
        assertFalse(column.acceptsTypedStringValue("2026-02-30"));
    }

    @Test
    void shouldValidateTime() {
        Column column = new Column("event_time", DataType.TIME);
        assertTrue(column.acceptsTypedStringValue("13:35:06.980471"));
        assertFalse(column.acceptsTypedStringValue("25:10:00"));
    }

    @Test
    void shouldValidateTimestampWithoutOffset() {
        Column column = new Column("created_at", DataType.TIMESTAMP);
        assertTrue(column.acceptsTypedStringValue("2018-11-05 13:35:06.980471"));
        assertFalse(column.acceptsTypedStringValue("2018-11-05 28:35:06"));
    }

    @Test
    void shouldValidateTimestampWithOffset() {
        Column column = new Column("created_at", DataType.TIMESTAMP);
        assertTrue(column.acceptsTypedStringValue("2018-11-05 13:35:06.980471+03:00"));
    }

    @Test
    void shouldValidateHumanReadableInterval() {
        Column column = new Column("duration", DataType.INTERVAL);
        assertTrue(column.acceptsTypedStringValue(
                "6 years 5 months 4 days 3 hours 2 minutes 1 second"
        ));
        assertFalse(column.acceptsTypedStringValue("six years"));
    }

    @Test
    void shouldValidateIsoInterval() {
        Column column = new Column("duration", DataType.INTERVAL);
        assertTrue(column.acceptsTypedStringValue("P6Y5M4DT3H2M1S"));
        assertFalse(column.acceptsTypedStringValue("P"));
    }
}
