package com.yekdb.concurrency;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Lock manager'in belirli bir andaki immutable tanilama gorunumudur.
 */
public record LockManagerSnapshot(
        List<ResourceState> resources,
        Map<String, List<String>> waitForDependencies
) {

    public LockManagerSnapshot {

        resources = List.copyOf(
                Objects.requireNonNull(
                        resources,
                        "Resources cannot be null."
                )
        );

        Objects.requireNonNull(
                waitForDependencies,
                "WaitForDependencies cannot be null."
        );

        Map<String, List<String>> dependencyCopy =
                new TreeMap<>();

        waitForDependencies.forEach((ownerId, blockers) ->
                dependencyCopy.put(
                        ownerId,
                        List.copyOf(blockers)
                )
        );

        waitForDependencies =
                Collections.unmodifiableMap(
                        dependencyCopy
                );
    }

    public int getActiveResourceCount() {
        return resources.size();
    }

    public int getWaitingRequestCount() {

        return resources.stream()
                .mapToInt(resource ->
                        resource.waitingRequests()
                                .size()
                )
                .sum();
    }

    public int getWaitingOwnerCount() {
        return waitForDependencies.size();
    }

    public record ResourceState(
            LockResource resource,
            List<OwnerState> owners,
            List<WaitingState> waitingRequests
    ) {

        public ResourceState {

            Objects.requireNonNull(
                    resource,
                    "Resource cannot be null."
            );

            owners = List.copyOf(
                    Objects.requireNonNull(
                            owners,
                            "Owners cannot be null."
                    )
            );

            waitingRequests = List.copyOf(
                    Objects.requireNonNull(
                            waitingRequests,
                            "WaitingRequests cannot be null."
                    )
            );
        }
    }

    public record OwnerState(
            String ownerId,
            int sharedHoldCount,
            int exclusiveHoldCount
    ) {

        public OwnerState {

            Objects.requireNonNull(
                    ownerId,
                    "OwnerId cannot be null."
            );

            if (sharedHoldCount < 0
                    || exclusiveHoldCount < 0) {
                throw new IllegalArgumentException(
                        "Hold counts cannot be negative."
                );
            }
        }

        public int getHoldCount() {
            return sharedHoldCount
                    + exclusiveHoldCount;
        }

        public LockMode getEffectiveMode() {

            if (exclusiveHoldCount > 0) {
                return LockMode.EXCLUSIVE;
            }

            return LockMode.SHARED;
        }
    }

    public record WaitingState(
            int queuePosition,
            String ownerId,
            LockMode requestedMode
    ) {

        public WaitingState {

            if (queuePosition <= 0) {
                throw new IllegalArgumentException(
                        "Queue position must be positive."
                );
            }

            Objects.requireNonNull(
                    ownerId,
                    "OwnerId cannot be null."
            );

            Objects.requireNonNull(
                    requestedMode,
                    "RequestedMode cannot be null."
            );
        }
    }
}
