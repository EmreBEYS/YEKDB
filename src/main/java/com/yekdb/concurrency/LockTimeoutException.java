package com.yekdb.concurrency;

import java.time.Duration;

/**
 * Bir lock istegi verilen bekleme suresi icinde karsilanamadiginda atilir.
 */
public final class LockTimeoutException
        extends RuntimeException {

    private final LockResource resource;
    private final LockMode requestedMode;
    private final String ownerId;
    private final Duration timeout;

    public LockTimeoutException(
            LockResource resource,
            LockMode requestedMode,
            String ownerId,
            Duration timeout
    ) {

        super(
                "Timed out waiting for "
                        + requestedMode
                        + " lock on "
                        + resource.resourceType()
                        + " "
                        + resource.resourceName()
                        + " for owner "
                        + ownerId
        );

        this.resource = resource;
        this.requestedMode = requestedMode;
        this.ownerId = ownerId;
        this.timeout = timeout;
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

    public Duration getTimeout() {
        return timeout;
    }
}
