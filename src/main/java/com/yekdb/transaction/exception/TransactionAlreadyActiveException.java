package com.yekdb.transaction.exception;

/**
 * Yeni transaction baslatilmak istenirken aktif transaction
 * zaten varsa firlatilir.
 */
public final class TransactionAlreadyActiveException extends TransactionException {

    public TransactionAlreadyActiveException(
            String message
    ) {

        super(
                message
        );
    }
}
