package com.yekdb.storage.table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * YEKDB tablosuna ait metadata bilgilerini temsil eder.
 *
 * Metadata içerisinde:
 * - tablo adı,
 * - sütun sayısı,
 * - oluşturulma zamanı,
 * - fiziksel dosya adı,
 * - metadata sürümü
 *
 * saklanır.
 *
 * Sürüm: 1.0
 */
public class TableMetadata {

    private static final int CURRENT_VERSION = 1;

    private final String tableName;
    private final int columnCount;
    private final LocalDateTime createdAt;
    private final String fileName;
    private final int version;

    /**
     * Yeni tablo metadata bilgisi oluşturur.
     *
     * @param tableName   tablo adı
     * @param columnCount sütun sayısı
     */
    public TableMetadata(
            String tableName,
            int columnCount
    ) {

        this(
                TableNameValidator.validate(tableName),
                columnCount,
                LocalDateTime.now(),
                TableNameValidator.validate(tableName)
                        + ".tbl",
                CURRENT_VERSION
        );
    }

    /**
     * Mevcut metadata bilgilerinden nesne oluşturur.
     *
     * Bu constructor ileride metadata dosyasından
     * tablo bilgileri okunurken kullanılabilir.
     *
     * @param tableName   tablo adı
     * @param columnCount sütun sayısı
     * @param createdAt   oluşturulma zamanı
     * @param fileName    fiziksel dosya adı
     * @param version     metadata sürümü
     */
    public TableMetadata(
            String tableName,
            int columnCount,
            LocalDateTime createdAt,
            String fileName,
            int version
    ) {

        this.tableName =
                TableNameValidator.validate(tableName);

        if (columnCount <= 0) {
            throw new IllegalArgumentException(
                    "Column count must be greater than zero."
            );
        }

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time cannot be null."
        );

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table file name cannot be null or blank."
            );
        }

        if (version <= 0) {
            throw new IllegalArgumentException(
                    "Metadata version must be greater than zero."
            );
        }

        this.columnCount = columnCount;
        this.fileName = fileName.trim();
        this.version = version;
    }

    public String getTableName() {
        return tableName;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFileName() {
        return fileName;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "TableMetadata{" +
                "tableName='" + tableName + '\'' +
                ", columnCount=" + columnCount +
                ", createdAt=" + createdAt +
                ", fileName='" + fileName + '\'' +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TableMetadata that)) {
            return false;
        }

        return columnCount == that.columnCount
                && version == that.version
                && tableName.equals(that.tableName)
                && createdAt.equals(that.createdAt)
                && fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tableName,
                columnCount,
                createdAt,
                fileName,
                version
        );
    }
}