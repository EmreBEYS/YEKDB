package com.yekdb.table;

/**
 * Diskten geri yüklenen tablo ve metadata bilgisini
 * birlikte taşır.
 *
 * Bu record TableFileMetadataReader tarafından
 * oluşturulur ve TableManager tarafından katalog
 * recovery işleminde kullanılır.
 *
 * Sürüm: 1.0
 */
public record TableRecoveryEntry(
        Table table,
        TableMetadata metadata
) {

    public TableRecoveryEntry {

        if (table == null) {
            throw new IllegalArgumentException("Table cannot be null.");
        }

        if (metadata == null) {
            throw new IllegalArgumentException("Table metadata cannot be null.");
        }

        if (!table.getTableName().equals(metadata.getTableName())) {

            throw new IllegalArgumentException("Table and metadata names must match.");
        }

        if (table.getColumnCount() != metadata.getColumnCount()) {

            throw new IllegalArgumentException("Table and metadata column counts must match.");
        }
    }
}