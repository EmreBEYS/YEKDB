package com.yekdb.storage.exception;

/**
 * Diskten okunan binary TableHeader verisinin bozuk,
 * eksik veya geçersiz olması durumunda fırlatılır.
 */
public class CorruptedTableHeaderException extends RuntimeException {

    public CorruptedTableHeaderException(String message) {
        super(message);
    }

    public CorruptedTableHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}