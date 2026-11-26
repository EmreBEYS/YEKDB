package com.yekdb.storage;

import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.wal.WalRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoragePhysicalWalIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldForceWalBeforePersistingPageData()
            throws Exception {

        Path dataPath = tempDirectory.resolve(
                "database.ydb"
        );
        byte[] firstDiskImage;

        try (StorageEngineHandle handle =
                     openEngine(dataPath)) {

            StorageEngine engine = handle.engine();
            Page firstPage = pageWithMarker(0, (byte) 11);

            long firstLsn = engine.writePage(
                    101,
                    firstPage
            );

            assertEquals(1, firstLsn);
            assertTrue(Files.isRegularFile(engine.getWalPath()));

            WalRecord firstRecord = engine
                    .readWalRecords()
                    .getFirst();

            assertEquals(101, firstRecord.getTransactionId());
            assertEquals(0, firstRecord.getPageId());
            assertEquals(0, firstRecord.getBeforeImage().length);
            assertEquals(
                    Page.PAGE_SIZE,
                    firstRecord.getAfterImage().length
            );

            firstDiskImage = Files.readAllBytes(dataPath);
            byte[] persistedPage = new byte[Page.PAGE_SIZE];
            System.arraycopy(
                    firstDiskImage,
                    firstDiskImage.length - Page.PAGE_SIZE,
                    persistedPage,
                    0,
                    Page.PAGE_SIZE
            );

            assertArrayEquals(
                    firstRecord.getAfterImage(),
                    persistedPage
            );

            Page updatedPage = pageWithMarker(0, (byte) 22);
            long secondLsn = engine.writePage(
                    102,
                    updatedPage
            );

            assertEquals(2, secondLsn);

            List<WalRecord> records =
                    engine.readWalRecords();

            assertEquals(2, records.size());
            assertArrayEquals(
                    records.get(0).getAfterImage(),
                    records.get(1).getBeforeImage()
            );
            assertEquals(
                    22,
                    engine.readPage(0)
                            .getPayload()[0]
            );
        }

        try (StorageEngineHandle handle =
                     openEngine(dataPath)) {

            List<WalRecord> records =
                    handle.engine().readWalRecords();

            assertEquals(2, records.size());
            assertEquals(1, records.get(0).getLsn());
            assertEquals(2, records.get(1).getLsn());
            assertEquals(
                    22,
                    handle.engine()
                            .readPage(0)
                            .getPayload()[0]
            );
        }
    }

    @Test
    void shouldProtectWritesMadeThroughStoragePageManager()
            throws Exception {

        Path dataPath = tempDirectory.resolve(
                "page-manager-path.ydb"
        );

        try (StorageEngineHandle handle =
                     openEngine(dataPath)) {

            StorageEngine engine = handle.engine();

            engine.getPageManager().writePage(
                    pageWithMarker(0, (byte) 33)
            );
            engine.getPageManager().sync();

            List<WalRecord> records =
                    engine.readWalRecords();

            assertEquals(1, records.size());
            assertEquals(0, records.getFirst().getTransactionId());
            assertEquals(0, records.getFirst().getPageId());
            assertEquals(
                    33,
                    engine.readPage(0).getPayload()[0]
            );
        }
    }

    private StorageEngineHandle openEngine(Path dataPath)
            throws Exception {

        StorageEngine engine = new StorageEngine(dataPath);
        engine.initialize();

        return new StorageEngineHandle(engine);
    }

    private Page pageWithMarker(
            int pageId,
            byte marker
    ) {

        Page page = new Page(
                pageId,
                PageType.DATA
        );
        page.getPayload()[0] = marker;
        page.getHeader().setUsedBytes(1);
        page.getHeader().setRecordCount(1);

        return page;
    }

    private record StorageEngineHandle(
            StorageEngine engine
    ) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            engine.shutdown();
        }
    }
}
