package com.yekdb.index.exception;

/**
 * Aynı isimde bir indeks tekrar oluşturulmaya çalışıldığında fırlatılır.
 */
public class DuplicateIndexException extends RuntimeException {

    public DuplicateIndexException(String message) {
        super(message);
    }

    public DuplicateIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}