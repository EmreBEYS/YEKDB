package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;

import java.util.Locale;

/**
 * YEKDB sütun adlarının doğrulanması ve normalize edilmesinden
 * sorumlu yardımcı sınıftır.
 *
 * Desteklenen sütun biçimleri:
 *
 * name
 * employee_id
 * _internal
 *
 * Qualified column kullanımları:
 *
 * e.name
 * employees.name
 * d.id
 *
 * Her identifier bölümü:
 * - Harf veya alt çizgi (_) ile başlamalıdır.
 * - Devamında harf, rakam veya alt çizgi kullanılabilir.
 *
 * Sürüm: 1.0
 */
final class ColumnNameValidator {

    /**
     * Tek bir SQL identifier bölümü.
     *
     * Örnek:
     * name
     * employee_id
     * _internal
     */
    private static final String IDENTIFIER_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    /**
     * Normal veya qualified column adı.
     *
     * Örnek:
     * name
     * e.name
     * employees.employee_id
     */
    private static final String COLUMN_NAME_PATTERN =
            IDENTIFIER_PATTERN
                    + "(\\."
                    + IDENTIFIER_PATTERN
                    + ")*";

    private ColumnNameValidator() {
        // Utility sınıfı olduğu için nesnesi oluşturulamaz.
    }

    /**
     * Sütun adını doğrular ve normalize eder.
     *
     * @param columnName sütun adı
     * @return normalize edilmiş sütun adı
     */
    static String validate(String columnName) {

        if (columnName == null || columnName.isBlank()) {
            throw new InvalidColumnException(
                    "Column name cannot be null or blank."
            );
        }

        String normalizedName =
                columnName.trim();

        if (!normalizedName.matches(COLUMN_NAME_PATTERN)) {
            throw new InvalidColumnException(
                    "Invalid column name: "
                            + normalizedName
            );
        }

        return normalizedName.toLowerCase(Locale.ROOT);
    }
}