package com.yekdb.storage.record.page;

import java.util.Objects;

/**
 * Bir YEKDB sayfasının fiziksel başlık bilgisini temsil eder.
 *
 * Disk düzeni:
 *
 * Offset  Size  Field
 * ------  ----  ----------------
 * 0       4     Page ID
 * 4       4     Page type
 * 8       4     Record count
 * 12      4     Used bytes
 * 16      4     Next page ID
 *
 * Toplam: 20 byte
 */
public final class PageHeader {

    public static final int HEADER_SIZE = 20;

    public static final int NO_NEXT_PAGE = -1;

    private final int pageId;

    private final PageType pageType;

    private int recordCount;

    private int usedBytes;

    private int nextPageId;

    public PageHeader(
            int pageId,
            PageType pageType
    ) {
        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative."
            );
        }

        this.pageId = pageId;
        this.pageType = Objects.requireNonNull(
                pageType,
                "Page type cannot be null."
        );

        this.recordCount = 0;
        this.usedBytes = 0;
        this.nextPageId = NO_NEXT_PAGE;
    }

    public int getPageId() {
        return pageId;
    }

    public PageType getPageType() {
        return pageType;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {

        if (recordCount < 0) {
            throw new IllegalArgumentException(
                    "Record count cannot be negative."
            );
        }

        this.recordCount = recordCount;
    }

    public int getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(int usedBytes) {

        if (usedBytes < 0) {
            throw new IllegalArgumentException(
                    "Used bytes cannot be negative."
            );
        }

        if (usedBytes > Page.PAYLOAD_SIZE) {
            throw new IllegalArgumentException(
                    "Used bytes cannot exceed page payload capacity."
            );
        }

        this.usedBytes = usedBytes;
    }

    public int getNextPageId() {
        return nextPageId;
    }

    public void setNextPageId(int nextPageId) {

        if (nextPageId < NO_NEXT_PAGE) {
            throw new IllegalArgumentException(
                    "Next page ID cannot be less than -1."
            );
        }

        this.nextPageId = nextPageId;
    }
}