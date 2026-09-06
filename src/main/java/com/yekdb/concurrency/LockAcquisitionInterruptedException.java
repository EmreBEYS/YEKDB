package com.yekdb.concurrency;

/**
 * Lock bekleyen thread interrupt edildiginde atilir.
 */
public final class LockAcquisitionInterruptedException
        extends RuntimeException {

    private final LockResource resource;
    private final LockMode requestedMode;
    private final String ownerId;

    public LockAcquisitionInterruptedException(
            LockResource resource,
            LockMode requestedMode,
            String ownerId,
            InterruptedException cause
    ) {

        super(
                "Lock acquisition was interrupted for "
                        + resource.resourceType()
                        + " "
                        + resource.resourceName()
                        + " and owner "
                        + ownerId,
                cause
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
