package com.yekdb.concurrency;

import java.util.List;
import java.util.Objects;

/**
 * Wait-for graph icinde dongu tespit edildiginde firlatilir.
 */
public final class DeadlockDetectedException
        extends RuntimeException {

    private final String ownerId;
    private final List<String> cycleOwners;

    public DeadlockDetectedException(
            String ownerId,
            List<String> cycleOwners
    ) {

        super(
                "Deadlock detected: "
                        + String.join(
                                " -> ",
                                cycleOwners
                        )
        );

        this.ownerId = Objects.requireNonNull(
                ownerId,
                "OwnerId cannot be null."
        );

        this.cycleOwners = List.copyOf(
                Objects.requireNonNull(
                        cycleOwners,
                        "Cycle owners cannot be null."
                )
        );
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getCycleOwners() {
        return cycleOwners;
    }
}
