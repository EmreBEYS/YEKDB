package com.yekdb.transaction;

/**
 * Transaction izolasyon seviyesi ve tablo read-lock yasam dongusu.
 *
 * <p>Sprint 00-36 ile izolasyon seviyesi yalnizca metadata olmaktan
 * cikmistir. Tablo seviyesindeki mevcut concurrency modeli icinde hangi
 * SELECT islemlerinin read-lock alacagini ve kilidin statement ya da
 * transaction sonunda birakilacagini merkezi olarak tanimlar.</p>
 */
public enum TransactionIsolationLevel {

    READ_UNCOMMITTED(false, false),
    READ_COMMITTED(true, false),
    REPEATABLE_READ(true, true),
    SERIALIZABLE(true, true);

    private final boolean readLockRequired;
    private final boolean transactionScopedReadLock;

    TransactionIsolationLevel(
            boolean readLockRequired,
            boolean transactionScopedReadLock
    ) {
        this.readLockRequired = readLockRequired;
        this.transactionScopedReadLock = transactionScopedReadLock;
    }

    public boolean requiresReadLock() {
        return readLockRequired;
    }

    public boolean holdsReadLockUntilTransactionCompletion() {
        return transactionScopedReadLock;
    }
}
