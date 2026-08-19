package com.yekdb.storage.table;

import java.util.Locale;

/**
 * YEKDB tablo adlarının doğrulanması ve normalize edilmesinden
 * sorumlu yardımcı sınıftır.
 *
 * Geçerli tablo adları:
 * - Bir harf veya alt çizgi (_) ile başlamalıdır.
 * - Devamında harf, rakam veya alt çizgi kullanılabilir.
 *
 * Sürüm: 1.0
 */
final class TableNameValidator {

    private static final String TABLE_NAME_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    private TableNameValidator() {
        // Utility sınıfı olduğu için nesnesi oluşturulamaz.
    }

    /**
     * Tablo adını doğrular ve standart forma dönüştürür.
     *
     * @param tableName tablo adı
     * @return normalize edilmiş tablo adı
     */
    static String validate(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        String normalizedName = tableName.trim();

        if (!normalizedName.matches(TABLE_NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid table name: " + normalizedName
            );
        }

        return normalizedName.toLowerCase(Locale.ROOT);
    }
}