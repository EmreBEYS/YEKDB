package com.yekdb.transaction.exception;

/**
 * Istenen savepoint aktif transaction icinde bulunamazsa firlatilir.
 */
public final class SavepointNotFoundException extends TransactionException {

    public SavepointNotFoundException(
            String message
    ) {

        super(
                message
        );
    }
}
