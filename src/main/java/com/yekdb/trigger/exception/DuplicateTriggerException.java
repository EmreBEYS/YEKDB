package com.yekdb.trigger.exception;

/**
 * Aynı isimde bir trigger zaten bulunduğunda fırlatılır.
 */
public class DuplicateTriggerException extends RuntimeException {

    public DuplicateTriggerException(String message) {
        super(message);
    }

    public DuplicateTriggerException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
