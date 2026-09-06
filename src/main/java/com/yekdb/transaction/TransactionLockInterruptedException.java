package com.yekdb.transaction;

import com.yekdb.concurrency.LockMode;

/**
 * Transaction table lock bekleyisi interrupt edildiginde atilir.
 */
public final class TransactionLockInterruptedException
        extends TransactionLockException {

    public TransactionLockInterruptedException(
            String tableName,
            LockMode lockMode,
            Throwable cause
    ) {

        super(
                "Interrupted while waiting for table "
                        + lockName(lockMode)
                        + " lock: "
                        + tableName,
                cause
        );
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
