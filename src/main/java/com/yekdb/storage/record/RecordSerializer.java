package com.yekdb.storage.record;

import java.nio.ByteBuffer;

/**
 * Record nesnelerini byte dizisine,
 * byte dizilerini tekrar Record nesnesine dönüştürür.
 *
 * Disk formatı:
 *
 * [Record ID: 8 byte]
 * [Deleted Flag: 1 byte]
 * [Data Length: 4 byte]
 * [Data: N byte]
 */
public final class RecordSerializer {

    private static final int RECORD_ID_SIZE = Long.BYTES;
    private static final int DELETED_FLAG_SIZE = Byte.BYTES;
    private static final int DATA_LENGTH_SIZE = Integer.BYTES;

    public static final int HEADER_SIZE =
            RECORD_ID_SIZE +
                    DELETED_FLAG_SIZE +
                    DATA_LENGTH_SIZE;

    private RecordSerializer() {
        // Utility class olduğu için nesne oluşturulamaz.
    }

    public static byte[] serialize(Record record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Serialize edilecek Record null olamaz."
            );
        }

        byte[] data = record.getData();

        ByteBuffer buffer = ByteBuffer.allocate(
                HEADER_SIZE + data.length
        );

        buffer.putLong(record.getRecordId());
        buffer.put(record.isDeleted() ? (byte) 1 : (byte) 0);
        buffer.putInt(data.length);
        buffer.put(data);

        return buffer.array();
    }

    public static Record deserialize(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException(
                    "Deserialize edilecek byte dizisi null olamaz."
            );
        }

        if (bytes.length < HEADER_SIZE) {
            throw new IllegalArgumentException(
                    "Byte dizisi geçerli bir Record için çok küçük."
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        long recordId = buffer.getLong();
        byte deletedFlag = buffer.get();
        int dataLength = buffer.getInt();

        if (deletedFlag != 0 && deletedFlag != 1) {
            throw new IllegalArgumentException(
                    "Geçersiz silinme bayrağı: " + deletedFlag
            );
        }

        if (dataLength < 0) {
            throw new IllegalArgumentException(
                    "Kayıt veri uzunluğu negatif olamaz."
            );
        }

        if (dataLength != buffer.remaining()) {
            throw new IllegalArgumentException(
                    "Kayıt veri uzunluğu ile byte dizisi uyuşmuyor."
            );
        }

        byte[] data = new byte[dataLength];
        buffer.get(data);

        Record record = new Record(recordId, data);

        if (deletedFlag == 1) {
            record.markAsDeleted();
        }

        return record;
    }

    public static int calculateSerializedSize(Record record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Record null olamaz."
            );
        }

        return HEADER_SIZE + record.getDataLength();
    }
}