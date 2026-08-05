package com.yekdb.query.parser;

import java.util.Objects;

/**
 * SQL Tokenizer tarafından üretilen tek bir tokenı temsil eder.
 *
 * Her token:
 * - bir türe
 * - metinsel değere
 * sahiptir.
 */

public final class SqlToken {
    /**
     * Token türü.
     */
    private final SqlTokenType type;

    /**
     * Token'ın metinsel değeri.
     */
    private final String value;

    /**
     * Yeni bir SQL token oluşturur.
     *
     * @param type token türü
     * @param value token değeri
     */
    public SqlToken(SqlTokenType type,String value){
        this.type=Objects.requireNonNull(type,"Token type cannot be null");
        this.value=value== null ? "" :value;
    }
    /**
     * Token türünü döndürür.
     *
     * @return token türü
     */
    public SqlTokenType getType(){
        return type;
    }
    /**
     * Token değerini döndürür.
     *
     * @return token değeri
     */
    public String getValue() {
        return value;
    }
    /**
     * Token'ın belirtilen türde olup olmadığını kontrol eder.
     *
     * @param expectedType beklenen tür
     * @return eşleşiyorsa true
     */
    public boolean is(SqlTokenType expectedType){
        return type== expectedType;
    }
    @Override
    public String toString() {
        return "SqlToken{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof SqlToken other)) {
            return false;
        }

        return type == other.type
                && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type,
                value
        );
    }


}

