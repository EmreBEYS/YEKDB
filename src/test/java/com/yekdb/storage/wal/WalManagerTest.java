package com.yekdb.storage.wal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalManagerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldPersistBinaryPageImagesAndMetadata()
            throws Exception {

        Path walPath = tempDirectory.resolve(
                "physical.wal"
        );

        byte[] beforeImage = {1, 2, 3};
        byte[] afterImage = {4, 5, 6, 7};

        try (WalManager manager = new WalManager(walPath)) {
            manager.open();

            long lsn = manager.appendPageUpdate(
                    91,
                    12,
                    beforeImage,
                    afterImage
            );

            assertEquals(1, lsn);

            WalRecord record = manager
                    .readAll()
                    .getFirst();

            assertEquals(1, record.getLsn());
            assertEquals(91, record.getTransactionId());
            assertEquals(12, record.getPageId());
            assertEquals(
                    WalRecordType.PAGE_UPDATE,
                    record.getType()
            );
            assertArrayEquals(
                    beforeImage,
                    record.getBeforeImage()
            );
            assertArrayEquals(
                    afterImage,
                    record.getAfterImage()
            );
        }

        assertEquals(
                WalManager.FILE_HEADER_SIZE
                        + WalWriter.FIXED_RECORD_SIZE
                        + beforeImage.length
                        + afterImage.length,
                Files.size(walPath)
        );
    }

    @Test
    void shouldContinueMonotonicLsnAfterRestart()
            throws Exception {

        Path walPath = tempDirectory.resolve(
                "restart.wal"
        );

        try (WalManager manager = new WalManager(walPath)) {
            manager.open();
            assertEquals(
                    1,
                    manager.appendPageUpdate(
                            1,
                            0,
                            new byte[0],
                            new byte[]{1}
                    )
            );
        }

        try (WalManager manager = new WalManager(walPath)) {
            manager.open();
            assertEquals(
                    2,
                    manager.appendPageUpdate(
                            2,
                            0,
                            new byte[]{1},
                            new byte[]{2}
                    )
            );

            List<WalRecord> records = manager.readAll();

            assertEquals(2, records.size());
            assertEquals(1, records.get(0).getLsn());
            assertEquals(2, records.get(1).getLsn());
        }
    }

    @Test
    void shouldDiscardTornTailAndKeepDurablePrefix()
            throws Exception {

        Path walPath = tempDirectory.resolve(
                "torn-tail.wal"
        );
        long durableLength;

        try (WalManager manager = new WalManager(walPath)) {
            manager.open();
            manager.appendPageUpdate(
                    7,
                    3,
                    new byte[0],
                    new byte[]{9, 8, 7}
            );
            durableLength = Files.size(walPath);
        }

        Files.write(
                walPath,
                new byte[]{0x57, 0x41, 0x4c},
                StandardOpenOption.APPEND
        );

        try (WalManager manager = new WalManager(walPath)) {
            manager.open();

            assertEquals(
                    1,
                    manager.readAll().size()
            );
            assertEquals(
                    durableLength,
                    Files.size(walPath)
            );
            assertEquals(
                    2,
                    manager.appendPageUpdate(
                            8,
                            3,
                            new byte[]{9, 8, 7},
                            new byte[]{6}
                    )
            );
        }
    }

    @Test
    void shouldRejectCompleteRecordWithInvalidCrc32()
            throws Exception {

        Path walPath = tempDirectory.resolve(
                "corrupted.wal"
        );

        try (WalManager manager = new WalManager(walPath)) {
            manager.open();
            manager.appendPageUpdate(
                    4,
                    2,
                    new byte[]{1},
                    new byte[]{2}
            );
        }

        try (RandomAccessFile file = new RandomAccessFile(
                walPath.toFile(),
                "rw"
        )) {
            long checksumByte = file.length() - 1;
            file.seek(checksumByte);
            int original = file.readUnsignedByte();
            file.seek(checksumByte);
            file.writeByte(original ^ 0xFF);
        }

        WalManager manager = new WalManager(walPath);

        assertThrows(
                WalCorruptionException.class,
                manager::open
        );
    }
}
