package com.yekdb.exception;

public class YekdbException extends RuntimeException {

    public YekdbException(String message) {
        super(message);
    }

    public YekdbException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}