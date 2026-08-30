package com.yekdb.query.function;

import java.util.Objects;

/**
 * Bir SQL fonksiyonuna geçirilen bir parametreyi temsil eder.
 *
 * FunctionParameter, fonksiyon argümanlarının sabit değerlerden,
 * sütunlardan veya değerlendirilmiş ifadelerden kaynaklanabileceği
 * için kasıtlı olarak genel tutulmuştur.
 */
public final class FunctionParameter {

    private final Object value;

    public FunctionParameter(Object value) {
        this.value = value;
    }

    /**
     * Yeni bir fonksiyon parametresi oluşturur.
     *
     * @param value parametre değeri
     * @return fonksiyon parametresi
     */
    public static FunctionParameter of(Object value) {
        return new FunctionParameter(value);
    }

    /**
     * Ham parametre değerini döndürür.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Bu parametre SQL NULL değerini temsil ettiğinde true döndürür.
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Temel değer String olduğunda true döndürür.
     */
    public boolean isString() {
        return value instanceof String;
    }

    /**
     * Temel değer sayısal olduğunda true döndürür.
     */
    public boolean isNumber() {
        return value instanceof Number;
    }

    /**
     * Değeri String olarak döndürür.
     *
     * @return String değer veya SQL NULL için null
     * @throws FunctionException değer String değilse
     */
    public String asString() {
        if (value == null) {
            return null;
        }

        if (!(value instanceof String)) {
            throw new FunctionException(
                    "Expected STRING function parameter but got "
                            + value.getClass().getSimpleName()
            );
        }

        return (String) value;
    }

    /**
     * Değeri Number olarak döndürür.
     *
     * @return Number değer veya SQL NULL için null
     * @throws FunctionException değer sayısal değilse
     */
    public Number asNumber() {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Number)) {
            throw new FunctionException(
                    "Expected NUMERIC function parameter but got "
                            + value.getClass().getSimpleName()
            );
        }

        return (Number) value;
    }

    @Override
    public String toString() {
        return Objects.toString(value, "NULL");
    }
}