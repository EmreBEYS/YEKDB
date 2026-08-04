package com.yekdb.query.command;

import java.util.Objects;

/**
 * DELETE SQL komutunu temsil eder.
 *
 * <p>Sprint 00-10 kapsamında kayıt silme işlemi
 * Record ID üzerinden gerçekleştirilir.</p>
 */
public final class DeleteCommand implements Command {

    /**
     * Kaydın silineceği tablo adı.
     */
    private final String tableName;

    /**
     * Silinecek kaydın benzersiz kimliği.
     */
    private final long recordId;

    /**
     * Yeni DELETE komutu oluşturur.
     *
     * @param tableName hedef tablo adı
     * @param recordId  silinecek kayıt kimliği
     */
    public DeleteCommand(
            String tableName,
            long recordId
    ) {
        this.tableName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (this.tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        if (recordId < 0) {
            throw new IllegalArgumentException(
                    "Record ID cannot be negative: " + recordId
            );
        }

        this.recordId = recordId;
    }

    /**
     * Hedef tablo adını döndürür.
     *
     * @return tablo adı
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Silinecek kayıt kimliğini döndürür.
     *
     * @return kayıt kimliği
     */
    public long getRecordId() {
        return recordId;
    }

    @Override
    public String toString() {
        return "DeleteCommand{" +
                "tableName='" + tableName + '\'' +
                ", recordId=" + recordId +
                '}';
    }
}