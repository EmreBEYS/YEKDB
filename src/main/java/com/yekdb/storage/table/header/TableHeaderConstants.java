package com.yekdb.storage.table.header;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Binary Table Header disk formatında kullanılan sabitleri içerir.
 *
 * <pre>
 * Offset   Size      Alan
 * 0        4 byte    Magic Number
 * 4        2 byte    Format Version
 * 6        2 byte    Header Size
 * 8        8 byte    Table ID
 * 16       2 byte    Table Name Length
 * 18       255 byte  Table Name
 * 273      4 byte    Column Count
 * 277      8 byte    Row Count
 * 285      8 byte    First Data Page ID
 * 293      8 byte    Last Data Page ID
 * 301      8 byte    Schema Offset
 * 309      4 byte    Flags
 * 313      199 byte  Reserved
 * 512                Header End
 * </pre>
 */
public final class TableHeaderConstants {

    public static final int MAGIC_NUMBER = 0x59454B54;
    public static final short FORMAT_VERSION = 1;
    public static final int HEADER_SIZE = 512;
    public static final int MAX_TABLE_NAME_LENGTH = 255;
    public static final int FLAG_NONE = 0;
    public static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    public static final Charset TABLE_NAME_CHARSET = StandardCharsets.UTF_8;

    public static final int OFFSET_MAGIC_NUMBER = 0;
    public static final int OFFSET_FORMAT_VERSION = 4;
    public static final int OFFSET_HEADER_SIZE = 6;
    public static final int OFFSET_TABLE_ID = 8;
    public static final int OFFSET_TABLE_NAME_LENGTH = 16;
    public static final int OFFSET_TABLE_NAME = 18;
    public static final int OFFSET_COLUMN_COUNT = 273;
    public static final int OFFSET_ROW_COUNT = 277;
    public static final int OFFSET_FIRST_DATA_PAGE_ID = 285;
    public static final int OFFSET_LAST_DATA_PAGE_ID = 293;
    public static final int OFFSET_SCHEMA_OFFSET = 301;
    public static final int OFFSET_FLAGS = 309;
    public static final int OFFSET_RESERVED = 313;

    private TableHeaderConstants() {}
}
