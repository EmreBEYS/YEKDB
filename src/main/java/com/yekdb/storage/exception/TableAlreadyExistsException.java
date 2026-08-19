package com.yekdb.storage.exception;

/**
 * Aynı isimde bir tablo zaten bulunduğunda fırlatılır.
 */
public class TableAlreadyExistsException extends RuntimeException {

    public TableAlreadyExistsException(String message) {
        super(message);
    }

    public TableAlreadyExistsException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}