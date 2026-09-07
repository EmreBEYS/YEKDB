package com.yekdb.transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 00-36 Phase 1 isolation policy contract.
 */
class TransactionIsolationLevelPolicyTest {

    @Test
    void shouldDefineReadLockPolicyForEveryIsolationLevel() {

        assertFalse(
                TransactionIsolationLevel.READ_UNCOMMITTED
                        .requiresReadLock()
        );

        assertTrue(
                TransactionIsolationLevel.READ_COMMITTED
                        .requiresReadLock()
        );

        assertFalse(
                TransactionIsolationLevel.READ_COMMITTED
                        .holdsReadLockUntilTransactionCompletion()
        );

        assertTrue(
                TransactionIsolationLevel.REPEATABLE_READ
                        .holdsReadLockUntilTransactionCompletion()
        );

        assertTrue(
                TransactionIsolationLevel.SERIALIZABLE
                        .holdsReadLockUntilTransactionCompletion()
        );
    }
}
