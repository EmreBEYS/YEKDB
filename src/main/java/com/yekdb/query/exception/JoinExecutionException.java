package com.yekdb.query.exception;

public class JoinExecutionException extends RuntimeException {

    public JoinExecutionException(String message) {
        super(message);
    }

    public JoinExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}