package com.yekdb.view.exception;

/**
 * Aynı isimde bir view zaten bulunduğunda fırlatılır.
 */
public class DuplicateViewException extends RuntimeException {

    public DuplicateViewException(String message) {
        super(message);
    }

    public DuplicateViewException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
