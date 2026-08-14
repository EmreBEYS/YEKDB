package com.yekdb.query.exception;

public class UnknownColumnException extends RuntimeException {

    public UnknownColumnException(String columnName) {
        super("Unknown column: " + columnName);
    }
}