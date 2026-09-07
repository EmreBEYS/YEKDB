package com.yekdb.concurrency;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JVM icinde shared/exclusive uyumlulugunu uygulayan lock manager.
 *
 * Anlik conflict sonucu ile sureli ve interrupt edilebilir bekleme
 * davranislarini birlikte destekler.
 */
public final class InMemoryLockManager
        implements LockManager {

    private static final int DEADLOCK_HISTORY_LIMIT = 16;

    private final Map<LockResource, ResourceLockState> locks =
            new HashMap<>();

    private final WaitForGraph waitForGraph =
            new WaitForGraph();

    private final Deque<LockManagerSnapshot.DeadlockState> recentDeadlocks =
            new ArrayDeque<>();

    private long detectedDeadlockCount;

    @Override
    public LockHandle acquire(
            LockResource resource,
            LockMode mode,
            String ownerId
    ) {

        AcquisitionRequest request =
                validateRequest(
                        resource,
                        mode,
                        ownerId
                );

        synchronized (locks) {

            ResourceLockState state =
                    locks.computeIfAbsent(
                            request.resource(),
                            ignored -> new ResourceLockState()
                    );

            if (!state.canAcquireImmediately(
                    request.ownerId(),
                    request.mode()
            )) {

                throw new LockConflictException(
                        request.resource(),
                        request.mode(),
                        request.ownerId()
                );
            }

            state.acquire(
                    request.ownerId(),
                    request.mode()
            );
        }

        return createHandle(request);
    }

    @Override
    public LockHandle convert(
            LockHandle currentHandle,
            LockMode targetMode
    ) {

        LockHandle validatedHandle =
                validateConversion(
                        currentHandle,
                        targetMode
                );

        if (validatedHandle.getMode() == targetMode) {
            return validatedHandle;
        }

        LockHandle convertedHandle =
                acquire(
                        validatedHandle.getResource(),
                        targetMode,
                        validatedHandle.getOwnerId()
                );

        validatedHandle.close();
        return convertedHandle;
    }

    @Override
    public LockHandle convert(
            LockHandle currentHandle,
            LockMode targetMode,
            Duration timeout
    ) {

        Duration validatedTimeout =
                validateTimeout(timeout);

        LockHandle validatedHandle =
                validateConversion(
                        currentHandle,
                        targetMode
                );

        if (validatedHandle.getMode() == targetMode) {
            return validatedHandle;
        }

        LockHandle convertedHandle =
                acquire(
                        validatedHandle.getResource(),
                        targetMode,
                        validatedHandle.getOwnerId(),
                        validatedTimeout
                );

        validatedHandle.close();
        return convertedHandle;
    }

    @Override
    public LockHandle acquire(
            LockResource resource,
            LockMode mode,
            String ownerId,
            Duration timeout
    ) {

        AcquisitionRequest request =
                validateRequest(
                        resource,
                        mode,
                        ownerId
                );

        Duration validatedTimeout =
                validateTimeout(timeout);

        long timeoutNanos =
                toNanosSaturated(
                        validatedTimeout
                );

        long startedAt =
                System.nanoTime();

        synchronized (locks) {

            ResourceLockState state =
                    locks.computeIfAbsent(
                            request.resource(),
                            ignored -> new ResourceLockState()
                    );

            WaitingRequest waitingRequest =
                    null;

            try {

                while (true) {

                    if (waitingRequest == null
                            && state.canAcquireImmediately(
                            request.ownerId(),
                            request.mode()
                    )) {

                        state.acquire(
                                request.ownerId(),
                                request.mode()
                        );

                        break;
                    }

                    if (waitingRequest == null) {
                        waitingRequest =
                                state.enqueue(
                                        request.ownerId(),
                                        request.mode()
                                );
                    }

                    if (state.canAcquireQueued(
                            waitingRequest
                    )) {

                        state.removeWaitingRequest(
                                waitingRequest
                        );

                        state.acquire(
                                request.ownerId(),
                                request.mode()
                        );

                        rebuildWaitForGraph();
                        locks.notifyAll();
                        break;
                    }

                    long elapsedNanos =
                            System.nanoTime()
                                    - startedAt;

                    long remainingNanos =
                            timeoutNanos
                                    - elapsedNanos;

                    if (remainingNanos <= 0) {
                        throw new LockTimeoutException(
                                request.resource(),
                                request.mode(),
                                request.ownerId(),
                                validatedTimeout
                        );
                    }

                    rebuildWaitForGraph();

                    List<String> cycleOwners =
                            waitForGraph.findCycleFrom(
                                            request.ownerId()
                                    )
                                    .orElse(null);

                    if (cycleOwners != null) {

                        recordDeadlock(
                                request,
                                cycleOwners
                        );

                        throw new DeadlockDetectedException(
                                request.ownerId(),
                                cycleOwners
                        );
                    }

                    try {

                        TimeUnit.NANOSECONDS.timedWait(
                                locks,
                                remainingNanos
                        );

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                                .interrupt();

                        throw new LockAcquisitionInterruptedException(
                                request.resource(),
                                request.mode(),
                                request.ownerId(),
                                exception
                        );
                    }
                }
            } finally {

                if (waitingRequest != null) {
                    state.removeWaitingRequest(
                            waitingRequest
                    );
                }

                if (state.isEmpty()) {
                    locks.remove(
                            request.resource(),
                            state
                    );
                }

                rebuildWaitForGraph();
                locks.notifyAll();
            }
        }

        return createHandle(request);
    }

    private AcquisitionRequest validateRequest(
            LockResource resource,
            LockMode mode,
            String ownerId
    ) {

        Objects.requireNonNull(
                resource,
                "Resource cannot be null."
        );

        Objects.requireNonNull(
                mode,
                "Mode cannot be null."
        );

        String normalizedOwnerId =
                normalizeOwnerId(
                        ownerId
                );

        return new AcquisitionRequest(
                resource,
                mode,
                normalizedOwnerId
        );
    }

    private LockHandle validateConversion(
            LockHandle currentHandle,
            LockMode targetMode
    ) {

        Objects.requireNonNull(
                currentHandle,
                "CurrentHandle cannot be null."
        );

        Objects.requireNonNull(
                targetMode,
                "TargetMode cannot be null."
        );

        if (!currentHandle.belongsTo(this)) {
            throw new IllegalArgumentException(
                    "Lock handle belongs to another lock manager."
            );
        }

        if (currentHandle.isReleased()) {
            throw new IllegalStateException(
                    "Released lock handle cannot be converted."
            );
        }

        return currentHandle;
    }

    private void rebuildWaitForGraph() {

        Map<String, Set<String>> dependencies =
                new LinkedHashMap<>();

        for (ResourceLockState state : locks.values()) {

            for (WaitingRequest waitingRequest
                    : state.waitingRequests()) {

                dependencies.computeIfAbsent(
                                waitingRequest.ownerId(),
                                ignored -> new LinkedHashSet<>()
                        )
                        .addAll(
                                state.blockingOwners(
                                        waitingRequest
                                )
                        );
            }
        }

        waitForGraph.replaceAllDependencies(
                dependencies
        );
    }

    private LockHandle createHandle(
            AcquisitionRequest request
    ) {

        return new LockHandle(
                request.resource(),
                request.mode(),
                request.ownerId(),
                this,
                () -> release(
                        request.resource(),
                        request.mode(),
                        request.ownerId()
                )
        );
    }

    private Duration validateTimeout(
            Duration timeout
    ) {

        Objects.requireNonNull(
                timeout,
                "Timeout cannot be null."
        );

        if (timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Timeout cannot be negative."
            );
        }

        return timeout;
    }

    private long toNanosSaturated(
            Duration timeout
    ) {

        try {
            return timeout.toNanos();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    public int getActiveResourceCount() {

        synchronized (locks) {
            return locks.size();
        }
    }

    @Override
    public LockManagerSnapshot snapshot() {

        synchronized (locks) {

            List<LockManagerSnapshot.ResourceState> resources =
                    locks.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey(
                                    Comparator.comparing(
                                                    LockResource::databaseIdentity
                                            )
                                            .thenComparing(resource ->
                                                    resource.resourceType()
                                                            .name()
                                            )
                                            .thenComparing(
                                                    LockResource::resourceName
                                            )
                            ))
                            .map(entry -> entry.getValue()
                                    .snapshot(entry.getKey()))
                            .toList();

            return new LockManagerSnapshot(
                    resources,
                    waitForGraph.snapshotDependencies(),
                    detectedDeadlockCount,
                    List.copyOf(recentDeadlocks)
            );
        }
    }

    private void recordDeadlock(
            AcquisitionRequest request,
            List<String> cycleOwners
    ) {

        detectedDeadlockCount++;

        recentDeadlocks.addLast(
                new LockManagerSnapshot.DeadlockState(
                        detectedDeadlockCount,
                        request.ownerId(),
                        request.resource(),
                        request.mode(),
                        cycleOwners
                )
        );

        while (recentDeadlocks.size()
                > DEADLOCK_HISTORY_LIMIT) {
            recentDeadlocks.removeFirst();
        }
    }

    public int getWaitingOwnerCount() {
        return waitForGraph.getWaitingOwnerCount();
    }

    public int getHoldCount(
            LockResource resource,
            String ownerId
    ) {

        Objects.requireNonNull(
                resource,
                "Resource cannot be null."
        );

        String normalizedOwnerId =
                normalizeOwnerId(
                        ownerId
                );

        synchronized (locks) {

            ResourceLockState state =
                    locks.get(resource);

            if (state == null) {
                return 0;
            }

            return state.getHoldCount(
                    normalizedOwnerId
            );
        }
    }

    private void release(
            LockResource resource,
            LockMode mode,
            String ownerId
    ) {

        synchronized (locks) {

            ResourceLockState state =
                    locks.get(resource);

            if (state == null) {
                return;
            }

            state.release(
                    ownerId,
                    mode
            );

            if (state.isEmpty()) {
                locks.remove(resource);
            }

            rebuildWaitForGraph();
            locks.notifyAll();
        }
    }

    private String normalizeOwnerId(
            String ownerId
    ) {

        Objects.requireNonNull(
                ownerId,
                "OwnerId cannot be null."
        );

        if (ownerId.isBlank()) {
            throw new IllegalArgumentException(
                    "OwnerId cannot be blank."
            );
        }

        return ownerId.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static final class ResourceLockState {

        private final Map<String, OwnerHold> ownerHolds =
                new HashMap<>();

        private final Deque<WaitingRequest> waitingRequests =
                new ArrayDeque<>();

        private long waitingSequence;

        private boolean canAcquireImmediately(
                String ownerId,
                LockMode requestedMode
        ) {

            if (!canAcquireFromOwners(
                    ownerId,
                    requestedMode
            )) {
                return false;
            }

            return ownerHolds.containsKey(ownerId)
                    || waitingRequests.isEmpty();
        }

        private boolean canAcquireFromOwners(
                String ownerId,
                LockMode requestedMode
        ) {

            for (Map.Entry<String, OwnerHold> entry
                    : ownerHolds.entrySet()) {

                if (entry.getKey().equals(ownerId)) {
                    continue;
                }

                if (!requestedMode.isCompatibleWith(
                        entry.getValue().effectiveMode()
                )) {
                    return false;
                }
            }

            return true;
        }

        private WaitingRequest enqueue(
                String ownerId,
                LockMode mode
        ) {

            WaitingRequest waitingRequest =
                    new WaitingRequest(
                            ++waitingSequence,
                            ownerId,
                            mode
                    );

            waitingRequests.addLast(
                    waitingRequest
            );

            return waitingRequest;
        }

        private boolean canAcquireQueued(
                WaitingRequest waitingRequest
        ) {

            if (!canAcquireFromOwners(
                    waitingRequest.ownerId(),
                    waitingRequest.mode()
            )) {
                return false;
            }

            if (ownerHolds.containsKey(
                    waitingRequest.ownerId()
            )) {
                return true;
            }

            for (WaitingRequest queuedRequest
                    : waitingRequests) {

                if (queuedRequest == waitingRequest) {
                    return true;
                }

                if (queuedRequest.mode() == LockMode.EXCLUSIVE
                        || waitingRequest.mode()
                        == LockMode.EXCLUSIVE) {
                    return false;
                }
            }

            return false;
        }

        private void removeWaitingRequest(
                WaitingRequest waitingRequest
        ) {
            waitingRequests.removeFirstOccurrence(
                    waitingRequest
            );
        }

        private Iterable<WaitingRequest> waitingRequests() {
            return waitingRequests;
        }

        private void acquire(
                String ownerId,
                LockMode mode
        ) {

            OwnerHold ownerHold =
                    ownerHolds.computeIfAbsent(
                            ownerId,
                            ignored -> new OwnerHold()
                    );

            ownerHold.acquire(mode);
        }

        private List<String> blockingOwners(
                WaitingRequest waitingRequest
        ) {

            Set<String> blockingOwners =
                    new LinkedHashSet<>();

            ownerHolds.entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey()
                            .equals(waitingRequest.ownerId()))
                    .filter(entry -> !waitingRequest.mode()
                            .isCompatibleWith(
                                    entry.getValue()
                                            .effectiveMode()
                            ))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .forEach(blockingOwners::add);

            for (WaitingRequest queuedRequest
                    : waitingRequests) {

                if (queuedRequest == waitingRequest) {
                    break;
                }

                if (queuedRequest.mode() == LockMode.EXCLUSIVE
                        || waitingRequest.mode()
                        == LockMode.EXCLUSIVE) {
                    blockingOwners.add(
                            queuedRequest.ownerId()
                    );
                }
            }

            return List.copyOf(blockingOwners);
        }

        private void release(
                String ownerId,
                LockMode mode
        ) {

            OwnerHold ownerHold =
                    ownerHolds.get(ownerId);

            if (ownerHold == null) {
                return;
            }

            ownerHold.release(mode);

            if (ownerHold.isEmpty()) {
                ownerHolds.remove(ownerId);
            }
        }

        private int getHoldCount(
                String ownerId
        ) {

            OwnerHold ownerHold =
                    ownerHolds.get(ownerId);

            if (ownerHold == null) {
                return 0;
            }

            return ownerHold.getHoldCount();
        }

        private LockManagerSnapshot.ResourceState snapshot(
                LockResource resource
        ) {

            List<LockManagerSnapshot.OwnerState> owners =
                    ownerHolds.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(entry ->
                                    new LockManagerSnapshot.OwnerState(
                                            entry.getKey(),
                                            entry.getValue()
                                                    .getSharedCount(),
                                            entry.getValue()
                                                    .getExclusiveCount()
                                    )
                            )
                            .toList();

            List<LockManagerSnapshot.WaitingState> waiters =
                    new ArrayList<>();

            int queuePosition = 1;

            for (WaitingRequest waitingRequest
                    : waitingRequests) {

                waiters.add(
                        new LockManagerSnapshot.WaitingState(
                                queuePosition,
                                waitingRequest.ownerId(),
                                waitingRequest.mode()
                        )
                );

                queuePosition++;
            }

            return new LockManagerSnapshot.ResourceState(
                    resource,
                    owners,
                    waiters
            );
        }

        private boolean isEmpty() {
            return ownerHolds.isEmpty()
                    && waitingRequests.isEmpty();
        }
    }

    private record WaitingRequest(
            long sequence,
            String ownerId,
            LockMode mode
    ) {
    }

    private static final class OwnerHold {

        private int sharedCount;
        private int exclusiveCount;

        private void acquire(
                LockMode mode
        ) {

            if (mode == LockMode.SHARED) {
                sharedCount++;
            } else {
                exclusiveCount++;
            }
        }

        private void release(
                LockMode mode
        ) {

            if (mode == LockMode.SHARED
                    && sharedCount > 0) {
                sharedCount--;
            }

            if (mode == LockMode.EXCLUSIVE
                    && exclusiveCount > 0) {
                exclusiveCount--;
            }
        }

        private LockMode effectiveMode() {

            if (exclusiveCount > 0) {
                return LockMode.EXCLUSIVE;
            }

            return LockMode.SHARED;
        }

        private int getHoldCount() {
            return sharedCount + exclusiveCount;
        }

        private int getSharedCount() {
            return sharedCount;
        }

        private int getExclusiveCount() {
            return exclusiveCount;
        }

        private boolean isEmpty() {
            return getHoldCount() == 0;
        }
    }

    private record AcquisitionRequest(
            LockResource resource,
            LockMode mode,
            String ownerId
    ) {
    }
}
