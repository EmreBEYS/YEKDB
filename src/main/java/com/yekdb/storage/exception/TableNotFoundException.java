package com.yekdb.storage.exception;

/**
 * İstenen tablo bulunamadığında fırlatılır.
 */
public class TableNotFoundException extends RuntimeException {

    public TableNotFoundException(String message) {
        super(message);
    }

    public TableNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}