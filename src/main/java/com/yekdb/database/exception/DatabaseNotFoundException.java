package com.yekdb.database.exception;

/**
 * İstenen veritabanı bulunamadığında fırlatılır.
 */
public class DatabaseNotFoundException extends RuntimeException {

    public DatabaseNotFoundException(String databaseName) {
        super("Database not found: " + databaseName);
    }

    public DatabaseNotFoundException(
            String databaseName,
            Throwable cause
    ) {
        super("Database not found: " + databaseName, cause);
    }
}