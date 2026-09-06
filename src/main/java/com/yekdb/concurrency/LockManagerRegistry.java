package com.yekdb.concurrency;

/**
 * Ayni JVM icindeki database session'larinin ortak lock alanini saglar.
 */
public final class LockManagerRegistry {

    private static final LockManager GLOBAL_LOCK_MANAGER =
            new InMemoryLockManager();

    private LockManagerRegistry() {
    }

    public static LockManager global() {
        return GLOBAL_LOCK_MANAGER;
    }
}
