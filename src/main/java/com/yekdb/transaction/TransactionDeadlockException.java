package com.yekdb.transaction;

import com.yekdb.concurrency.DeadlockDetectedException;
import com.yekdb.concurrency.LockMode;

import java.util.List;

/**
 * Transaction lock bekleyisi deadlock dongusune katildiginda atilir.
 */
public final class TransactionDeadlockException
        extends TransactionLockException {

    private final String tableName;
    private final LockMode lockMode;
    private final String ownerId;
    private final List<String> cycleOwners;

    public TransactionDeadlockException(
            String tableName,
            LockMode lockMode,
            DeadlockDetectedException cause
    ) {

        super(
                "Deadlock detected while waiting for table "
                        + lockName(lockMode)
                        + " lock: "
                        + tableName,
                cause
        );

        this.tableName = tableName;
        this.lockMode = lockMode;
        this.ownerId = cause.getOwnerId();
        this.cycleOwners = cause.getCycleOwners();
    }

    public String getTableName() {
        return tableName;
    }

    public LockMode getLockMode() {
        return lockMode;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getCycleOwners() {
        return cycleOwners;
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
