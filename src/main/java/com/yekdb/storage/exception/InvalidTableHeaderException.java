package com.yekdb.storage.exception;

/**
 * Mantıksal olarak geçersiz bir TableHeader oluşturulduğunda fırlatılır.
 */
public class InvalidTableHeaderException extends RuntimeException {

    public InvalidTableHeaderException(String message) {
        super(message);
    }

    public InvalidTableHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}