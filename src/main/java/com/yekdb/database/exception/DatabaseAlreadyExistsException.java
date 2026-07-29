package com.yekdb.database.exception;

/**
 * Zaten var olan bir veritabanı oluşturmaya çalışırken fırlatılır.
 */

public class DatabaseAlreadyExistsException  extends RuntimeException {
    public DatabaseAlreadyExistsException(String databaseName){
        super("Database already exits: "+databaseName);
    }
    public DatabaseAlreadyExistsException(String databaseName,Throwable cause){
        super("Database already exits: "+databaseName,cause);
    }
}
