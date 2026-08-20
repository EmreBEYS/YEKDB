package com.yekdb.storage.table.header;

/**
 * Table header metadata güncelleme işlemleri sırasında
 * oluşan geçersiz mutation durumlarını temsil eder.
 */
public class TableHeaderUpdateException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TableHeaderUpdateException(
            String message
    ) {
        super(message);
    }

    public TableHeaderUpdateException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}