package com.yekdb.storage.page;

import java.util.Arrays;

public class Page {
    public static final int PAGE_SIZE=4096;

    private final PageHeader header;

    private final byte[] payload;

    public Page(
            int pageId,
            PageType pageType
    ){
      this.header=new PageHeader(pageId,pageType);
      this.payload=new byte[PAGE_SIZE];
    }
    public PageHeader getHeader() {
        return header;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void clear() {
        Arrays.fill(payload, (byte) 0);

        header.setRecordCount(0);
        header.setUsedBytes(0);
        header.setNextPageId(0);
    }

    public int getFreeSpace() {
        return PAGE_SIZE - header.getUsedBytes();
    }

    public boolean hasEnoughSpace(
            int requiredBytes
    ) {
        return getFreeSpace() >= requiredBytes;
    }
}
