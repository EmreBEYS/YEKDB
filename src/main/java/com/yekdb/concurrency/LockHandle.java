package com.yekdb.concurrency;

import java.util.Objects;

/**
 * Basarili lock acquisition sonucunda donen idempotent release handle'i.
 */
public final class LockHandle
        implements AutoCloseable {

    private final LockResource resource;
    private final LockMode mode;
    private final String ownerId;
    private final Object managerIdentity;
    private final Runnable releaseAction;

    private boolean released;

    LockHandle(
            LockResource resource,
            LockMode mode,
            String ownerId,
            Object managerIdentity,
            Runnable releaseAction
    ) {

        this.resource = Objects.requireNonNull(
                resource,
                "Resource cannot be null."
        );

        this.mode = Objects.requireNonNull(
                mode,
                "Mode cannot be null."
        );

        this.ownerId = Objects.requireNonNull(
                ownerId,
                "OwnerId cannot be null."
        );

        this.managerIdentity = Objects.requireNonNull(
                managerIdentity,
                "ManagerIdentity cannot be null."
        );

        this.releaseAction = Objects.requireNonNull(
                releaseAction,
                "ReleaseAction cannot be null."
        );
    }

    public LockResource getResource() {
        return resource;
    }

    public LockMode getMode() {
        return mode;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public synchronized boolean isReleased() {
        return released;
    }

    synchronized boolean belongsTo(
            Object expectedManagerIdentity
    ) {
        return managerIdentity == expectedManagerIdentity;
    }

    @Override
    public synchronized void close() {

        if (released) {
            return;
        }

        released = true;
        releaseAction.run();
    }
}
