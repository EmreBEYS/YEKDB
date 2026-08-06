package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonOperator;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * İki değeri verilen karşılaştırma operatörüne göre değerlendirir.
 *
 * Desteklenen operatörler:
 *
 * =
 * !=
 * >
 * <
 * >=
 * <=
 */
public final class PredicateEvaluator {

    /**
     * PredicateEvaluator nesne üretilmesine ihtiyaç duymaz.
     */
    private PredicateEvaluator() {
    }

    /**
     * İki değeri belirtilen operatöre göre karşılaştırır.
     *
     * @param actualValue satırdan alınan gerçek değer
     * @param expectedValue WHERE koşulunda beklenen değer
     * @param operator karşılaştırma operatörü
     * @return karşılaştırma sonucu
     */
    public static boolean evaluate(
            Object actualValue,
            Object expectedValue,
            ComparisonOperator operator
    ) {
        Objects.requireNonNull(
                operator,
                "Karşılaştırma operatörü null olamaz."
        );

        return switch (operator) {
            case EQUALS ->
                    equalsValue(actualValue, expectedValue);

            case NOT_EQUALS ->
                    !equalsValue(actualValue, expectedValue);

            case GREATER_THAN ->
                    compareValues(actualValue, expectedValue) > 0;

            case LESS_THAN ->
                    compareValues(actualValue, expectedValue) < 0;

            case GREATER_THAN_OR_EQUALS ->
                    compareValues(actualValue, expectedValue) >= 0;

            case LESS_THAN_OR_EQUALS ->
                    compareValues(actualValue, expectedValue) <= 0;
        };
    }

    /**
     * İki değerin eşit olup olmadığını kontrol eder.
     */
    private static boolean equalsValue(
            Object actualValue,
            Object expectedValue
    ) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }

        if (actualValue == null || expectedValue == null) {
            return false;
        }

        if (actualValue instanceof Number actualNumber
                && expectedValue instanceof Number expectedNumber) {

            BigDecimal actualDecimal = toBigDecimal(actualNumber);
            BigDecimal expectedDecimal = toBigDecimal(expectedNumber);

            return actualDecimal.compareTo(expectedDecimal) == 0;
        }

        return Objects.equals(actualValue, expectedValue);
    }

    /**
     * İki değeri sıralama operatörleri için karşılaştırır.
     *
     * @return pozitif, negatif veya sıfır
     */
    private static int compareValues(
            Object actualValue,
            Object expectedValue
    ) {
        if (actualValue == null || expectedValue == null) {
            throw new IllegalArgumentException(
                    "Sıralama karşılaştırmalarında null değer kullanılamaz."
            );
        }

        if (actualValue instanceof Number actualNumber
                && expectedValue instanceof Number expectedNumber) {

            BigDecimal actualDecimal = toBigDecimal(actualNumber);
            BigDecimal expectedDecimal = toBigDecimal(expectedNumber);

            return actualDecimal.compareTo(expectedDecimal);
        }

        if (actualValue instanceof String actualString
                && expectedValue instanceof String expectedString) {

            return actualString.compareTo(expectedString);
        }

        if (actualValue instanceof Comparable<?> comparable
                && actualValue.getClass().isInstance(expectedValue)) {

            return compareComparable(comparable, expectedValue);
        }

        throw new IllegalArgumentException(
                "Değerler karşılaştırılamıyor. Gerçek değer türü: "
                        + actualValue.getClass().getName()
                        + ", beklenen değer türü: "
                        + expectedValue.getClass().getName()
        );
    }

    /**
     * Number türünü güvenli biçimde BigDecimal değerine dönüştürür.
     */
    private static BigDecimal toBigDecimal(Number number) {
        return new BigDecimal(number.toString());
    }

    /**
     * Comparable nesneleri karşılaştırır.
     */
    @SuppressWarnings("unchecked")
    private static int compareComparable(
            Comparable<?> comparable,
            Object expectedValue
    ) {
        return ((Comparable<Object>) comparable).compareTo(expectedValue);
    }
}