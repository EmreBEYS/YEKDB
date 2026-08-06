package com.yekdb.storage.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageTest {

    @Test
    void shouldCreateDataPage() {
        Page page = new Page(
                1,
                PageType.DATA
        );

        assertEquals(
                1,
                page.getHeader().getPageId()
        );

        assertEquals(
                PageType.DATA,
                page.getHeader().getPageType()
        );

        assertEquals(
                Page.PAYLOAD_SIZE,
                page.getFreeSpace()
        );

        assertEquals(
                0,
                page.getHeader().getRecordCount()
        );
    }
}