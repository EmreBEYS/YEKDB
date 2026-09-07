package com.yekdb.concurrency;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sprint 00-36 Phase 4 deadlock diagnostic snapshot contract.
 */
class LockManagerSnapshotDeadlockTest {

    @Test
    void shouldCopyDeadlockHistoryAndPreserveLegacyConstructor() {

        LockManagerSnapshot legacySnapshot =
                new LockManagerSnapshot(
                        List.of(),
                        Map.of()
                );

        assertEquals(
                0,
                legacySnapshot.detectedDeadlockCount()
        );

        LockManagerSnapshot.DeadlockState state =
                new LockManagerSnapshot.DeadlockState(
                        1,
                        "session-b",
                        LockResource.table(
                                Path.of("database"),
                                "users"
                        ),
                        LockMode.EXCLUSIVE,
                        List.of(
                                "session-b",
                                "session-a",
                                "session-b"
                        )
                );

        LockManagerSnapshot snapshot =
                new LockManagerSnapshot(
                        List.of(),
                        Map.of(),
                        1,
                        List.of(state)
                );

        assertEquals(
                state,
                snapshot.recentDeadlocks().getFirst()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.recentDeadlocks().add(state)
        );
    }
}
