package com.yekdb.transaction;

import com.yekdb.transaction.exception.NoActiveTransactionException;
import com.yekdb.transaction.exception.SavepointNotFoundException;
import com.yekdb.transaction.exception.TransactionAlreadyActiveException;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Session-scope transaction yasam dongusunu yonetir.
 */
public final class TransactionManager {

    private final AtomicLong transactionIdSequence;
    private final Clock clock;
    private final Deque<TransactionUndoAction> undoActions;
    private final Deque<NamedSavepoint> savepoints;

    private TransactionContext activeTransaction;

    public TransactionManager() {

        this(
                Clock.systemUTC()
        );
    }

    public TransactionManager(
            Clock clock
    ) {

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "Clock cannot be null."
                );

        this.transactionIdSequence =
                new AtomicLong();

        this.undoActions =
                new ArrayDeque<>();

        this.savepoints =
                new ArrayDeque<>();
    }

    public TransactionContext begin() {

        return begin(
                TransactionAccessMode.READ_WRITE
        );
    }

    public TransactionContext begin(
            TransactionAccessMode accessMode
    ) {

        return begin(
                accessMode,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public TransactionContext begin(
            TransactionAccessMode accessMode,
            TransactionIsolationLevel isolationLevel
    ) {

        if (activeTransaction != null
                && activeTransaction.isActive()) {

            throw new TransactionAlreadyActiveException(
                    "A transaction is already active."
            );
        }

        activeTransaction =
                new TransactionContext(
                        transactionIdSequence.incrementAndGet(),
                        clock.instant(),
                        Objects.requireNonNull(
                                accessMode,
                                "AccessMode cannot be null."
                        ),
                        Objects.requireNonNull(
                                isolationLevel,
                                "IsolationLevel cannot be null."
                        )
                );

        return activeTransaction;
    }

    public TransactionContext commit() {

        TransactionContext transaction =
                requireActiveTransaction();

        transaction.markCommitted(
                clock.instant()
        );

        undoActions.clear();
        savepoints.clear();

        activeTransaction =
                null;

        return transaction;
    }

    public TransactionContext rollback() {

        TransactionContext transaction =
                requireActiveTransaction();

        try {

            rollbackUndoActions();

        } finally {

            if (transaction.isActive()) {
                transaction.markRolledBack(
                        clock.instant()
                );
            }

            activeTransaction =
                    null;
        }

        return transaction;
    }

    public Optional<TransactionContext> getActiveTransaction() {

        if (activeTransaction == null
                || !activeTransaction.isActive()) {

            return Optional.empty();
        }

        return Optional.of(
                activeTransaction
        );
    }

    public void registerUndoAction(
            TransactionUndoAction undoAction
    ) {

        requireActiveTransaction();

        undoActions.addLast(
                Objects.requireNonNull(
                        undoAction,
                        "UndoAction cannot be null."
                )
        );
    }

    public int getUndoActionCount() {
        return undoActions.size();
    }

    public int getSavepointCount() {
        return savepoints.size();
    }

    public List<TransactionSavepointInfo> getSavepoints() {

        if (!hasActiveTransaction()) {
            return List.of();
        }

        List<TransactionSavepointInfo> result =
                new ArrayList<>();

        int ordinal = 1;

        for (NamedSavepoint savepoint : savepoints) {
            result.add(
                    new TransactionSavepointInfo(
                            ordinal,
                            savepoint.name(),
                            savepoint.undoActionCount()
                    )
            );

            ordinal++;
        }

        return List.copyOf(
                result
        );
    }

    public int createSavepoint() {

        requireActiveTransaction();

        return undoActions.size();
    }

    public void createSavepoint(
            String savepointName
    ) {

        requireActiveTransaction();

        String normalizedName =
                normalizeSavepointName(
                        savepointName
                );

        removeSavepoint(
                normalizedName
        );

        savepoints.addLast(
                new NamedSavepoint(
                        savepointName.trim(),
                        normalizedName,
                        undoActions.size()
                )
        );
    }

    public void rollbackToSavepoint(
            int savepoint
    ) {

        requireActiveTransaction();

        if (savepoint < 0
                || savepoint > undoActions.size()) {

            throw new IllegalArgumentException(
                    "Invalid transaction savepoint: "
                            + savepoint
            );
        }

        rollbackUndoActionsUntil(
                savepoint
        );

        removeSavepointsAfter(
                savepoint
        );
    }

    public void rollbackToSavepoint(
            String savepointName
    ) {

        requireActiveTransaction();

        NamedSavepoint savepoint =
                requireSavepoint(
                        savepointName
                );

        rollbackUndoActionsUntil(
                savepoint.undoActionCount()
        );

        removeSavepointsAfter(
                savepoint.undoActionCount()
        );
    }

    public void releaseSavepoint(
            String savepointName
    ) {

        requireActiveTransaction();

        String normalizedName =
                normalizeSavepointName(
                        savepointName
                );

        if (!removeSavepoint(
                normalizedName
        )) {
            throw new SavepointNotFoundException(
                    "Savepoint not found: "
                            + savepointName
            );
        }
    }

    public boolean hasActiveTransaction() {
        return getActiveTransaction()
                .isPresent();
    }

    public boolean isReadOnlyTransactionActive() {
        return getActiveTransaction()
                .map(TransactionContext::isReadOnly)
                .orElse(false);
    }

    private TransactionContext requireActiveTransaction() {

        if (activeTransaction == null
                || !activeTransaction.isActive()) {

            throw new NoActiveTransactionException(
                    "No active transaction exists."
            );
        }

        return activeTransaction;
    }

    private void rollbackUndoActions() {

        rollbackUndoActionsUntil(
                0
        );

        savepoints.clear();
    }

    private void rollbackUndoActionsUntil(
            int targetSize
    ) {

        RuntimeException firstFailure =
                null;

        while (undoActions.size() > targetSize) {

            TransactionUndoAction undoAction =
                    undoActions.removeLast();

            try {

                undoAction.rollback();

            } catch (RuntimeException exception) {

                if (firstFailure == null) {
                    firstFailure = exception;
                }
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    private NamedSavepoint requireSavepoint(
            String savepointName
    ) {

        String normalizedName =
                normalizeSavepointName(
                        savepointName
                );

        Iterator<NamedSavepoint> iterator =
                savepoints.descendingIterator();

        while (iterator.hasNext()) {
            NamedSavepoint savepoint =
                    iterator.next();

            if (savepoint.normalizedName()
                    .equals(
                            normalizedName
                    )) {
                return savepoint;
            }
        }

        throw new SavepointNotFoundException(
                "Savepoint not found: "
                        + savepointName
        );
    }

    private boolean removeSavepoint(
            String normalizedName
    ) {

        Iterator<NamedSavepoint> iterator =
                savepoints.iterator();

        while (iterator.hasNext()) {
            NamedSavepoint savepoint =
                    iterator.next();

            if (savepoint.normalizedName()
                    .equals(
                            normalizedName
                    )) {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    private void removeSavepointsAfter(
            int undoActionCount
    ) {

        savepoints.removeIf(savepoint ->
                savepoint.undoActionCount()
                        > undoActionCount
        );
    }

    private String normalizeSavepointName(
            String savepointName
    ) {

        Objects.requireNonNull(
                savepointName,
                "SavepointName cannot be null."
        );

        if (savepointName.isBlank()) {
            throw new IllegalArgumentException(
                    "Savepoint name cannot be blank."
            );
        }

        return savepointName.toLowerCase(
                Locale.ROOT
        );
    }

    private record NamedSavepoint(
            String name,
            String normalizedName,
            int undoActionCount
    ) {
    }
}
