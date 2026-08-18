package com.yekdb.core;

import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YekdbEngineTest {

    private Path testDirectory;
    private Path databaseFile;

    private YekdbEngine engine;

    @BeforeEach
    void setUp() throws Exception {

        testDirectory = Path.of(
                "target",
                "test-yekdb-engine"
        );

        Files.createDirectories(testDirectory);

        databaseFile = testDirectory.resolve(
                "yekdb-engine-test.ydb"
        );

        Files.deleteIfExists(databaseFile);

        engine = new YekdbEngine(databaseFile);
    }

    @AfterEach
    void tearDown() throws Exception {

        if (engine != null && engine.isRunning()) {
            engine.shutdown();
        }

        Files.deleteIfExists(databaseFile);
    }

    @Test
    void shouldStartEngine() throws Exception {

        engine.start();

        assertTrue(engine.isRunning());
    }

    @Test
    void shouldShutdownEngine() throws Exception {

        engine.start();
        engine.shutdown();

        assertFalse(engine.isRunning());
    }

    @Test
    void shouldCreateDatabaseHeaderWhenStarted()
            throws Exception {

        engine.start();

        DatabaseHeader header =
                engine.getDatabaseHeader();

        assertNotNull(header);

        assertEquals(
                DatabaseHeader.CURRENT_VERSION,
                header.getVersion()
        );

        assertEquals(
                Page.PAGE_SIZE,
                header.getPageSize()
        );

        assertEquals(
                0,
                header.getTotalPages()
        );
    }

    @Test
    void shouldInitiallyContainZeroPages()
            throws Exception {

        engine.start();

        assertEquals(
                0,
                engine.getPageCount()
        );
    }

    @Test
    void shouldWriteAndReadPage()
            throws Exception {

        engine.start();

        Page page = new Page(
                0,
                PageType.DATA
        );

        byte[] message =
                "YEKDB Engine Test"
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

        engine.writePage(page);

        Page restored =
                engine.readPage(0);

        assertEquals(
                0,
                restored.getHeader().getPageId()
        );

        assertEquals(
                PageType.DATA,
                restored.getHeader().getPageType()
        );

        assertEquals(
                1,
                restored.getHeader().getRecordCount()
        );

        assertEquals(
                message.length,
                restored.getHeader().getUsedBytes()
        );

        String restoredText =
                new String(
                        restored.getPayload(),
                        0,
                        restored.getHeader().getUsedBytes(),
                        StandardCharsets.UTF_8
                );

        assertEquals(
                "YEKDB Engine Test",
                restoredText
        );
    }

    @Test
    void shouldIncreasePageCountAfterWritingPage()
            throws Exception {

        engine.start();

        engine.writePage(
                new Page(0, PageType.DATA)
        );

        engine.writePage(
                new Page(1, PageType.INDEX)
        );

        assertEquals(
                2,
                engine.getPageCount()
        );

        assertEquals(
                2,
                engine.getDatabaseHeader().getTotalPages()
        );
    }

    @Test
    void shouldDetectExistingPage()
            throws Exception {

        engine.start();

        engine.writePage(
                new Page(0, PageType.DATA)
        );

        assertTrue(
                engine.pageExists(0)
        );

        assertFalse(
                engine.pageExists(1)
        );
    }

    @Test
    void shouldCreateCheckpoint()
            throws Exception {

        engine.start();

        long before =
                engine.getDatabaseHeader()
                        .getLastCheckpoint();

        Thread.sleep(5);

        engine.checkpoint();

        long after =
                engine.getDatabaseHeader()
                        .getLastCheckpoint();

        assertTrue(after > before);
    }

    @Test
    void shouldReturnCorrectDatabaseFileSize()
            throws Exception {

        engine.start();

        engine.writePage(
                new Page(0, PageType.DATA)
        );

        assertEquals(
                DatabaseHeader.HEADER_SIZE
                        + Page.PAGE_SIZE,
                engine.getDataFileSize()
        );
    }

    @Test
    void shouldRejectOperationsBeforeStart() {

        assertThrows(
                IllegalStateException.class,
                () -> engine.getPageCount()
        );

        assertThrows(
                IllegalStateException.class,
                () -> engine.writePage(
                        new Page(0, PageType.DATA)
                )
        );
    }

    @Test
    void shouldNotCreateDuplicatePagesWhenRestarted()
            throws Exception {

        engine.start();

        engine.writePage(
                new Page(0, PageType.DATA)
        );

        engine.shutdown();

        engine = new YekdbEngine(databaseFile);

        engine.start();

        assertEquals(
                1,
                engine.getPageCount()
        );

        assertTrue(
                engine.pageExists(0)
        );

        assertEquals(
                1,
                engine.getDatabaseHeader().getTotalPages()
        );
    }
}