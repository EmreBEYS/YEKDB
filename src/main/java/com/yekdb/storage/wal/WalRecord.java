package com.yekdb.storage.wal;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bir fiziksel sayfa degisikliginin binary WAL kaydi.
 */
public final class WalRecord {

    private final long lsn;

    private final long transactionId;

    private final int pageId;

    private final WalRecordType type;

    private final byte[] beforeImage;

    private final byte[] afterImage;

    public WalRecord(
            long lsn,
            long transactionId,
            int pageId,
            WalRecordType type,
            byte[] beforeImage,
            byte[] afterImage
    ) {

        if (lsn <= 0) {
            throw new IllegalArgumentException(
                    "LSN must be positive."
            );
        }

        if (transactionId < 0) {
            throw new IllegalArgumentException(
                    "Transaction ID cannot be negative."
            );
        }

        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative."
            );
        }

        this.lsn = lsn;
        this.transactionId = transactionId;
        this.pageId = pageId;
        this.type = Objects.requireNonNull(
                type,
                "WAL record type cannot be null."
        );
        this.beforeImage = copyImage(
                beforeImage,
                "Before image cannot be null."
        );
        this.afterImage = copyImage(
                afterImage,
                "After image cannot be null."
        );
    }

    public long getLsn() {
        return lsn;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public int getPageId() {
        return pageId;
    }

    public WalRecordType getType() {
        return type;
    }

    public byte[] getBeforeImage() {
        return Arrays.copyOf(
                beforeImage,
                beforeImage.length
        );
    }

    public byte[] getAfterImage() {
        return Arrays.copyOf(
                afterImage,
                afterImage.length
        );
    }

    int getBeforeImageLength() {
        return beforeImage.length;
    }

    int getAfterImageLength() {
        return afterImage.length;
    }

    private static byte[] copyImage(
            byte[] image,
            String nullMessage
    ) {

        Objects.requireNonNull(
                image,
                nullMessage
        );

        return Arrays.copyOf(
                image,
                image.length
        );
    }
}
