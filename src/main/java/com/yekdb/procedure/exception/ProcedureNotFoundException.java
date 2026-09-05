package com.yekdb.procedure.exception;

/**
 * İstenen stored procedure bulunamadığında fırlatılır.
 */
public class ProcedureNotFoundException extends RuntimeException {

    public ProcedureNotFoundException(String message) {
        super(message);
    }

    public ProcedureNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
