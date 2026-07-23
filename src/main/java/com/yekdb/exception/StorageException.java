package com.yekdb.exception;

public class StorageException extends  YekdbException{
    public StorageException(String message){
        super(message);
    }
    public StorageException(String message,Throwable cause){
        super(message, cause);
    }
}
