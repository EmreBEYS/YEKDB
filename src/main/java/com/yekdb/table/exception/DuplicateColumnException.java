package com.yekdb.table.exception;

/**
 * Aynı isimde birden fazla sütun tanımlandığında fırlatılır.
 *
 * Sürüm: 1.0
 */
public class DuplicateColumnException
        extends IllegalArgumentException {

    public DuplicateColumnException(String message) {
        super(message);
    }

    public DuplicateColumnException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}