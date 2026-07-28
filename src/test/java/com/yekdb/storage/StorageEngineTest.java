package com.yekdb.storage;

import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.page.Page;
import com.yekdb.storage.page.PageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageEngineTest {

    private Path testDirectory;
    private Path databaseFile;

    private StorageEngine storageEngine;

    @BeforeEach
    void setUp() throws Exception {

        testDirectory = Path.of(
                "target",
                "test-storage-engine"
        );

        Files.createDirectories(testDirectory);

        databaseFile = testDirectory.resolve(
                "storage-engine.ydb"
        );

        Files.deleteIfExists(databaseFile);

        storageEngine =
                new StorageEngine(databaseFile);

        storageEngine.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {

        if (storageEngine != null &&
                storageEngine.isInitialized()) {

            storageEngine.shutdown();
        }

        Files.deleteIfExists(databaseFile);
    }

    @Test
    void shouldInitializeStorageEngine() {

        assertTrue(storageEngine.isInitialized());
    }

    @Test
    void shouldCreateDatabaseHeader() {

        DatabaseHeader header =
                storageEngine.getDatabaseHeader();

        assertNotNull(header);

        assertEquals(
                DatabaseHeader.CURRENT_VERSION,
                header.getVersion()
        );

        assertEquals(
                Page.PAGE_SIZE,
                header.getPageSize()
        );
    }

    @Test
    void shouldInitiallyContainZeroPages()
            throws Exception {

        assertEquals(
                0,
                storageEngine.getPageCount()
        );
    }

    @Test
    void shouldWriteAndReadPage()
            throws Exception {

        Page page =
                new Page(
                        0,
                        PageType.DATA
                );

        byte[] message =
                "Hello YEKDB"
                        .getBytes(StandardCharsets.UTF_8);

        System.arraycopy(
                message,
                0,
                page.getPayload(),
                0,
                message.length
        );

        page.getHeader().setRecordCount(1);
        page.getHeader().setUsedBytes(message.length);

        storageEngine.writePage(page);

        Page restored =
                storageEngine.readPage(0);

        assertEquals(
                PageType.DATA,
                restored.getHeader().getPageType()
        );

        assertEquals(
                1,
                restored.getHeader().getRecordCount()
        );

        String restoredText =
                new String(
                        restored.getPayload(),
                        0,
                        restored.getHeader().getUsedBytes(),
                        StandardCharsets.UTF_8
                );

        assertEquals(
                "Hello YEKDB",
                restoredText
        );
    }

    @Test
    void shouldIncreasePageCount()
            throws Exception {

        storageEngine.writePage(
                new Page(
                        0,
                        PageType.DATA
                )
        );

        storageEngine.writePage(
                new Page(
                        1,
                        PageType.DATA
                )
        );

        assertEquals(
                2,
                storageEngine.getPageCount()
        );
    }

    @Test
    void shouldUpdateDatabaseHeader()
            throws Exception {

        storageEngine.writePage(
                new Page(
                        0,
                        PageType.DATA
                )
        );

        DatabaseHeader header =
                storageEngine.getDatabaseHeader();

        assertEquals(
                1,
                header.getTotalPages()
        );
    }

    @Test
    void shouldDetectExistingPage()
            throws Exception {

        storageEngine.writePage(
                new Page(
                        0,
                        PageType.DATA
                )
        );

        assertTrue(
                storageEngine.pageExists(0)
        );

        assertFalse(
                storageEngine.pageExists(1)
        );
    }

    @Test
    void shouldCheckpointSuccessfully()
            throws Exception {

        long before =
                storageEngine
                        .getDatabaseHeader()
                        .getLastCheckpoint();

        Thread.sleep(5);

        storageEngine.checkpoint();

        long after =
                storageEngine
                        .getDatabaseHeader()
                        .getLastCheckpoint();

        assertTrue(after > before);
    }

    @Test
    void shouldReturnDatabaseFileSize()
            throws Exception {

        storageEngine.writePage(
                new Page(
                        0,
                        PageType.DATA
                )
        );

        assertEquals(
                DatabaseHeader.HEADER_SIZE
                        + Page.PAGE_SIZE,
                storageEngine.getFileSize()
        );
    }

    @Test
    void shouldShutdownCorrectly()
            throws Exception {

        storageEngine.shutdown();

        assertFalse(
                storageEngine.isInitialized()
        );
    }

    @Test
    void shouldRejectOperationsAfterShutdown()
            throws Exception {

        storageEngine.shutdown();

        assertThrows(
                IllegalStateException.class,
                () -> storageEngine.getPageCount()
        );

        assertThrows(
                IllegalStateException.class,
                () -> storageEngine.writePage(
                        new Page(
                                0,
                                PageType.DATA
                        )
                )
        );
    }
}