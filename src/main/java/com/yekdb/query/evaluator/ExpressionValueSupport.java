package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ColumnExpression;

import java.util.Map;
import java.util.Objects;

/**
 * Expression değerlendirmesinde kullanılan ortak değer yardımcılarını tutar.
 *
 * <p>Bu sınıf kolon çözümleme, sayısal eşitlik, LIKE regex üretimi ve
 * sıralama karşılaştırması gibi düşük seviyeli işlemleri tek noktada toplar.</p>
 */
final class ExpressionValueSupport {

    private ExpressionValueSupport() {
        // Utility sınıfı.
    }

    static Object resolveColumnValue(
            ColumnExpression columnExpression,
            Map<String, Object> rowValues
    ) {
        Objects.requireNonNull(
                columnExpression,
                "Column expression cannot be null."
        );

        String columnReference =
                columnExpression.getQualifiedName();

        for (Map.Entry<String, Object> entry
                : rowValues.entrySet()) {

            if (entry.getKey()
                    .equalsIgnoreCase(columnReference)) {

                return entry.getValue();
            }
        }

        if (columnExpression.isQualified()) {
            throw new IllegalArgumentException(
                    "Column not found: " + columnReference
            );
        }

        String suffix =
                "." + columnExpression.getColumnName();

        Object resolvedValue = null;
        boolean found = false;

        for (Map.Entry<String, Object> entry
                : rowValues.entrySet()) {

            if (!entry.getKey()
                    .toLowerCase()
                    .endsWith(suffix.toLowerCase())) {

                continue;
            }

            if (!found) {
                resolvedValue = entry.getValue();
                found = true;
                continue;
            }

            if (!valuesEqual(
                    resolvedValue,
                    entry.getValue()
            )) {
                throw new IllegalArgumentException(
                        "Ambiguous column reference: "
                                + columnExpression.getColumnName()
                );
            }
        }

        if (!found) {
            throw new IllegalArgumentException(
                    "Column not found: "
                            + columnExpression.getColumnName()
            );
        }

        return resolvedValue;
    }

    static boolean valuesEqual(
            Object actualValue,
            Object expectedValue
    ) {
        if (actualValue == null
                || expectedValue == null) {

            return Objects.equals(
                    actualValue,
                    expectedValue
            );
        }

        if (actualValue instanceof Number actualNumber
                && expectedValue instanceof Number expectedNumber) {

            return Double.compare(
                    actualNumber.doubleValue(),
                    expectedNumber.doubleValue()
            ) == 0;
        }

        return Objects.equals(
                actualValue,
                expectedValue
        );
    }

    static String toLikeRegex(String sqlPattern) {
        StringBuilder regex = new StringBuilder("^");

        for (int i = 0; i < sqlPattern.length(); i++) {
            char character = sqlPattern.charAt(i);

            switch (character) {
                case '%' -> regex.append(".*");
                case '_' -> regex.append(".");

                case '\\',
                     '.',
                     '^',
                     '$',
                     '|',
                     '?',
                     '*',
                     '+',
                     '(',
                     ')',
                     '[',
                     ']',
                     '{',
                     '}' -> regex.append("\\").append(character);

                default -> regex.append(character);
            }
        }

        return regex.append("$").toString();
    }

    static int compare(
            Object actualValue,
            Object expectedValue
    ) {
        if (actualValue == null
                || expectedValue == null) {

            throw new IllegalArgumentException(
                    "Ordering comparison cannot be performed with null values."
            );
        }

        if (actualValue instanceof Number actualNumber
                && expectedValue instanceof Number expectedNumber) {

            return Double.compare(
                    actualNumber.doubleValue(),
                    expectedNumber.doubleValue()
            );
        }

        if (actualValue instanceof Comparable<?> comparable
                && actualValue.getClass().isInstance(expectedValue)) {

            return compareComparable(
                    comparable,
                    expectedValue
            );
        }

        throw new IllegalArgumentException(
                "Values cannot be compared: "
                        + actualValue
                        + " and "
                        + expectedValue
        );
    }

    @SuppressWarnings("unchecked")
    private static int compareComparable(
            Comparable<?> actualValue,
            Object expectedValue
    ) {
        Comparable<Object> comparable =
                (Comparable<Object>) actualValue;

        return comparable.compareTo(expectedValue);
    }
}
