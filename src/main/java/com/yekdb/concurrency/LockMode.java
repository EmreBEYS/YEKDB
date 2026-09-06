package com.yekdb.concurrency;

/**
 * Bir concurrency kaynagi uzerinde alinabilen temel kilit modlari.
 */
public enum LockMode {

    SHARED,
    EXCLUSIVE;

    public boolean isCompatibleWith(
            LockMode other
    ) {

        return this == SHARED
                && other == SHARED;
    }
}
