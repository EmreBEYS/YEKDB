package com.yekdb.query.parser;

/**
 * SQL tokenizer tarafından üretilebilen token türlerini temsil eder.
 *
 * <p>Bu enum, SQL metnindeki anahtar kelimeleri, tanımlayıcıları,
 * sabit değerleri ve noktalama işaretlerini sınıflandırmak için
 * kullanılır.</p>
 */
public enum SqlTokenType {

    /*
     * SQL anahtar kelimeleri
     */
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    INTO,
    VALUES,
    FROM,
    SET,
    WHERE,
    CREATE,
    DROP,
    DATABASE,
    TABLE,
    USE,

    /*
     * Genel token türleri
     */
    IDENTIFIER,
    STRING_LITERAL,
    NUMBER_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,

    /*
     * Operatörler
     */
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUALS,

    /*
     * Noktalama işaretleri
     */
    COMMA,
    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,
    ASTERISK,
    SEMICOLON,

    /*
     * Girdi sonu
     */
    END_OF_INPUT
}