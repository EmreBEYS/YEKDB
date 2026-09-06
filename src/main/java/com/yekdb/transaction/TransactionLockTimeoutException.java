package com.yekdb.transaction;

import com.yekdb.concurrency.LockMode;

import java.time.Duration;

/**
 * Transaction table lock istegi verilen surede karsilanamadiginda atilir.
 */
public final class TransactionLockTimeoutException
        extends TransactionLockException {

    private final String tableName;
    private final LockMode lockMode;
    private final Duration timeout;

    public TransactionLockTimeoutException(
            String tableName,
            LockMode lockMode,
            Duration timeout,
            Throwable cause
    ) {

        super(
                "Timed out waiting for table "
                        + lockName(lockMode)
                        + " lock after "
                        + timeout.toMillis()
                        + " ms: "
                        + tableName,
                cause
        );

        this.tableName = tableName;
        this.lockMode = lockMode;
        this.timeout = timeout;
    }

    public String getTableName() {
        return tableName;
    }

    public LockMode getLockMode() {
        return lockMode;
    }

    public Duration getTimeout() {
        return timeout;
    }

    private static String lockName(
            LockMode lockMode
    ) {

        if (lockMode == LockMode.SHARED) {
            return "read";
        }

        return "write";
    }
}
