package com.yekdb.query.exception;

public class AmbiguousColumnException extends RuntimeException {

    public AmbiguousColumnException(String columnName) {
        super("Ambiguous column reference: " + columnName);
    }
}