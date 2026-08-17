package com.yekdb.table.exception;

/**
 * Geçersiz bir sütun tanımı yapıldığında fırlatılır.
 *
 * Sürüm: 1.0
 */
public class InvalidColumnException
        extends IllegalArgumentException {

    public InvalidColumnException(String message) {
        super(message);
    }

    public InvalidColumnException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}