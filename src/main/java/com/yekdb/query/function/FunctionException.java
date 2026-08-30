package com.yekdb.query.function;

/**
 * SQL fonksiyon alt sistemi tarafından kullanılan temel çalışma zamanı istisnası.
 */
public class FunctionException extends RuntimeException {
    public FunctionException(String message) {
        super(message);
    }

    public FunctionException(String message, Throwable cause) {
        super(message, cause);
    }
}
