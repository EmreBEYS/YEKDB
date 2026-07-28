package com.yekdb.storage.file;

/**
 * YEKDB veri dosyası işlemleri sırasında oluşan
 * storage katmanı hatalarını temsil eder.
 */

public class DataFileException extends RuntimeException{

    public DataFileException(String message){
        super(message);
    }
    public DataFileException(String message, Throwable cause){
        super(message, cause);
    }
}
