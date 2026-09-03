package com.yekdb.transaction.exception;

/**
 * COMMIT veya ROLLBACK aktif transaction olmadan
 * cagrildiginda firlatilir.
 */
public final class NoActiveTransactionException extends TransactionException {

    public NoActiveTransactionException(
            String message
    ) {

        super(
                message
        );
    }
}
