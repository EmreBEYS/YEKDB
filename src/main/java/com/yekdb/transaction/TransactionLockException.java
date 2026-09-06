package com.yekdb.transaction;

import com.yekdb.transaction.exception.TransactionException;

/**
 * Transaction izolasyonu icin tablo lock alma/birakma hatalarini temsil eder.
 */
public class TransactionLockException extends TransactionException {

    public TransactionLockException(
            String message
    ) {

        super(message);
    }

    public TransactionLockException(
            String message,
            Throwable cause
    ) {

        super(message, cause);
    }
}
