package com.yekdb.storage.wal;

import java.util.zip.CRC32;

/**
 * WAL kayitlarinin CRC32 butunluk degerini hesaplar.
 */
public final class WalChecksum {

    private WalChecksum() {
    }

    public static int crc32(
            byte[] bytes,
            int offset,
            int length
    ) {

        CRC32 crc32 = new CRC32();
        crc32.update(bytes, offset, length);

        return (int) crc32.getValue();
    }
}
