package com.yekdb.storage.table.header;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderUpdaterTest {

    private TableHeader createHeader(
            long rowCount
    ) {

        return new TableHeader(
                1L,
                "users",
                3,
                rowCount,
                -1L,
                -1L,
                TableHeaderConstants.HEADER_SIZE,
                TableHeaderConstants.FLAG_NONE
        );
    }

    @Test
    void shouldUpdateRowCount() {

        TableHeader header =
                createHeader(10L);

        TableHeader updated =
                TableHeaderUpdater.withRowCount(
                        header,
                        25L
                );

        assertEquals(
                25L,
                updated.getRowCount()
        );
    }

    @Test
    void shouldIncrementRowCount() {

        TableHeader header =
                createHeader(10L);

        TableHeader updated =
                TableHeaderUpdater.incrementRowCount(
                        header
                );

        assertEquals(
                11L,
                updated.getRowCount()
        );
    }

    @Test
    void shouldDecrementRowCount() {

        TableHeader header =
                createHeader(10L);

        TableHeader updated =
                TableHeaderUpdater.decrementRowCount(
                        header
                );

        assertEquals(
                9L,
                updated.getRowCount()
        );
    }

    @Test
    void shouldNotModifyOriginalHeader() {

        TableHeader header =
                createHeader(10L);

        TableHeaderUpdater.incrementRowCount(
                header
        );

        assertEquals(
                10L,
                header.getRowCount()
        );
    }

    @Test
    void shouldPreserveOtherHeaderFields() {

        TableHeader header =
                createHeader(10L);

        TableHeader updated =
                TableHeaderUpdater.withRowCount(
                        header,
                        20L
                );

        assertEquals(
                header.getTableId(),
                updated.getTableId()
        );

        assertEquals(
                header.getTableName(),
                updated.getTableName()
        );

        assertEquals(
                header.getColumnCount(),
                updated.getColumnCount()
        );

        assertEquals(
                header.getFirstDataPageId(),
                updated.getFirstDataPageId()
        );

        assertEquals(
                header.getLastDataPageId(),
                updated.getLastDataPageId()
        );

        assertEquals(
                header.getSchemaOffset(),
                updated.getSchemaOffset()
        );

        assertEquals(
                header.getFlags(),
                updated.getFlags()
        );
    }

    @Test
    void shouldRejectNegativeRowCount() {

        TableHeader header =
                createHeader(10L);

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater.withRowCount(
                        header,
                        -1L
                )
        );
    }

    @Test
    void shouldRejectDecrementBelowZero() {

        TableHeader header =
                createHeader(0L);

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater.decrementRowCount(
                        header
                )
        );
    }

    @Test
    void shouldRejectRowCountOverflow() {

        TableHeader header =
                createHeader(
                        Long.MAX_VALUE
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater.incrementRowCount(
                        header
                )
        );
    }

    @Test
    void shouldRejectNullHeader() {

        assertThrows(
                NullPointerException.class,
                () -> TableHeaderUpdater.incrementRowCount(
                        null
                )
        );
    }
}