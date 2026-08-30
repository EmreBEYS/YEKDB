package com.yekdb.storage.record;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RowSerializerExtendedTypesPhase15Test {

    @Test
    void shouldRoundTripTemporalAndUuidTextValues() {
        Row row = new Row(List.of(
                "550e8400-e29b-41d4-a716-446655440000",
                "2026-08-30",
                "13:35:06.980471",
                "2018-11-05 13:35:06.980471+03:00"
        ));

        assertEquals(row, RowSerializer.deserialize(RowSerializer.serialize(row)));
    }

    @Test
    void shouldRoundTripJsonArrayAndHstoreTextValues() {
        Row row = new Row(List.of(
                "{\"city\":\"Malatya\",\"active\":true}",
                "{1,2,3}",
                "\"brand\"=>\"ASUS\", \"model\"=>\"TUF\""
        ));

        assertEquals(row, RowSerializer.deserialize(RowSerializer.serialize(row)));
    }

    @Test
    void shouldRoundTripUdtTextValue() {
        Row row = new Row(List.of("custom-value"));

        assertEquals(row, RowSerializer.deserialize(RowSerializer.serialize(row)));
    }

    @Test
    void shouldKeepCalculatedSizeExactForExtendedTextValues() {
        Row row = new Row(List.of(
                "2026-08-30",
                "{1,2,3}",
                "{\"ok\":true}"
        ));

        byte[] bytes = RowSerializer.serialize(row);
        assertEquals(bytes.length, RowSerializer.calculateSerializedSize(row));
    }
}
