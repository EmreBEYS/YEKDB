package com.yekdb.concurrency;

import java.time.Duration;

/**
 * Concurrency lock acquisition ve release sozlesmesi.
 */
public interface LockManager {

    LockHandle acquire(
            LockResource resource,
            LockMode mode,
            String ownerId
    );

    LockHandle acquire(
            LockResource resource,
            LockMode mode,
            String ownerId,
            Duration timeout
    );

    LockHandle convert(
            LockHandle currentHandle,
            LockMode targetMode
    );

    LockHandle convert(
            LockHandle currentHandle,
            LockMode targetMode,
            Duration timeout
    );

    LockManagerSnapshot snapshot();
}
