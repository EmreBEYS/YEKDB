package com.yekdb.concurrency;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitForGraphTest {

    @Test
    void shouldDetectCycleStartingFromWaitingOwner() {

        WaitForGraph graph =
                new WaitForGraph();

        graph.replaceDependencies(
                "session-a",
                List.of("session-b")
        );

        graph.replaceDependencies(
                "session-b",
                List.of("session-a")
        );

        assertEquals(
                List.of(
                        "session-b",
                        "session-a",
                        "session-b"
                ),
                graph.findCycleFrom("session-b")
                        .orElseThrow()
        );
    }

    @Test
    void shouldRemoveWaitingOwnerDependencies() {

        WaitForGraph graph =
                new WaitForGraph();

        graph.replaceDependencies(
                "session-a",
                List.of("session-b")
        );

        graph.removeDependencies("session-a");

        assertEquals(
                0,
                graph.getWaitingOwnerCount()
        );

        assertTrue(
                graph.findCycleFrom("session-a")
                        .isEmpty()
        );
    }

    @Test
    void shouldReturnImmutableDependencySnapshot() {

        WaitForGraph graph =
                new WaitForGraph();

        graph.replaceDependencies(
                "session-b",
                List.of("session-a")
        );

        var snapshot =
                graph.snapshotDependencies();

        graph.removeDependencies("session-b");

        assertEquals(
                List.of("session-a"),
                snapshot.get("session-b")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.put(
                        "session-c",
                        List.of("session-a")
                )
        );
    }
}
