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
        Map<String, List<String>> waitForDependencies,
        long detectedDeadlockCount,
        List<DeadlockState> recentDeadlocks
) {

    public LockManagerSnapshot {

        if (detectedDeadlockCount < 0) {
            throw new IllegalArgumentException(
                    "DetectedDeadlockCount cannot be negative."
            );
        }

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

        recentDeadlocks = List.copyOf(
                Objects.requireNonNull(
                        recentDeadlocks,
                        "RecentDeadlocks cannot be null."
                )
        );
    }

    /**
     * Sprint 00-35 source compatibility constructor.
     */
    public LockManagerSnapshot(
            List<ResourceState> resources,
            Map<String, List<String>> waitForDependencies
    ) {
        this(
                resources,
                waitForDependencies,
                0,
                List.of()
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

    public record DeadlockState(
            long detectionSequence,
            String victimOwnerId,
            LockResource waitingResource,
            LockMode requestedMode,
            List<String> cycleOwners
    ) {

        public DeadlockState {

            if (detectionSequence <= 0) {
                throw new IllegalArgumentException(
                        "DetectionSequence must be positive."
                );
            }

            Objects.requireNonNull(
                    victimOwnerId,
                    "VictimOwnerId cannot be null."
            );

            Objects.requireNonNull(
                    waitingResource,
                    "WaitingResource cannot be null."
            );

            Objects.requireNonNull(
                    requestedMode,
                    "RequestedMode cannot be null."
            );

            cycleOwners = List.copyOf(
                    Objects.requireNonNull(
                            cycleOwners,
                            "CycleOwners cannot be null."
                    )
            );
        }
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
