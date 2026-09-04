package com.yekdb.transaction;

/**
 * Transaction kapsaminda tutulan tablo write-lock bilgisidir.
 */
public final class TransactionTableWriteLock
        implements AutoCloseable {

    private final String lockKey;
    private final Runnable releaseAction;

    private boolean closed;

    TransactionTableWriteLock(
            String lockKey,
            Runnable releaseAction
    ) {

        this.lockKey = lockKey;
        this.releaseAction = releaseAction;
    }

    public String getLockKey() {
        return lockKey;
    }

    @Override
    public synchronized void close() {

        if (closed) {
            return;
        }

        closed = true;
        releaseAction.run();
    }
}
