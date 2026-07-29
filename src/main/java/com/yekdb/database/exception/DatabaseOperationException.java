package com.yekdb.database.exception;

/**
 * Veritabanı dosya sistemi işlemi başarısız olduğunda fırlatılır.<
 */
public class DatabaseOperationException extends RuntimeException {

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}