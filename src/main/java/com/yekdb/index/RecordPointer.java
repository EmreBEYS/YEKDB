package com.yekdb.index;

import java.io.Serializable;
import java.util.Objects;

/**
 * Bir kaydın fiziksel konumunu temsil eder.
 *
 * RecordPointer, bir kaydın veri dosyası içerisindeki
 * sayfa (Page) ve slot (Record) konumunu gösterir.
 *
 * Örnek:
 * Page 12
 * Slot 7
 */
public class RecordPointer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Kaydın bulunduğu sayfa numarası.
     */
    private int pageId;

    /**
     * Sayfa içerisindeki kayıt numarası.
     */
    private int slotId;

    /**
     * Varsayılan constructor.
     */
    public RecordPointer() {
    }

    /**
     * Parametreli constructor.
     *
     * @param pageId Sayfa numarası
     * @param slotId Slot numarası
     */
    public RecordPointer(int pageId, int slotId) {
        this.pageId = pageId;
        this.slotId = slotId;
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

    /**
     * Pointer'ın geçerli olup olmadığını kontrol eder.
     *
     * @return geçerliyse true
     */
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