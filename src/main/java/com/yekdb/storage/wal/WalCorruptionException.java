package com.yekdb.storage.wal;

import java.io.IOException;

/**
 * Tam bir WAL kaydinin yapisal veya CRC32 dogrulamasinin bozuldugunu bildirir.
 */
public final class WalCorruptionException extends IOException {

    public WalCorruptionException(String message) {
        super(message);
    }

    public WalCorruptionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
