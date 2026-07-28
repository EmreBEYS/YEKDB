package com.yekdb.storage.page;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PageManagerTest {

    private Path testDirectory;
    private Path testFilePath;

    private DataFile dataFile;
    private PageManager pageManager;

    @BeforeEach
    void setUp() throws Exception {

        testDirectory = Path.of(
                "target",
                "test-data",
                "page-manager"
        );

        testFilePath = testDirectory.resolve(
                "page-manager-test.data"
        );

        Files.createDirectories(testDirectory);
        Files.deleteIfExists(testFilePath);

        dataFile = new DataFile(testFilePath);
        dataFile.open();

        /*
         * Page alanından önce database header bulunmalıdır.
         */
        DatabaseHeader databaseHeader =
                new DatabaseHeader();

        dataFile.write(
                0,
                databaseHeader.toBytes()
        );

        pageManager =
                new PageManager(dataFile);
    }

    @AfterEach
    void tearDown() throws Exception {

        if (dataFile != null && dataFile.isOpen()) {
            dataFile.close();
        }

        Files.deleteIfExists(testFilePath);
    }

    @Test
    void shouldCalculatePageOffsetsCorrectly() {

        assertEquals(
                128,
                pageManager.calculatePageOffset(0)
        );

        assertEquals(
                4224,
                pageManager.calculatePageOffset(1)
        );

        assertEquals(
                8320,
                pageManager.calculatePageOffset(2)
        );
    }

    @Test
    void shouldInitiallyContainZeroPages() throws Exception {

        assertEquals(
                0,
                pageManager.getPageCount()
        );
    }

    @Test
    void shouldWriteFirstPage() throws Exception {

        Page page = new Page(
                0,
                PageType.DATA
        );

        pageManager.writePage(page);

        assertEquals(
                1,
                pageManager.getPageCount()
        );

        assertEquals(
                DatabaseHeader.HEADER_SIZE + Page.PAGE_SIZE,
                dataFile.size()
        );
    }

    @Test
    void shouldWriteAndReadPage() throws Exception {

        Page original = new Page(
                0,
                PageType.DATA
        );

        original.getPayload()[0] = 10;
        original.getPayload()[1] = 20;
        original.getPayload()[2] = 30;

        original.getHeader().setRecordCount(1);
        original.getHeader().setUsedBytes(3);

        pageManager.writePage(original);

        Page restored =
                pageManager.readPage(0);

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
                3,
                restored.getHeader().getUsedBytes()
        );

        assertEquals(
                10,
                restored.getPayload()[0]
        );

        assertEquals(
                20,
                restored.getPayload()[1]
        );

        assertEquals(
                30,
                restored.getPayload()[2]
        );
    }

    @Test
    void shouldWriteMultiplePagesSequentially() throws Exception {

        pageManager.writePage(
                new Page(0, PageType.DATA)
        );

        pageManager.writePage(
                new Page(1, PageType.INDEX)
        );

        pageManager.writePage(
                new Page(2, PageType.FREE)
        );

        assertEquals(
                3,
                pageManager.getPageCount()
        );

        assertEquals(
                PageType.DATA,
                pageManager.readPage(0)
                        .getHeader()
                        .getPageType()
        );

        assertEquals(
                PageType.INDEX,
                pageManager.readPage(1)
                        .getHeader()
                        .getPageType()
        );

        assertEquals(
                PageType.FREE,
                pageManager.readPage(2)
                        .getHeader()
                        .getPageType()
        );
    }

    @Test
    void shouldUpdateExistingPageWithoutIncreasingPageCount()
            throws Exception {

        Page original = new Page(
                0,
                PageType.DATA
        );

        pageManager.writePage(original);

        Page updated = new Page(
                0,
                PageType.DATA
        );

        updated.getPayload()[0] = 99;
        updated.getHeader().setRecordCount(1);
        updated.getHeader().setUsedBytes(1);

        pageManager.writePage(updated);

        assertEquals(
                1,
                pageManager.getPageCount()
        );

        Page restored =
                pageManager.readPage(0);

        assertEquals(
                99,
                restored.getPayload()[0]
        );
    }

    @Test
    void shouldDetectExistingPages() throws Exception {

        pageManager.writePage(
                new Page(0, PageType.DATA)
        );

        assertTrue(
                pageManager.pageExists(0)
        );

        assertFalse(
                pageManager.pageExists(1)
        );
    }

    @Test
    void shouldRejectNonSequentialPageWrite() {

        Page page = new Page(
                4,
                PageType.DATA
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> pageManager.writePage(page)
        );
    }

    @Test
    void shouldRejectReadingMissingPage() {

        assertThrows(
                IllegalArgumentException.class,
                () -> pageManager.readPage(0)
        );
    }

    @Test
    void shouldRejectNegativePageId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> pageManager.calculatePageOffset(-1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> pageManager.readPage(-1)
        );
    }

    @Test
    void shouldRejectOperationsWhenDataFileIsClosed()
            throws Exception {

        dataFile.close();

        assertThrows(
                IllegalStateException.class,
                () -> pageManager.getPageCount()
        );

        assertThrows(
                IllegalStateException.class,
                () -> pageManager.writePage(
                        new Page(0, PageType.DATA)
                )
        );
    }

    @Test
    void shouldRejectPageOperationsWithoutDatabaseHeader()
            throws Exception {

        dataFile.resize(0);

        assertThrows(
                IllegalStateException.class,
                () -> pageManager.getPageCount()
        );

        assertThrows(
                IllegalStateException.class,
                () -> pageManager.writePage(
                        new Page(0, PageType.DATA)
                )
        );
    }

    @Test
    void shouldRejectMisalignedDataFile() throws Exception {

        /*
         * Header sonrasına geçersiz, eksik sayfa verisi ekliyoruz.
         */
        dataFile.append(
                new byte[]{1, 2, 3}
        );

        assertThrows(
                IllegalStateException.class,
                () -> pageManager.getPageCount()
        );
    }
}