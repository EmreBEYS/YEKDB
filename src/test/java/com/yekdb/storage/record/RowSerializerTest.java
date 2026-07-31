package com.yekdb.storage.record;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RecordSerializerTest {

    @Test
    void shouldSerializeAndDeserializeRecord() {

        Record originalRecord = new Record(
                10L,
                new byte[]{1, 2, 3, 4}
        );

        byte[] serialized =
                RecordSerializer.serialize(originalRecord);

        Record restoredRecord =
                RecordSerializer.deserialize(serialized);

        assertEquals(
                originalRecord.getRecordId(),
                restoredRecord.getRecordId()
        );

        assertArrayEquals(
                originalRecord.getData(),
                restoredRecord.getData()
        );

        assertFalse(restoredRecord.isDeleted());
    }

    @Test
    void shouldPreserveDeletedState() {

        Record originalRecord = new Record(
                5L,
                new byte[]{10, 20}
        );

        originalRecord.markAsDeleted();

        byte[] serialized =
                RecordSerializer.serialize(originalRecord);

        Record restoredRecord =
                RecordSerializer.deserialize(serialized);

        assertTrue(restoredRecord.isDeleted());

        assertEquals(
                originalRecord.getRecordId(),
                restoredRecord.getRecordId()
        );

        assertArrayEquals(
                originalRecord.getData(),
                restoredRecord.getData()
        );
    }

    @Test
    void shouldSerializeEmptyData() {

        Record record = new Record(
                1L,
                new byte[0]
        );

        byte[] serialized =
                RecordSerializer.serialize(record);

        Record restoredRecord =
                RecordSerializer.deserialize(serialized);

        assertEquals(0, restoredRecord.getDataLength());
        assertArrayEquals(
                new byte[0],
                restoredRecord.getData()
        );
    }

    @Test
    void shouldCalculateCorrectSerializedSize() {

        Record record = new Record(
                7L,
                new byte[]{1, 2, 3, 4, 5}
        );

        int calculatedSize =
                RecordSerializer.calculateSerializedSize(record);

        byte[] serialized =
                RecordSerializer.serialize(record);

        assertEquals(
                serialized.length,
                calculatedSize
        );

        assertEquals(
                RecordSerializer.HEADER_SIZE + 5,
                calculatedSize
        );
    }

    @Test
    void shouldUseExpectedHeaderSize() {

        assertEquals(
                Long.BYTES
                        + Byte.BYTES
                        + Integer.BYTES,
                RecordSerializer.HEADER_SIZE
        );
    }

    @Test
    void shouldPreserveLargeRecordId() {

        Record originalRecord = new Record(
                Long.MAX_VALUE,
                new byte[]{42}
        );

        Record restoredRecord =
                roundTrip(originalRecord);

        assertEquals(
                Long.MAX_VALUE,
                restoredRecord.getRecordId()
        );
    }

    @Test
    void shouldPreserveBinaryData() {

        byte[] data = new byte[]{
                Byte.MIN_VALUE,
                -1,
                0,
                1,
                Byte.MAX_VALUE
        };

        Record originalRecord = new Record(
                15L,
                data
        );

        Record restoredRecord =
                roundTrip(originalRecord);

        assertArrayEquals(
                data,
                restoredRecord.getData()
        );
    }

    @Test
    void shouldCreateDefensiveDataCopy() {

        byte[] originalData =
                new byte[]{1, 2, 3};

        Record record =
                new Record(1L, originalData);

        originalData[0] = 99;

        assertArrayEquals(
                new byte[]{1, 2, 3},
                record.getData()
        );
    }

    @Test
    void shouldReturnDefensiveDataCopy() {

        Record record = new Record(
                1L,
                new byte[]{1, 2, 3}
        );

        byte[] returnedData =
                record.getData();

        returnedData[0] = 99;

        assertArrayEquals(
                new byte[]{1, 2, 3},
                record.getData()
        );
    }

    @Test
    void shouldRejectNullRecordDuringSerialization() {

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.serialize(null)
        );
    }

    @Test
    void shouldRejectNullRecordDuringSizeCalculation() {

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer
                        .calculateSerializedSize(null)
        );
    }

    @Test
    void shouldRejectNullBytesDuringDeserialization() {

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(null)
        );
    }

    @Test
    void shouldRejectBytesSmallerThanHeader() {

        byte[] invalidBytes =
                new byte[
                        RecordSerializer.HEADER_SIZE - 1
                        ];

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(
                        invalidBytes
                )
        );
    }

    @Test
    void shouldRejectInvalidDeletedFlag() {

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        RecordSerializer.HEADER_SIZE
                );

        buffer.putLong(1L);

        // Only zero and one are valid.
        buffer.put((byte) 2);

        buffer.putInt(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(
                        buffer.array()
                )
        );
    }

    @Test
    void shouldRejectNegativeDataLength() {

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        RecordSerializer.HEADER_SIZE
                );

        buffer.putLong(1L);
        buffer.put((byte) 0);
        buffer.putInt(-1);

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(
                        buffer.array()
                )
        );
    }

    @Test
    void shouldRejectDataLengthLargerThanRemainingBytes() {

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        RecordSerializer.HEADER_SIZE + 2
                );

        buffer.putLong(1L);
        buffer.put((byte) 0);

        // Claims that five data bytes exist.
        buffer.putInt(5);

        // Only two bytes are available.
        buffer.put((byte) 10);
        buffer.put((byte) 20);

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(
                        buffer.array()
                )
        );
    }

    @Test
    void shouldRejectUnexpectedTrailingBytes() {

        Record record = new Record(
                1L,
                new byte[]{10, 20}
        );

        byte[] validBytes =
                RecordSerializer.serialize(record);

        byte[] bytesWithTrailingData =
                Arrays.copyOf(
                        validBytes,
                        validBytes.length + 1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(
                        bytesWithTrailingData
                )
        );
    }

    @Test
    void shouldProduceSameBinaryForEquivalentRecords() {

        Record firstRecord = new Record(
                4L,
                new byte[]{1, 2, 3}
        );

        Record secondRecord = new Record(
                4L,
                new byte[]{1, 2, 3}
        );

        assertArrayEquals(
                RecordSerializer.serialize(firstRecord),
                RecordSerializer.serialize(secondRecord)
        );
    }

    @Test
    void shouldPreserveUpdatedData() {

        Record record = new Record(
                3L,
                new byte[]{1, 2}
        );

        record.updateData(
                new byte[]{10, 20, 30}
        );

        Record restoredRecord =
                roundTrip(record);

        assertArrayEquals(
                new byte[]{10, 20, 30},
                restoredRecord.getData()
        );

        assertEquals(
                3,
                restoredRecord.getDataLength()
        );
    }

    @Test
    void shouldRejectUpdatingDeletedRecord() {

        Record record = new Record(
                1L,
                new byte[]{1}
        );

        record.markAsDeleted();

        assertThrows(
                IllegalStateException.class,
                () -> record.updateData(
                        new byte[]{2}
                )
        );
    }

    private Record roundTrip(Record record) {

        byte[] serialized =
                RecordSerializer.serialize(record);

        return RecordSerializer.deserialize(
                serialized
        );
    }
}