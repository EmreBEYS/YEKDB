package com.yekdb.view.exception;

/**
 * İstenen view bulunamadığında fırlatılır.
 */
public class ViewNotFoundException extends RuntimeException {

    public ViewNotFoundException(String message) {
        super(message);
    }

    public ViewNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
