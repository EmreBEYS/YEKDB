package com.yekdb.storage.record;

import java.io.Serializable;

/**
 * Bir kaydın fiziksel sayfa içerisindeki kimliğini temsil eder.
 *
 * <p>RecordId, fiziksel kayıt adresini {@code pageId + slotId}
 * çiftiyle ifade eder. Bu kimlik storage ve index katmanları arasında
 * ortak fiziksel adres tipi olarak kullanılabilir.</p>
 *
 * @param pageId kaydın bulunduğu fiziksel sayfa kimliği
 * @param slotId sayfa içerisindeki mantıksal slot kimliği
 */
public record RecordId(
        int pageId,
        int slotId
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public RecordId {

        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative: " + pageId
            );
        }

        if (slotId < 0) {
            throw new IllegalArgumentException(
                    "Slot ID cannot be negative: " + slotId
            );
        }
    }
}
