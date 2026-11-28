package com.yekdb.storage.recovery;

import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageType;

final class CrashRecoveryTestPages {

    private CrashRecoveryTestPages() {
    }

    static Page page(byte marker) {
        Page page = new Page(0, PageType.DATA);
        page.getPayload()[0] = marker;
        page.getHeader().setUsedBytes(1);
        page.getHeader().setRecordCount(1);
        return page;
    }
}
