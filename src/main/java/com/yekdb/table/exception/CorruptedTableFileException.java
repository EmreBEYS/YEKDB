package com.yekdb.table.exception;
/**
 * Fiziksel tablo dosyası geçersiz, eksik veya
 * bozulmuş olduğunda fırlatılır.
 *
 * Sürüm: 1.0
 */

public class CorruptedTableFileException extends RuntimeException {
    public CorruptedTableFileException(String message){
        super(message);
    }
    public CorruptedTableFileException(String message,Throwable cause ){
        super(message,cause);
    }
}
