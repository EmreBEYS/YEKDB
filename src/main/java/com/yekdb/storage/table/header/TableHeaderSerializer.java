package com.yekdb.storage.table.header;

import com.yekdb.storage.exception.CorruptedTableHeaderException;

import java.nio.ByteBuffer;

/**
 * {@link TableHeader} nesnelerini sabit boyutlu binary formata
 * serileştirir ve binary veriyi tekrar header modeline dönüştürür.
 */
public final class TableHeaderSerializer {

    private TableHeaderSerializer() {
    }

    public static byte[] serialize(TableHeader header) {

        TableHeaderValidator.validate(header);

        byte[] tableNameBytes =
                header.getTableName()
                        .getBytes(TableHeaderConstants.TABLE_NAME_CHARSET);

        ByteBuffer buffer =
                ByteBuffer.allocate(TableHeaderConstants.HEADER_SIZE)
                        .order(TableHeaderConstants.BYTE_ORDER);

        buffer.putInt(
                TableHeaderConstants.OFFSET_MAGIC_NUMBER,
                TableHeaderConstants.MAGIC_NUMBER
        );

        buffer.putShort(
                TableHeaderConstants.OFFSET_FORMAT_VERSION,
                TableHeaderConstants.FORMAT_VERSION
        );

        buffer.putShort(
                TableHeaderConstants.OFFSET_HEADER_SIZE,
                (short) TableHeaderConstants.HEADER_SIZE
        );

        buffer.putLong(
                TableHeaderConstants.OFFSET_TABLE_ID,
                header.getTableId()
        );

        buffer.putShort(
                TableHeaderConstants.OFFSET_TABLE_NAME_LENGTH,
                (short) tableNameBytes.length
        );

        buffer.position(
                TableHeaderConstants.OFFSET_TABLE_NAME
        );

        buffer.put(tableNameBytes);

        buffer.putInt(
                TableHeaderConstants.OFFSET_COLUMN_COUNT,
                header.getColumnCount()
        );

        buffer.putLong(
                TableHeaderConstants.OFFSET_ROW_COUNT,
                header.getRowCount()
        );

        buffer.putLong(
                TableHeaderConstants.OFFSET_FIRST_DATA_PAGE_ID,
                header.getFirstDataPageId()
        );

        buffer.putLong(
                TableHeaderConstants.OFFSET_LAST_DATA_PAGE_ID,
                header.getLastDataPageId()
        );

        buffer.putLong(
                TableHeaderConstants.OFFSET_SCHEMA_OFFSET,
                header.getSchemaOffset()
        );

        buffer.putInt(
                TableHeaderConstants.OFFSET_FLAGS,
                header.getFlags()
        );

        return buffer.array();
    }

    public static TableHeader deserialize(byte[] data) {

        validateBinaryData(data);

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(TableHeaderConstants.BYTE_ORDER);

        int magicNumber =
                buffer.getInt(
                        TableHeaderConstants.OFFSET_MAGIC_NUMBER
                );

        if (magicNumber != TableHeaderConstants.MAGIC_NUMBER) {
            throw new CorruptedTableHeaderException(
                    "Invalid table header magic number."
            );
        }

        short formatVersion =
                buffer.getShort(
                        TableHeaderConstants.OFFSET_FORMAT_VERSION
                );

        if (formatVersion != TableHeaderConstants.FORMAT_VERSION) {
            throw new CorruptedTableHeaderException(
                    "Unsupported table header format version: "
                            + formatVersion
            );
        }

        int headerSize =
                Short.toUnsignedInt(
                        buffer.getShort(
                                TableHeaderConstants.OFFSET_HEADER_SIZE
                        )
                );

        if (headerSize != TableHeaderConstants.HEADER_SIZE) {
            throw new CorruptedTableHeaderException(
                    "Invalid table header size: "
                            + headerSize
            );
        }

        long tableId =
                buffer.getLong(
                        TableHeaderConstants.OFFSET_TABLE_ID
                );

        int tableNameLength =
                Short.toUnsignedInt(
                        buffer.getShort(
                                TableHeaderConstants.OFFSET_TABLE_NAME_LENGTH
                        )
                );

        if (tableNameLength >
                TableHeaderConstants.MAX_TABLE_NAME_LENGTH) {

            throw new CorruptedTableHeaderException(
                    "Invalid table name length: "
                            + tableNameLength
            );
        }

        byte[] tableNameBytes =
                new byte[tableNameLength];

        buffer.position(
                TableHeaderConstants.OFFSET_TABLE_NAME
        );

        buffer.get(tableNameBytes);

        String tableName =
                new String(
                        tableNameBytes,
                        TableHeaderConstants.TABLE_NAME_CHARSET
                );

        int columnCount =
                buffer.getInt(
                        TableHeaderConstants.OFFSET_COLUMN_COUNT
                );

        long rowCount =
                buffer.getLong(
                        TableHeaderConstants.OFFSET_ROW_COUNT
                );

        long firstDataPageId =
                buffer.getLong(
                        TableHeaderConstants.OFFSET_FIRST_DATA_PAGE_ID
                );

        long lastDataPageId =
                buffer.getLong(
                        TableHeaderConstants.OFFSET_LAST_DATA_PAGE_ID
                );

        long schemaOffset =
                buffer.getLong(
                        TableHeaderConstants.OFFSET_SCHEMA_OFFSET
                );

        int flags =
                buffer.getInt(
                        TableHeaderConstants.OFFSET_FLAGS
                );

        TableHeader header =
                new TableHeader(
                        tableId,
                        tableName,
                        columnCount,
                        rowCount,
                        firstDataPageId,
                        lastDataPageId,
                        schemaOffset,
                        flags
                );

        try {
            TableHeaderValidator.validate(header);
        } catch (RuntimeException exception) {
            throw new CorruptedTableHeaderException(
                    "Binary table header contains invalid metadata.",
                    exception
            );
        }

        return header;
    }

    private static void validateBinaryData(byte[] data) {

        if (data == null) {
            throw new CorruptedTableHeaderException(
                    "Binary table header cannot be null."
            );
        }

        if (data.length != TableHeaderConstants.HEADER_SIZE) {
            throw new CorruptedTableHeaderException(
                    "Invalid binary table header length. Expected="
                            + TableHeaderConstants.HEADER_SIZE
                            + ", actual="
                            + data.length
            );
        }
    }
}