package com.yekdb.storage.record;

import java.util.Arrays;
import java.util.Objects;

/**
 * YEKDB içerisinde saklanacak tek bir kaydı temsil eder.
 *
 * Record nesnesi:
 * - Kaydın benzersiz kimliğini
 * - Kaydın ham byte verisini
 * - Silinme durumunu
 *
 * saklar.
 */
public class Record {

    private final long recordId;
    private byte[] data;
    private boolean deleted;

    public Record(long recordId, byte[] data) {
        if (recordId < 0) {
            throw new IllegalArgumentException(
                    "Record ID negatif olamaz: " + recordId
            );
        }

        if (data == null) {
            throw new IllegalArgumentException(
                    "Record verisi null olamaz."
            );
        }

        this.recordId = recordId;
        this.data = Arrays.copyOf(data, data.length);
        this.deleted = false;
    }

    public long getRecordId() {
        return recordId;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public int getDataLength() {
        return data.length;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void updateData(byte[] newData) {
        if (deleted) {
            throw new IllegalStateException(
                    "Silinmiş bir kayıt güncellenemez."
            );
        }

        if (newData == null) {
            throw new IllegalArgumentException(
                    "Yeni kayıt verisi null olamaz."
            );
        }

        this.data = Arrays.copyOf(newData, newData.length);
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Record record)) {
            return false;
        }

        return recordId == record.recordId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }

    @Override
    public String toString() {
        return "Record{" +
                "recordId=" + recordId +
                ", dataLength=" + data.length +
                ", deleted=" + deleted +
                '}';
    }
}