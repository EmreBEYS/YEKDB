package com.yekdb.transaction;

import com.yekdb.concurrency.LockAcquisitionInterruptedException;
import com.yekdb.concurrency.DeadlockDetectedException;
import com.yekdb.concurrency.LockConflictException;
import com.yekdb.concurrency.LockHandle;
import com.yekdb.concurrency.LockManager;
import com.yekdb.concurrency.LockManagerRegistry;
import com.yekdb.concurrency.LockMode;
import com.yekdb.concurrency.LockResource;
import com.yekdb.concurrency.LockTimeoutException;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Ayni JVM icindeki QueryExecutor oturumlari arasinda tablo bazli
 * shared read-lock acquisition saglar.
 */
public final class TransactionTableReadLockManager {

    private static final LockManager LOCK_MANAGER =
            LockManagerRegistry.global();

    public TransactionTableReadLock acquire(
            Path databasePath,
            String tableName,
            String ownerId
    ) {

        return acquireInternal(
                databasePath,
                tableName,
                ownerId,
                null
        );
    }

    public TransactionTableReadLock acquire(
            Path databasePath,
            String tableName,
            String ownerId,
            Duration timeout
    ) {

        Objects.requireNonNull(
                timeout,
                "Timeout cannot be null."
        );

        return acquireInternal(
                databasePath,
                tableName,
                ownerId,
                timeout
        );
    }

    private TransactionTableReadLock acquireInternal(
            Path databasePath,
            String tableName,
            String ownerId,
            Duration timeout
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        Objects.requireNonNull(
                tableName,
                "TableName cannot be null."
        );

        Objects.requireNonNull(
                ownerId,
                "OwnerId cannot be null."
        );

        LockResource resource =
                LockResource.table(
                        databasePath,
                        tableName
                );

        LockHandle lockHandle;

        try {

            lockHandle =
                    acquireLock(
                            resource,
                            ownerId,
                            timeout
                    );

        } catch (DeadlockDetectedException exception) {

            throw new TransactionDeadlockException(
                    tableName,
                    LockMode.SHARED,
                    exception
            );

        } catch (LockConflictException exception) {

            throw new TransactionLockException(
                    "Table is write-locked by another active transaction: "
                            + tableName
            );

        } catch (LockTimeoutException exception) {

            throw new TransactionLockTimeoutException(
                    tableName,
                    LockMode.SHARED,
                    exception.getTimeout(),
                    exception
            );

        } catch (LockAcquisitionInterruptedException exception) {

            throw new TransactionLockInterruptedException(
                    tableName,
                    LockMode.SHARED,
                    exception
            );
        }

        return new TransactionTableReadLock(
                createLockKey(resource),
                lockHandle::close
        );
    }

    private LockHandle acquireLock(
            LockResource resource,
            String ownerId,
            Duration timeout
    ) {

        if (timeout == null) {
            return LOCK_MANAGER.acquire(
                    resource,
                    LockMode.SHARED,
                    ownerId
            );
        }

        return LOCK_MANAGER.acquire(
                resource,
                LockMode.SHARED,
                ownerId,
                timeout
        );
    }

    private String createLockKey(
            LockResource resource
    ) {

        return resource.databaseIdentity()
                + "::"
                + resource.resourceName();
    }
}
