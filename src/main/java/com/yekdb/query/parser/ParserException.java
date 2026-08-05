package com.yekdb.query.parser;

/**
 * SQL tokenization veya ayrıştırma işlemi sırasında
 * oluşan hataları temsil eder.
 */
public class ParserException extends RuntimeException {

    /**
     * Açıklama mesajıyla yeni bir ParserException oluşturur.
     *
     * @param message hata mesajı
     */
    public ParserException(String message) {
        super(message);
    }

    /**
     * Açıklama mesajı ve asıl hatayla yeni bir
     * ParserException oluşturur.
     *
     * @param message hata mesajı
     * @param cause   hatanın asıl nedeni
     */
    public ParserException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}