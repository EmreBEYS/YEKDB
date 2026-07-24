package com.yekdb.storage.page;

public final class PageHeader {

    private final int pageId;

    private final PageType pageType;

    private int recordCount;

    private int usedBytes;

    private int nextPageId;

    public PageHeader(
            int pageId,
            PageType pageType
    ){
        this.pageId=pageId;
        this.pageType=pageType;
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
        this.recordCount = recordCount;
    }

    public int getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(int usedBytes) {
        this.usedBytes = usedBytes;
    }

    public int getNextPageId() {
        return nextPageId;
    }

    public void setNextPageId(int nextPageId) {
        this.nextPageId = nextPageId;
    }

}
