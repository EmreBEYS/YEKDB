package com.yekdb.storage.record.page;

import java.util.Arrays;

/**
 * YEKDB içerisinde kullanılan sabit boyutlu fiziksel sayfayı temsil eder.
 */
public final class Page {

    /**
     * Bir fiziksel sayfanın toplam disk boyutu.
     */
    public static final int PAGE_SIZE = 4096;

    /**
     * Header haricinde kayıtların saklanabileceği alan.
     */
    public static final int PAYLOAD_SIZE =
            PAGE_SIZE - PageHeader.HEADER_SIZE;

    private final PageHeader header;

    private final byte[] payload;

    public Page(
            int pageId,
            PageType pageType
    ) {
        this.header = new PageHeader(
                pageId,
                pageType
        );

        this.payload = new byte[PAYLOAD_SIZE];
    }

    public PageHeader getHeader() {
        return header;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Sayfanın payload alanını ve değişken header bilgilerini sıfırlar.
     */
    public void clear() {

        Arrays.fill(payload, (byte) 0);

        header.setRecordCount(0);
        header.setUsedBytes(0);
        header.setNextPageId(
                PageHeader.NO_NEXT_PAGE
        );
    }

    /**
     * Payload içerisinde kalan kullanılabilir byte miktarı.
     */
    public int getFreeSpace() {
        return PAYLOAD_SIZE - header.getUsedBytes();
    }

    public boolean hasEnoughSpace(int requiredBytes) {

        if (requiredBytes < 0) {
            throw new IllegalArgumentException(
                    "Required bytes cannot be negative."
            );
        }

        return getFreeSpace() >= requiredBytes;
    }
}