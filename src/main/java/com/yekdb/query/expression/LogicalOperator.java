package com.yekdb.query.expression;

/**
 * Birden fazla WHERE ifadesini birbirine bağlayan
 * mantıksal operatörleri temsil eder.
 */
public enum LogicalOperator {

    AND,
    OR;

    /**
     * Metinsel operatörü enum değerine dönüştürür.
     *
     * @param value AND veya OR değeri
     * @return karşılık gelen LogicalOperator
     */
    public static LogicalOperator fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Mantıksal operatör boş olamaz."
            );
        }

        return switch (value.trim().toUpperCase()) {
            case "AND" -> AND;
            case "OR" -> OR;

            default -> throw new IllegalArgumentException(
                    "Desteklenmeyen mantıksal operatör: " + value
            );
        };
    }
}