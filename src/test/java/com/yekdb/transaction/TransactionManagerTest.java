package com.yekdb.transaction;

import com.yekdb.transaction.exception.NoActiveTransactionException;
import com.yekdb.transaction.exception.SavepointNotFoundException;
import com.yekdb.transaction.exception.TransactionAlreadyActiveException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionManagerTest {

    private static final Instant FIXED_TIME =
            Instant.parse(
                    "2026-09-03T00:00:00Z"
            );

    @Test
    void shouldBeginTransaction() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        TransactionContext transaction =
                manager.begin();

        assertEquals(
                1L,
                transaction.getTransactionId()
        );

        assertEquals(
                TransactionStatus.ACTIVE,
                transaction.getStatus()
        );

        assertEquals(
                FIXED_TIME,
                transaction.getStartedAt()
        );

        assertTrue(
                manager.hasActiveTransaction()
        );
    }

    @Test
    void shouldBeginReadOnlyTransaction() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        TransactionContext transaction =
                manager.begin(
                        TransactionAccessMode.READ_ONLY,
                        TransactionIsolationLevel.SERIALIZABLE
                );

        assertEquals(
                TransactionAccessMode.READ_ONLY,
                transaction.getAccessMode()
        );

        assertTrue(
                manager.isReadOnlyTransactionActive()
        );

        assertEquals(
                TransactionIsolationLevel.SERIALIZABLE,
                transaction.getIsolationLevel()
        );
    }

    @Test
    void shouldRejectNestedBegin() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        assertThrows(
                TransactionAlreadyActiveException.class,
                manager::begin
        );
    }

    @Test
    void shouldCommitActiveTransaction() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        manager.registerUndoAction(
                () -> {
                }
        );

        TransactionContext transaction =
                manager.commit();

        assertEquals(
                TransactionStatus.COMMITTED,
                transaction.getStatus()
        );

        assertNotNull(
                transaction.getCompletedAt()
        );

        assertFalse(
                manager.hasActiveTransaction()
        );

        assertEquals(
                0,
                manager.getUndoActionCount()
        );
    }

    @Test
    void shouldRollbackActiveTransaction() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        manager.registerUndoAction(
                () -> {
                }
        );

        TransactionContext transaction =
                manager.rollback();

        assertEquals(
                TransactionStatus.ROLLED_BACK,
                transaction.getStatus()
        );

        assertFalse(
                manager.hasActiveTransaction()
        );

        assertEquals(
                0,
                manager.getUndoActionCount()
        );
    }

    @Test
    void shouldRollbackUndoActionsInReverseOrder() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        List<String> operations =
                new ArrayList<>();

        manager.begin();

        manager.registerUndoAction(
                () -> operations.add(
                        "first"
                )
        );

        manager.registerUndoAction(
                () -> operations.add(
                        "second"
                )
        );

        manager.rollback();

        assertEquals(
                List.of(
                        "second",
                        "first"
                ),
                operations
        );
    }

    @Test
    void shouldRollbackOnlyActionsAfterSavepoint() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        List<String> operations =
                new ArrayList<>();

        manager.begin();

        manager.registerUndoAction(
                () -> operations.add(
                        "outer"
                )
        );

        int savepoint =
                manager.createSavepoint();

        manager.registerUndoAction(
                () -> operations.add(
                        "inner"
                )
        );

        manager.rollbackToSavepoint(
                savepoint
        );

        assertEquals(
                List.of(
                        "inner"
                ),
                operations
        );

        assertEquals(
                1,
                manager.getUndoActionCount()
        );

        manager.rollback();

        assertEquals(
                List.of(
                        "inner",
                        "outer"
                ),
                operations
        );
    }

    @Test
    void shouldRollbackToNamedSavepoint() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        List<String> operations =
                new ArrayList<>();

        manager.begin();

        manager.registerUndoAction(
                () -> operations.add(
                        "keep"
                )
        );

        manager.createSavepoint(
                "sp1"
        );

        manager.registerUndoAction(
                () -> operations.add(
                        "rollback"
                )
        );

        manager.rollbackToSavepoint(
                "SP1"
        );

        assertEquals(
                List.of(
                        "rollback"
                ),
                operations
        );

        assertEquals(
                1,
                manager.getUndoActionCount()
        );
    }

    @Test
    void shouldRejectMissingNamedSavepoint() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        assertThrows(
                SavepointNotFoundException.class,
                () -> manager.rollbackToSavepoint(
                        "missing"
                )
        );
    }

    @Test
    void shouldReleaseNamedSavepoint() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        manager.createSavepoint(
                "sp1"
        );

        manager.releaseSavepoint(
                "sp1"
        );

        assertThrows(
                SavepointNotFoundException.class,
                () -> manager.rollbackToSavepoint(
                        "sp1"
                )
        );
    }

    @Test
    void shouldExposeNamedSavepointsInCreationOrder() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        manager.createSavepoint(
                "BeforeInsert"
        );

        manager.registerUndoAction(
                () -> {
                }
        );

        manager.createSavepoint(
                "after_insert"
        );

        List<TransactionSavepointInfo> savepoints =
                manager.getSavepoints();

        assertEquals(
                2,
                savepoints.size()
        );

        assertEquals(
                1,
                savepoints.get(0)
                        .ordinal()
        );

        assertEquals(
                "BeforeInsert",
                savepoints.get(0)
                        .name()
        );

        assertEquals(
                0,
                savepoints.get(0)
                        .undoActionCount()
        );

        assertEquals(
                2,
                savepoints.get(1)
                        .ordinal()
        );

        assertEquals(
                "after_insert",
                savepoints.get(1)
                        .name()
        );

        assertEquals(
                1,
                savepoints.get(1)
                        .undoActionCount()
        );
    }

    @Test
    void shouldRemoveLaterNamedSavepointsAfterRollbackToSavepoint() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        manager.begin();

        manager.createSavepoint(
                "sp1"
        );

        manager.registerUndoAction(
                () -> {
                }
        );

        manager.createSavepoint(
                "sp2"
        );

        manager.rollbackToSavepoint(
                "sp1"
        );

        List<TransactionSavepointInfo> savepoints =
                manager.getSavepoints();

        assertEquals(
                1,
                savepoints.size()
        );

        assertEquals(
                "sp1",
                savepoints.get(0)
                        .name()
        );
    }

    @Test
    void shouldRejectCommitAndRollbackWithoutActiveTransaction() {

        TransactionManager manager =
                new TransactionManager(
                        fixedClock()
                );

        assertThrows(
                NoActiveTransactionException.class,
                manager::commit
        );

        assertThrows(
                NoActiveTransactionException.class,
                manager::rollback
        );
    }

    private static Clock fixedClock() {

        return Clock.fixed(
                FIXED_TIME,
                ZoneOffset.UTC
        );
    }
}
