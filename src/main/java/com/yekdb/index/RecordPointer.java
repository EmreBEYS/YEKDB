package com.yekdb.index;

import com.yekdb.storage.record.RecordId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Bir kaydın fiziksel konumunu temsil eden legacy index pointer tipidir.
 *
 * <p>Storage katmanındaki canonical fiziksel adres tipi artık
 * {@link RecordId}'dir. RecordPointer mevcut index API'leri ve serialized
 * verilerle geriye dönük uyumluluk için korunur ve RecordId ile çift yönlü
 * dönüşüm sağlar.</p>
 */
public class RecordPointer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageId;
    private int slotId;

    public RecordPointer() {
    }

    public RecordPointer(int pageId, int slotId) {
        this.pageId = pageId;
        this.slotId = slotId;
    }

    /**
     * Canonical RecordId üzerinden compatibility pointer oluşturur.
     */
    public RecordPointer(RecordId recordId) {
        Objects.requireNonNull(
                recordId,
                "RecordId cannot be null."
        );
        this.pageId = recordId.pageId();
        this.slotId = recordId.slotId();
    }

    public static RecordPointer fromRecordId(RecordId recordId) {
        return new RecordPointer(recordId);
    }

    /**
     * Bu pointer'ı storage katmanının canonical RecordId tipine dönüştürür.
     */
    public RecordId toRecordId() {
        if (!isValid()) {
            throw new IllegalStateException(
                    "Cannot convert invalid RecordPointer to RecordId: " + this
            );
        }
        return new RecordId(pageId, slotId);
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public boolean isValid() {
        return pageId >= 0 && slotId >= 0;
    }

    @Override
    public String toString() {
        return "RecordPointer{" +
                "pageId=" + pageId +
                ", slotId=" + slotId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecordPointer that)) return false;
        return pageId == that.pageId &&
                slotId == that.slotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, slotId);
    }
}
