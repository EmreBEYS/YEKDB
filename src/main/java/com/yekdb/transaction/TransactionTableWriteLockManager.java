package com.yekdb.transaction;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ayni JVM icindeki QueryExecutor oturumlari arasinda tablo bazli
 * write izolasyonu saglar.
 */
public final class TransactionTableWriteLockManager {

    private static final Map<String, LockOwner> LOCKS =
            new ConcurrentHashMap<>();

    public TransactionTableWriteLock acquire(
            Path databasePath,
            String tableName,
            String ownerId
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

        String lockKey =
                createLockKey(
                        databasePath,
                        tableName
                );

        synchronized (LOCKS) {

            LockOwner currentOwner =
                    LOCKS.get(lockKey);

            if (currentOwner != null
                    && !currentOwner.ownerId()
                    .equals(ownerId)) {

                throw new TransactionLockException(
                        "Table is locked by another active transaction: "
                                + tableName
                );
            }

            LOCKS.put(
                    lockKey,
                    new LockOwner(
                            ownerId
                    )
            );
        }

        return new TransactionTableWriteLock(
                lockKey,
                () -> release(
                        lockKey,
                        ownerId
                )
        );
    }

    private void release(
            String lockKey,
            String ownerId
    ) {

        synchronized (LOCKS) {

            LockOwner currentOwner =
                    LOCKS.get(lockKey);

            if (currentOwner == null) {
                return;
            }

            if (currentOwner.ownerId()
                    .equals(ownerId)) {
                LOCKS.remove(lockKey);
            }
        }
    }

    private String createLockKey(
            Path databasePath,
            String tableName
    ) {

        return databasePath
                .toAbsolutePath()
                .normalize()
                .toString()
                .toLowerCase(Locale.ROOT)
                + "::"
                + tableName.toLowerCase(Locale.ROOT);
    }

    private record LockOwner(
            String ownerId
    ) {
    }
}
