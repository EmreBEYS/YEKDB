package com.yekdb.query.parser;

/**
 * SQL tokenizer tarafından üretilebilen token türlerini temsil eder.
 *
 * Bu enum, SQL metnindeki anahtar kelimeleri,
 * tanımlayıcıları, sabit değerleri,
 * operatörleri ve noktalama işaretlerini
 * sınıflandırmak için kullanılır.
 *
 * Sprint 00-15:
 *
 * - INNER
 * - JOIN
 * - ON
 *
 * tokenları eklenmiştir.
 */
public enum SqlTokenType {

    // ==================================================
    // CORE SQL KEYWORDS
    // ==================================================

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

    // ==================================================
    // SPRINT 00-14
    // ALIAS
    // ==================================================

    AS,

    // ==================================================
    // SPRINT 00-14
    // PREDICATES
    // ==================================================

    BETWEEN,
    IN,
    LIKE,
    ILIKE,
    NOT,

    // ==================================================
    // SPRINT 00-14
    // ORDERING
    // ==================================================

    ORDER,
    BY,
    ASC,
    DESC,

    // ==================================================
    // SPRINT 00-14
    // RESULT LIMITING
    // ==================================================

    LIMIT,
    FETCH,
    FIRST,
    NEXT,
    ROW,
    ROWS,
    ONLY,

    // ==================================================
    // SPRINT 00-14
    // GROUPING
    // ==================================================

    GROUP,
    HAVING,

    // ==================================================
    // SPRINT 00-15
    // JOIN FOUNDATION
    // ==================================================

    /**
     * Explicit INNER JOIN keyword.
     *
     * Example:
     *
     * INNER JOIN department d
     */
    INNER,

    /**
     * JOIN keyword.
     *
     * Sprint 00-15 initially uses this together
     * with INNER.
     */
    JOIN,

    /**
     * JOIN condition keyword.
     *
     * Example:
     *
     * ON e.department_id = d.id
     */
    ON,

    // ==================================================
    // GENERAL TOKEN TYPES
    // ==================================================

    IDENTIFIER,

    STRING_LITERAL,
    NUMBER_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,

    // ==================================================
    // COMPARISON OPERATORS
    // ==================================================

    EQUALS,
    NOT_EQUALS,

    GREATER_THAN,
    GREATER_THAN_OR_EQUALS,

    LESS_THAN,
    LESS_THAN_OR_EQUALS,

    // ==================================================
    // PUNCTUATION
    // ==================================================

    COMMA,
    DOT,

    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,

    ASTERISK,
    SEMICOLON,

    // ==================================================
    // END
    // ==================================================

    END_OF_INPUT
}