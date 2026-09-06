package com.yekdb.transaction.exception;

import com.yekdb.exception.YekdbException;

/**
 * Transaction subsystem icin ortak hata tipidir.
 */
public class TransactionException extends YekdbException {

    public TransactionException(
            String message
    ) {

        super(
                message
        );
    }

    public TransactionException(
            String message,
            Throwable cause
    ) {

        super(
                message,
                cause
        );
    }
}
