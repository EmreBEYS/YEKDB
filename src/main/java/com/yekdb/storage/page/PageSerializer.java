package com.yekdb.storage.page;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Page nesnelerini fiziksel disk formatına dönüştürür
 * ve disk formatından tekrar Page nesnesi oluşturur.
 */
public final class PageSerializer {

    private static final ByteOrder BYTE_ORDER =
            ByteOrder.BIG_ENDIAN;

    /**
     * Page nesnesini tam olarak 4096 byte uzunluğunda
     * fiziksel sayfa verisine dönüştürür.
     *
     * @param page dönüştürülecek sayfa
     * @return 4096 byte uzunluğunda fiziksel sayfa
     */
    public byte[] serialize(Page page) {

        Objects.requireNonNull(
                page,
                "Page cannot be null."
        );

        validatePage(page);

        PageHeader header = page.getHeader();

        ByteBuffer buffer = ByteBuffer
                .allocate(Page.PAGE_SIZE)
                .order(BYTE_ORDER);

        buffer.putInt(header.getPageId());
        buffer.putInt(header.getPageType().getCode());
        buffer.putInt(header.getRecordCount());
        buffer.putInt(header.getUsedBytes());
        buffer.putInt(header.getNextPageId());

        buffer.put(page.getPayload());

        return buffer.array();
    }

    /**
     * 4096 byte uzunluğundaki fiziksel sayfa verisini
     * Page nesnesine dönüştürür.
     *
     * @param bytes fiziksel sayfa verisi
     * @return yeniden oluşturulan Page nesnesi
     */
    public Page deserialize(byte[] bytes) {

        Objects.requireNonNull(
                bytes,
                "Page bytes cannot be null."
        );

        if (bytes.length != Page.PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Physical page must be exactly "
                            + Page.PAGE_SIZE
                            + " bytes."
            );
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(bytes)
                .order(BYTE_ORDER);

        int pageId = buffer.getInt();
        int pageTypeCode = buffer.getInt();
        int recordCount = buffer.getInt();
        int usedBytes = buffer.getInt();
        int nextPageId = buffer.getInt();

        PageType pageType =
                PageType.fromCode(pageTypeCode);

        Page page = new Page(
                pageId,
                pageType
        );

        PageHeader header = page.getHeader();

        header.setRecordCount(recordCount);
        header.setUsedBytes(usedBytes);
        header.setNextPageId(nextPageId);

        buffer.get(page.getPayload());

        validatePage(page);

        return page;
    }

    private void validatePage(Page page) {

        PageHeader header = Objects.requireNonNull(
                page.getHeader(),
                "Page header cannot be null."
        );

        byte[] payload = Objects.requireNonNull(
                page.getPayload(),
                "Page payload cannot be null."
        );

        if (payload.length != Page.PAYLOAD_SIZE) {
            throw new IllegalArgumentException(
                    "Page payload must be exactly "
                            + Page.PAYLOAD_SIZE
                            + " bytes."
            );
        }

        if (header.getUsedBytes() > payload.length) {
            throw new IllegalArgumentException(
                    "Used bytes exceed page payload capacity."
            );
        }

        if (header.getRecordCount() > 0
                && header.getUsedBytes() == 0) {

            throw new IllegalArgumentException(
                    "A page containing records must have used bytes."
            );
        }
    }
}