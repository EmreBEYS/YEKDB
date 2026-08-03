package com.yekdb.index.exception;

/**
 * İstenen indeks bulunamadığında fırlatılır.
 */
public class IndexNotFoundException extends RuntimeException {

    public IndexNotFoundException(String message) {
        super(message);
    }

    public IndexNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}