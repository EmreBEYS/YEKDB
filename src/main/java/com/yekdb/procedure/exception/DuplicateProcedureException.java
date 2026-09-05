package com.yekdb.procedure.exception;

/**
 * Aynı isimde bir stored procedure zaten bulunduğunda fırlatılır.
 */
public class DuplicateProcedureException extends RuntimeException {

    public DuplicateProcedureException(String message) {
        super(message);
    }

    public DuplicateProcedureException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
