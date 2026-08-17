package com.yekdb.index;

import com.yekdb.index.exception.InvalidIndexException;

import java.util.Locale;

/**
 * Index altyapısında kullanılan identifier isimlerinin
 * doğrulanması ve normalize edilmesinden sorumludur.
 *
 * Bu sınıf:
 * - index adı,
 * - database adı,
 * - table adı,
 * - column adı
 *
 * için ortak doğrulama kurallarını sağlar.
 */
final class IndexIdentifierValidator {

    private static final String STANDARD_IDENTIFIER_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    private static final String DATABASE_NAME_PATTERN =
            "[A-Za-z][A-Za-z0-9_]*";

    private IndexIdentifierValidator() {
        // Utility sınıfı.
    }

    static String validateIndexName(String indexName) {
        return validate(
                indexName,
                "Index adı",
                STANDARD_IDENTIFIER_PATTERN
        );
    }

    static String validateDatabaseName(String databaseName) {
        return validate(
                databaseName,
                "Veritabanı adı",
                DATABASE_NAME_PATTERN
        );
    }

    static String validateTableName(String tableName) {
        return validate(
                tableName,
                "Tablo adı",
                STANDARD_IDENTIFIER_PATTERN
        );
    }

    static String validateColumnName(String columnName) {
        return validate(
                columnName,
                "Kolon adı",
                STANDARD_IDENTIFIER_PATTERN
        );
    }

    static boolean isValidIndexName(String value) {
        return matches(
                value,
                STANDARD_IDENTIFIER_PATTERN
        );
    }

    static boolean isValidDatabaseName(String value) {
        return matches(
                value,
                DATABASE_NAME_PATTERN
        );
    }

    static boolean isValidTableName(String value) {
        return matches(
                value,
                STANDARD_IDENTIFIER_PATTERN
        );
    }

    static boolean isValidColumnName(String value) {
        return matches(
                value,
                STANDARD_IDENTIFIER_PATTERN
        );
    }

    static String normalizeForComparison(String value) {

        if (value == null) {
            return null;
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String validate(
            String value,
            String fieldName,
            String pattern
    ) {

        if (value == null || value.isBlank()) {
            throw new InvalidIndexException(
                    fieldName + " null veya boş olamaz."
            );
        }

        String normalizedValue =
                normalizeForComparison(value);

        if (!normalizedValue.matches(pattern)) {
            throw new InvalidIndexException(
                    "Geçersiz "
                            + fieldName.toLowerCase(Locale.ROOT)
                            + ": "
                            + value
            );
        }

        return normalizedValue;
    }

    private static boolean matches(
            String value,
            String pattern
    ) {

        if (value == null || value.isBlank()) {
            return false;
        }

        String normalizedValue =
                normalizeForComparison(value);

        return normalizedValue.matches(pattern);
    }
}