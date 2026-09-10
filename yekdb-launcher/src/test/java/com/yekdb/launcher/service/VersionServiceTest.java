package com.yekdb.launcher.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionServiceTest {
    private final VersionService service = new VersionService();

    @Test void recognizesNewerVersions() {
        assertTrue(service.isNewer("v1.2.0", "1.1.9"));
        assertTrue(service.isNewer("2.0", "1.99.99"));
    }

    @Test void rejectsSameAndOlderVersions() {
        assertFalse(service.isNewer("v1.0.0", "1.0"));
        assertFalse(service.isNewer("0.9.9", "1.0.0"));
    }
}
