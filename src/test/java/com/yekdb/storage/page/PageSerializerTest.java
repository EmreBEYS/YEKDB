package com.yekdb.storage.page;

import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageHeader;
import com.yekdb.storage.record.page.PageSerializer;
import com.yekdb.storage.record.page.PageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class PageSerializerTest {

    private PageSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new PageSerializer();
    }

    @Test
    void shouldSerializePageToExactly4096Bytes() {

        Page page = new Page(
                1,
                PageType.DATA
        );

        byte[] bytes = serializer.serialize(page);

        assertEquals(
                Page.PAGE_SIZE,
                bytes.length
        );
    }

    @Test
    void shouldSerializeAndDeserializePageHeader() {

        Page original = new Page(
                15,
                PageType.DATA
        );

        original.getHeader().setRecordCount(3);
        original.getHeader().setUsedBytes(120);
        original.getHeader().setNextPageId(16);

        byte[] bytes = serializer.serialize(original);

        Page restored = serializer.deserialize(bytes);

        assertEquals(
                original.getHeader().getPageId(),
                restored.getHeader().getPageId()
        );

        assertEquals(
                original.getHeader().getPageType(),
                restored.getHeader().getPageType()
        );

        assertEquals(
                original.getHeader().getRecordCount(),
                restored.getHeader().getRecordCount()
        );

        assertEquals(
                original.getHeader().getUsedBytes(),
                restored.getHeader().getUsedBytes()
        );

        assertEquals(
                original.getHeader().getNextPageId(),
                restored.getHeader().getNextPageId()
        );
    }

    @Test
    void shouldSerializeAndDeserializePayload() {

        Page original = new Page(
                4,
                PageType.DATA
        );

        byte[] expectedPayload = {
                10, 20, 30, 40, 50
        };

        System.arraycopy(
                expectedPayload,
                0,
                original.getPayload(),
                0,
                expectedPayload.length
        );

        original.getHeader().setRecordCount(1);
        original.getHeader().setUsedBytes(
                expectedPayload.length
        );

        byte[] bytes = serializer.serialize(original);

        Page restored = serializer.deserialize(bytes);

        for (int index = 0;
             index < expectedPayload.length;
             index++) {

            assertEquals(
                    expectedPayload[index],
                    restored.getPayload()[index]
            );
        }
    }

    @Test
    void shouldPreserveAllPageTypes() {

        for (PageType pageType : PageType.values()) {

            Page original = new Page(
                    pageType.getCode(),
                    pageType
            );

            byte[] bytes =
                    serializer.serialize(original);

            Page restored =
                    serializer.deserialize(bytes);

            assertEquals(
                    pageType,
                    restored.getHeader().getPageType()
            );
        }
    }

    @Test
    void shouldStoreHeaderAtBeginningOfPhysicalPage() {

        Page page = new Page(
                25,
                PageType.INDEX
        );

        page.getHeader().setRecordCount(7);
        page.getHeader().setUsedBytes(300);
        page.getHeader().setNextPageId(26);

        byte[] bytes = serializer.serialize(page);

        ByteBuffer buffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN);

        assertEquals(25, buffer.getInt());
        assertEquals(
                PageType.INDEX.getCode(),
                buffer.getInt()
        );
        assertEquals(7, buffer.getInt());
        assertEquals(300, buffer.getInt());
        assertEquals(26, buffer.getInt());
    }

    @Test
    void shouldPlacePayloadAfterTwentyByteHeader() {

        Page page = new Page(
                1,
                PageType.DATA
        );

        page.getPayload()[0] = 99;
        page.getHeader().setRecordCount(1);
        page.getHeader().setUsedBytes(1);

        byte[] bytes = serializer.serialize(page);

        assertEquals(
                99,
                bytes[PageHeader.HEADER_SIZE]
        );
    }

    @Test
    void shouldRejectNullPage() {

        assertThrows(
                NullPointerException.class,
                () -> serializer.serialize(null)
        );
    }

    @Test
    void shouldRejectNullByteArray() {

        assertThrows(
                NullPointerException.class,
                () -> serializer.deserialize(null)
        );
    }

    @Test
    void shouldRejectIncorrectPhysicalPageSize() {

        byte[] invalidBytes =
                new byte[Page.PAGE_SIZE - 1];

        assertThrows(
                IllegalArgumentException.class,
                () -> serializer.deserialize(invalidBytes)
        );
    }

    @Test
    void shouldRejectUnknownPageTypeCode() {

        byte[] bytes = new byte[Page.PAGE_SIZE];

        ByteBuffer.wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .putInt(999);

        assertThrows(
                IllegalArgumentException.class,
                () -> serializer.deserialize(bytes)
        );
    }

    @Test
    void shouldCreateEmptyPageWithFullFreeSpace() {

        Page page = new Page(
                1,
                PageType.DATA
        );

        assertEquals(
                Page.PAYLOAD_SIZE,
                page.getFreeSpace()
        );

        assertTrue(
                page.hasEnoughSpace(Page.PAYLOAD_SIZE)
        );

        assertFalse(
                page.hasEnoughSpace(Page.PAYLOAD_SIZE + 1)
        );
    }
}