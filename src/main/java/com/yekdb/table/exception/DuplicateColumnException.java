package com.yekdb.table.exception;

/**
 * Aynı isimde birden fazla sütun tanımlandığında fırlatılır.
 *
 * Version: 1.0
 */
public class DuplicateColumnException extends RuntimeException {

    public DuplicateColumnException(String message) {
        super(message);
    }

    public DuplicateColumnException(String message, Throwable cause) {
        super(message, cause);
    }
}