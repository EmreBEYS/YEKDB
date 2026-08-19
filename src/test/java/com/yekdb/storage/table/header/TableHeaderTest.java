package com.yekdb.storage.table.header;

import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderTest {

    @Test
    void shouldCreateTableHeader() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                100L,
                5L,
                8L,
                512L,
                TableHeaderConstants.FLAG_NONE
        );

        assertEquals(1L, header.getTableId());
        assertEquals("users", header.getTableName());
        assertEquals(4, header.getColumnCount());
        assertEquals(100L, header.getRowCount());

        assertEquals(
                5L,
                header.getFirstDataPageId()
        );

        assertEquals(
                8L,
                header.getLastDataPageId()
        );

        assertEquals(
                512L,
                header.getSchemaOffset()
        );

        assertEquals(
                TableHeaderConstants.FLAG_NONE,
                header.getFlags()
        );
    }

    @Test
    void equalHeadersShouldBeEqual() {

        TableHeader first = new TableHeader(
                1L,
                "users",
                4,
                100L,
                5L,
                8L,
                512L,
                0
        );

        TableHeader second = new TableHeader(
                1L,
                "users",
                4,
                100L,
                5L,
                8L,
                512L,
                0
        );

        assertEquals(first, second);
        assertEquals(
                first.hashCode(),
                second.hashCode()
        );
    }
}