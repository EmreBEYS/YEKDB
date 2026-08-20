package com.yekdb.storage.table.header;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderPageMetadataTest {

    private TableHeader createHeader(
            long firstPageId,
            long lastPageId
    ) {

        return new TableHeader(
                1L,
                "users",
                3,
                10L,
                firstPageId,
                lastPageId,
                TableHeaderConstants.HEADER_SIZE,
                TableHeaderConstants.FLAG_NONE
        );
    }

    @Test
    void shouldSetDataPageRange() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        TableHeader updated =
                TableHeaderUpdater.withDataPageRange(
                        header,
                        10L,
                        20L
                );

        assertEquals(
                10L,
                updated.getFirstDataPageId()
        );

        assertEquals(
                20L,
                updated.getLastDataPageId()
        );
    }

    @Test
    void shouldSetSinglePageRange() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        TableHeader updated =
                TableHeaderUpdater.withDataPageRange(
                        header,
                        7L,
                        7L
                );

        assertEquals(
                7L,
                updated.getFirstDataPageId()
        );

        assertEquals(
                7L,
                updated.getLastDataPageId()
        );
    }

    @Test
    void shouldClearDataPageRange() {

        TableHeader header =
                createHeader(
                        10L,
                        20L
                );

        TableHeader updated =
                TableHeaderUpdater.withDataPageRange(
                        header,
                        -1L,
                        -1L
                );

        assertEquals(
                -1L,
                updated.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                updated.getLastDataPageId()
        );
    }

    @Test
    void shouldUpdateFirstDataPageId() {

        TableHeader header =
                createHeader(
                        10L,
                        20L
                );

        TableHeader updated =
                TableHeaderUpdater.withFirstDataPageId(
                        header,
                        15L
                );

        assertEquals(
                15L,
                updated.getFirstDataPageId()
        );

        assertEquals(
                20L,
                updated.getLastDataPageId()
        );
    }

    @Test
    void shouldUpdateLastDataPageId() {

        TableHeader header =
                createHeader(
                        10L,
                        20L
                );

        TableHeader updated =
                TableHeaderUpdater.withLastDataPageId(
                        header,
                        25L
                );

        assertEquals(
                10L,
                updated.getFirstDataPageId()
        );

        assertEquals(
                25L,
                updated.getLastDataPageId()
        );
    }

    @Test
    void shouldPreserveRowCountWhenUpdatingPages() {

        TableHeader header =
                createHeader(
                        10L,
                        20L
                );

        TableHeader updated =
                TableHeaderUpdater.withDataPageRange(
                        header,
                        30L,
                        40L
                );

        assertEquals(
                10L,
                updated.getRowCount()
        );
    }

    @Test
    void shouldNotModifyOriginalHeader() {

        TableHeader header =
                createHeader(
                        10L,
                        20L
                );

        TableHeaderUpdater.withDataPageRange(
                header,
                30L,
                40L
        );

        assertEquals(
                10L,
                header.getFirstDataPageId()
        );

        assertEquals(
                20L,
                header.getLastDataPageId()
        );
    }

    @Test
    void shouldRejectOnlyFirstPageId() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .withDataPageRange(
                                header,
                                10L,
                                -1L
                        )
        );
    }

    @Test
    void shouldRejectOnlyLastPageId() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .withDataPageRange(
                                header,
                                -1L,
                                10L
                        )
        );
    }

    @Test
    void shouldRejectFirstPageGreaterThanLastPage() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .withDataPageRange(
                                header,
                                20L,
                                10L
                        )
        );
    }

    @Test
    void shouldRejectInvalidNegativePageId() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .withDataPageRange(
                                header,
                                -2L,
                                -2L
                        )
        );
    }

    @Test
    void shouldRejectSettingOnlyFirstPageOnEmptyHeader() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .withFirstDataPageId(
                                header,
                                10L
                        )
        );
    }

    @Test
    void shouldRejectSettingOnlyLastPageOnEmptyHeader() {

        TableHeader header =
                createHeader(
                        -1L,
                        -1L
                );

        assertThrows(
                TableHeaderUpdateException.class,
                () -> TableHeaderUpdater
                        .withLastDataPageId(
                                header,
                                10L
                        )
        );
    }

    @Test
    void shouldRejectNullHeader() {

        assertThrows(
                NullPointerException.class,
                () -> TableHeaderUpdater
                        .withDataPageRange(
                                null,
                                1L,
                                1L
                        )
        );
    }
}