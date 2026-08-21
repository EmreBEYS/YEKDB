package com.yekdb.storage.record;

import com.yekdb.storage.record.page.Page;

import java.util.Objects;

/**
 * Bir Record nesnesinin fiziksel sayfa içerisindeki
 * mevcut konum bilgisini temsil eder.
 *
 * RecordLocation henüz kalıcı disk formatının bir parçası değildir.
 * RecordManager tarafından sayfa taraması sırasında oluşturulur.
 *
 * İleride slotted-page yapısına geçildiğinde offset ve serializedSize
 * bilgileri slot directory üzerinden elde edilebilir.
 */
public record RecordLocation(
        Page page,
        int offset,
        int serializedSize,
        int slotId,
        Record record
) {

    public RecordLocation {

        Objects.requireNonNull(
                page,
                "Page cannot be null."
        );

        Objects.requireNonNull(
                record,
                "Record cannot be null."
        );

        if (offset < 0) {
            throw new IllegalArgumentException(
                    "Record offset cannot be negative: " +
                            offset
            );
        }

        if (serializedSize <= 0) {
            throw new IllegalArgumentException(
                    "Serialized record size must be positive: " +
                            serializedSize
            );
        }

        if (slotId < 0) {
            throw new IllegalArgumentException(
                    "Slot ID cannot be negative: " +
                            slotId
            );
        }

        long endOffset =
                (long) offset + serializedSize;

        if (endOffset
                > page.getHeader().getUsedBytes()) {

            throw new IllegalArgumentException(
                    "Record location exceeds page used bytes."
            );
        }
    }

    /**
     * Kaydın fiziksel kimliğini döndürür.
     */
    public RecordId getRecordId() {
        return new RecordId(
                page.getHeader().getPageId(),
                slotId
        );
    }

    /**
     * Kaydın bulunduğu fiziksel sayfa kimliğini döndürür.
     */
    public int getPageId() {
        return page.getHeader().getPageId();
    }

    /**
     * Kaydın payload içerisindeki son byte offsetini döndürür.
     */
    public int getEndOffset() {
        return offset + serializedSize;
    }
}