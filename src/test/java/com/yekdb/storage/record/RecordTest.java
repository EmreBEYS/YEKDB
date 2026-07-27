package com.yekdb.storage.record;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    void shouldCreateRecordSuccessfully() {
        byte[] data = "YEKDB".getBytes(StandardCharsets.UTF_8);

        Record record = new Record(1L, data);

        assertEquals(1L, record.getRecordId());
        assertArrayEquals(data, record.getData());
        assertEquals(data.length, record.getDataLength());
        assertFalse(record.isDeleted());
    }

    @Test
    void shouldRejectNegativeRecordId() {
        byte[] data = "YEKDB".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Record(-1L, data)
        );

        assertTrue(exception.getMessage().contains("negatif"));
    }

    @Test
    void shouldRejectNullData() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Record(1L, null)
        );
    }

    @Test
    void shouldUpdateRecordData() {
        Record record = new Record(
                1L,
                "Eski veri".getBytes(StandardCharsets.UTF_8)
        );

        byte[] newData =
                "Yeni veri".getBytes(StandardCharsets.UTF_8);

        record.updateData(newData);

        assertArrayEquals(newData, record.getData());
        assertEquals(newData.length, record.getDataLength());
    }

    @Test
    void shouldMarkRecordAsDeleted() {
        Record record = new Record(
                1L,
                "YEKDB".getBytes(StandardCharsets.UTF_8)
        );

        record.markAsDeleted();

        assertTrue(record.isDeleted());
    }

    @Test
    void shouldNotUpdateDeletedRecord() {
        Record record = new Record(
                1L,
                "YEKDB".getBytes(StandardCharsets.UTF_8)
        );

        record.markAsDeleted();

        assertThrows(
                IllegalStateException.class,
                () -> record.updateData(
                        "Yeni veri".getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    @Test
    void shouldProtectInternalDataFromExternalChanges() {
        byte[] originalData =
                "YEKDB".getBytes(StandardCharsets.UTF_8);

        Record record = new Record(1L, originalData);

        originalData[0] = 'X';

        assertEquals(
                "YEKDB",
                new String(
                        record.getData(),
                        StandardCharsets.UTF_8
                )
        );

        byte[] returnedData = record.getData();
        returnedData[0] = 'Z';

        assertEquals(
                "YEKDB",
                new String(
                        record.getData(),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void recordsWithSameIdShouldBeEqual() {
        Record firstRecord = new Record(
                1L,
                "Birinci".getBytes(StandardCharsets.UTF_8)
        );

        Record secondRecord = new Record(
                1L,
                "İkinci".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(firstRecord, secondRecord);
        assertEquals(
                firstRecord.hashCode(),
                secondRecord.hashCode()
        );
    }

    @Test
    void recordsWithDifferentIdsShouldNotBeEqual() {
        Record firstRecord = new Record(
                1L,
                new byte[0]
        );

        Record secondRecord = new Record(
                2L,
                new byte[0]
        );

        assertNotEquals(firstRecord, secondRecord);
    }
}