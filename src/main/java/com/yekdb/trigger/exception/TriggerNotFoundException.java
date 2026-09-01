package com.yekdb.trigger.exception;

/**
 * İstenen trigger bulunamadığında fırlatılır.
 */
public class TriggerNotFoundException extends RuntimeException {

    public TriggerNotFoundException(String message) {
        super(message);
    }

    public TriggerNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
