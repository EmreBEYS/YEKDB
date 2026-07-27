package com.yekdb.storage.record;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RecordSerializerTest {

    @Test
    void shouldSerializeAndDeserializeRecord() {
        Record originalRecord = new Record(
                10L,
                "YEKDB Serializer".getBytes(StandardCharsets.UTF_8)
        );

        byte[] serialized =
                RecordSerializer.serialize(originalRecord);

        Record deserialized =
                RecordSerializer.deserialize(serialized);

        assertEquals(
                originalRecord.getRecordId(),
                deserialized.getRecordId()
        );

        assertArrayEquals(
                originalRecord.getData(),
                deserialized.getData()
        );

        assertEquals(
                originalRecord.isDeleted(),
                deserialized.isDeleted()
        );
    }

    @Test
    void shouldPreserveDeletedState() {
        Record originalRecord = new Record(
                5L,
                "Silinecek kayıt".getBytes(StandardCharsets.UTF_8)
        );

        originalRecord.markAsDeleted();

        byte[] serialized =
                RecordSerializer.serialize(originalRecord);

        Record deserialized =
                RecordSerializer.deserialize(serialized);

        assertTrue(deserialized.isDeleted());
    }

    @Test
    void shouldCalculateSerializedSize() {
        Record record = new Record(
                1L,
                "YEKDB".getBytes(StandardCharsets.UTF_8)
        );

        int expectedSize =
                RecordSerializer.HEADER_SIZE
                        + record.getDataLength();

        assertEquals(
                expectedSize,
                RecordSerializer.calculateSerializedSize(record)
        );

        assertEquals(
                expectedSize,
                RecordSerializer.serialize(record).length
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
    void shouldRejectNullByteArrayDuringDeserialization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(null)
        );
    }

    @Test
    void shouldRejectByteArraySmallerThanHeader() {
        byte[] invalidBytes =
                new byte[RecordSerializer.HEADER_SIZE - 1];

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(invalidBytes)
        );
    }

    @Test
    void shouldRejectInvalidDeletedFlag() {
        ByteBuffer buffer = ByteBuffer.allocate(
                RecordSerializer.HEADER_SIZE
        );

        buffer.putLong(1L);
        buffer.put((byte) 7);
        buffer.putInt(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(buffer.array())
        );
    }

    @Test
    void shouldRejectMismatchedDataLength() {
        ByteBuffer buffer = ByteBuffer.allocate(
                RecordSerializer.HEADER_SIZE + 3
        );

        buffer.putLong(1L);
        buffer.put((byte) 0);
        buffer.putInt(10);
        buffer.put(new byte[3]);

        assertThrows(
                IllegalArgumentException.class,
                () -> RecordSerializer.deserialize(buffer.array())
        );
    }

    @Test
    void shouldSupportEmptyRecordData() {
        Record originalRecord = new Record(1L, new byte[0]);

        byte[] serialized =
                RecordSerializer.serialize(originalRecord);

        Record deserialized =
                RecordSerializer.deserialize(serialized);

        assertEquals(
                RecordSerializer.HEADER_SIZE,
                serialized.length
        );

        assertEquals(0, deserialized.getDataLength());
        assertArrayEquals(new byte[0], deserialized.getData());
    }
}