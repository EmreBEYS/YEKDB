package com.yekdb.query.executor;

/**
 * SQL komutlarının yürütülmesi sırasında meydana gelen
 * hataları temsil eder.
 */
public class QueryExecutionException extends RuntimeException {

    public QueryExecutionException(String message) {
        super(message);
    }

    public QueryExecutionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}