package com.yekdb.storage.table.header;

import com.yekdb.storage.exception.CorruptedTableHeaderException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderSerializerTest {

    @Test
    void shouldSerializeHeaderToFixedSizeBinaryData() {

        TableHeader header = createHeader();

        byte[] data =
                TableHeaderSerializer.serialize(header);

        assertNotNull(data);

        assertEquals(
                TableHeaderConstants.HEADER_SIZE,
                data.length
        );
    }

    @Test
    void shouldPreserveMagicNumber() {

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(TableHeaderConstants.BYTE_ORDER);

        assertEquals(
                TableHeaderConstants.MAGIC_NUMBER,
                buffer.getInt(
                        TableHeaderConstants.OFFSET_MAGIC_NUMBER
                )
        );
    }

    @Test
    void shouldRoundTripTableHeader() {

        TableHeader original = createHeader();

        byte[] data =
                TableHeaderSerializer.serialize(original);

        TableHeader restored =
                TableHeaderSerializer.deserialize(data);

        assertEquals(original, restored);
    }

    @Test
    void shouldRoundTripEmptyTable() {

        TableHeader original =
                new TableHeader(
                        2L,
                        "empty_table",
                        3,
                        0L,
                        -1L,
                        -1L,
                        512L,
                        0
                );

        TableHeader restored =
                TableHeaderSerializer.deserialize(
                        TableHeaderSerializer.serialize(original)
                );

        assertEquals(original, restored);
    }

    @Test
    void shouldSupportUtf8TableName() {

        TableHeader original =
                new TableHeader(
                        3L,
                        "müşteriler",
                        5,
                        15L,
                        10L,
                        12L,
                        1024L,
                        0
                );

        TableHeader restored =
                TableHeaderSerializer.deserialize(
                        TableHeaderSerializer.serialize(original)
                );

        assertEquals(
                "müşteriler",
                restored.getTableName()
        );
    }

    @Test
    void shouldRejectNullBinaryData() {

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderSerializer.deserialize(null)
        );
    }

    @Test
    void shouldRejectWrongBinaryLength() {

        byte[] invalid =
                new byte[100];

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderSerializer.deserialize(invalid)
        );
    }

    @Test
    void shouldRejectInvalidMagicNumber() {

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(TableHeaderConstants.BYTE_ORDER);

        buffer.putInt(
                TableHeaderConstants.OFFSET_MAGIC_NUMBER,
                0x00000000
        );

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderSerializer.deserialize(data)
        );
    }

    @Test
    void shouldRejectUnsupportedVersion() {

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(TableHeaderConstants.BYTE_ORDER);

        buffer.putShort(
                TableHeaderConstants.OFFSET_FORMAT_VERSION,
                (short) 99
        );

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderSerializer.deserialize(data)
        );
    }

    @Test
    void shouldRejectInvalidHeaderSize() {

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(TableHeaderConstants.BYTE_ORDER);

        buffer.putShort(
                TableHeaderConstants.OFFSET_HEADER_SIZE,
                (short) 128
        );

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderSerializer.deserialize(data)
        );
    }

    @Test
    void shouldRejectInvalidTableNameLength() {

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(TableHeaderConstants.BYTE_ORDER);

        buffer.putShort(
                TableHeaderConstants.OFFSET_TABLE_NAME_LENGTH,
                (short) 300
        );

        assertThrows(
                CorruptedTableHeaderException.class,
                () -> TableHeaderSerializer.deserialize(data)
        );
    }

    @Test
    void reservedAreaShouldRemainZero() {

        byte[] data =
                TableHeaderSerializer.serialize(
                        createHeader()
                );

        for (
                int i = TableHeaderConstants.OFFSET_RESERVED;
                i < TableHeaderConstants.HEADER_SIZE;
                i++
        ) {
            assertEquals(
                    0,
                    data[i],
                    "Reserved byte at offset "
                            + i
                            + " should be zero."
            );
        }
    }

    private TableHeader createHeader() {

        return new TableHeader(
                1L,
                "users",
                4,
                100L,
                5L,
                8L,
                512L,
                TableHeaderConstants.FLAG_NONE
        );
    }
}