package com.yekdb.transaction;

import com.yekdb.transaction.exception.TransactionException;

/**
 * Transaction durability log okuma/yazma hatalarini temsil eder.
 */
public class TransactionDurabilityException extends TransactionException {

    public TransactionDurabilityException(
            String message
    ) {

        super(message);
    }
}
