package com.yekdb.storage.wal;

/**
 * Binary fiziksel WAL kayit turleri.
 */
public enum WalRecordType {

    PAGE_UPDATE((byte) 1);

    private final byte code;

    WalRecordType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
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
