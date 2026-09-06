package com.yekdb.concurrency;

/**
 * Bir lock istegi baska bir owner'in uyumsuz kilidiyle cakistiginda atilir.
 */
public final class LockConflictException
        extends RuntimeException {

    private final LockResource resource;
    private final LockMode requestedMode;
    private final String ownerId;

    public LockConflictException(
            LockResource resource,
            LockMode requestedMode,
            String ownerId
    ) {

        super(
                "Lock conflict for "
                        + resource.resourceType()
                        + " "
                        + resource.resourceName()
                        + ": requested "
                        + requestedMode
                        + " by owner "
                        + ownerId
        );

        this.resource = resource;
        this.requestedMode = requestedMode;
        this.ownerId = ownerId;
    }

    public LockResource getResource() {
        return resource;
    }

    public LockMode getRequestedMode() {
        return requestedMode;
    }

    public String getOwnerId() {
        return ownerId;
    }
}
