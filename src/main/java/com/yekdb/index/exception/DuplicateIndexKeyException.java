package com.yekdb.index.exception;

/**
 * PRIMARY veya UNIQUE bir indekse tekrar eden anahtar
 * eklenmeye çalışıldığında fırlatılır.
 */
public class DuplicateIndexKeyException extends RuntimeException {

    public DuplicateIndexKeyException(String message) {
        super(message);
    }

    public DuplicateIndexKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}