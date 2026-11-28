package com.yekdb.storage.wal;

/**
 * Binary fiziksel WAL kayit turleri.
 */
public enum WalRecordType {

    PAGE_UPDATE((byte) 1),
    TRANSACTION_BEGIN((byte) 2),
    TRANSACTION_COMMIT((byte) 3),
    TRANSACTION_ROLLBACK((byte) 4);

    private final byte code;

    WalRecordType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public boolean isTransactionBoundary() {
        return this != PAGE_UPDATE;
    }

    public static WalRecordType fromCode(byte code) {

        for (WalRecordType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown WAL record type: " + code
        );
    }
}
