package com.yekdb.storage.wal;

import java.nio.ByteBuffer;

/**
 * Fiziksel WAL kayitlarini platformdan bagimsiz binary formata cevirir.
 */
public final class WalWriter {

    static final int RECORD_MAGIC = 0x57414C52;

    static final short RECORD_VERSION = 1;

    static final int FIXED_RECORD_SIZE = 44;

    public byte[] serialize(WalRecord record) {

        byte[] beforeImage = record.getBeforeImage();
        byte[] afterImage = record.getAfterImage();

        int totalLength = Math.addExact(
                FIXED_RECORD_SIZE,
                Math.addExact(
                        beforeImage.length,
                        afterImage.length
                )
        );

        ByteBuffer buffer = ByteBuffer.allocate(
                totalLength
        );

        buffer.putInt(RECORD_MAGIC);
        buffer.putInt(totalLength);
        buffer.putShort(RECORD_VERSION);
        buffer.put(record.getType().getCode());
        buffer.put((byte) 0);
        buffer.putLong(record.getLsn());
        buffer.putLong(record.getTransactionId());
        buffer.putInt(record.getPageId());
        buffer.putInt(beforeImage.length);
        buffer.putInt(afterImage.length);
        buffer.put(beforeImage);
        buffer.put(afterImage);

        byte[] bytes = buffer.array();
        int checksum = WalChecksum.crc32(
                bytes,
                Integer.BYTES * 2,
                totalLength - Integer.BYTES * 3
        );

        buffer.putInt(checksum);

        return bytes;
    }
}
