package com.yekdb.table.exception;

/**
 * Geçersiz sütun tanımı yapıldığında fırlatılır.
 *
 * Version: 1.0
 */
public class InvalidColumnException extends RuntimeException {

    public InvalidColumnException(String message) {
        super(message);
    }

    public InvalidColumnException(String message, Throwable cause) {
        super(message, cause);
    }
}