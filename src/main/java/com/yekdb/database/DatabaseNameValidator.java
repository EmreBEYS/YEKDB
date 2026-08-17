package com.yekdb.database;

/**
 * YEKDB veritabanı adlarının doğrulanmasından sorumlu yardımcı sınıftır.
 *
 * <p>Geçerli bir veritabanı adı:</p>
 * <ul>
 *     <li>Boş veya null olamaz.</li>
 *     <li>Bir harf ile başlamalıdır.</li>
 *     <li>Sadece harf, rakam ve alt çizgi (_) içerebilir.</li>
 * </ul>
 */
final class DatabaseNameValidator {

    private static final String DATABASE_NAME_PATTERN =
            "[A-Za-z][A-Za-z0-9_]*";

    private DatabaseNameValidator() {
        // Utility sınıfının nesnesi oluşturulamaz.
    }

    /**
     * Veritabanı adını doğrular ve normalize eder.
     *
     * @param databaseName doğrulanacak veritabanı adı
     * @return başındaki ve sonundaki boşlukları temizlenmiş ad
     */
    static String validate(String databaseName) {

        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException(
                    "Database name cannot be null or blank."
            );
        }

        String normalizedName = databaseName.trim();

        if (!normalizedName.matches(DATABASE_NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Database name must start with a letter and "
                            + "contain only letters, numbers, "
                            + "and underscores."
            );
        }

        return normalizedName;
    }
}