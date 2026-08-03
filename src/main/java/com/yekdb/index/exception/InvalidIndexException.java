package com.yekdb.index.exception;

/**
 * Geçersiz indeks bilgileri veya işlemleri tespit edildiğinde fırlatılır.
 */
public class InvalidIndexException extends RuntimeException {

    public InvalidIndexException(String message) {
        super(message);
    }

    public InvalidIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}