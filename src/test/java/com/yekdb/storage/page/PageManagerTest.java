package com.yekdb.storage.page;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PageManagerTest {

    @TempDir
    Path tempDirectory;

    private DataFile dataFile;
    private PageManager pageManager;

    @BeforeEach
    void setUp() throws IOException {

        Path databasePath =
                tempDirectory.resolve("page-manager-test.yekdb");

        dataFile = new DataFile(databasePath);
        dataFile.open();

        DatabaseHeader databaseHeader =
                new DatabaseHeader();

        dataFile.write(
                0,
                databaseHeader.toBytes()
        );

        dataFile.sync();

        pageManager = new PageManager(dataFile);
    }

    @AfterEach
    void tearDown() throws IOException {

        if (dataFile != null
                && dataFile.isOpen()) {

            dataFile.close();
        }
    }

    @Test
    void shouldStartWithZeroPages() throws IOException {

        assertEquals(
                0,
                pageManager.getPageCount()
        );

        assertEquals(
                0,
                pageManager.getHeaderPageCount()
        );

        assertTrue(
                pageManager.isPageCountConsistent()
        );
    }

    @Test
    void shouldWriteFirstPage() throws IOException {

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
                1,
                pageManager.getHeaderPageCount()
        );

        assertTrue(
                pageManager.isPageCountConsistent()
        );
    }

    @Test
    void shouldNotIncrementHeaderCountWhenExistingPageIsUpdated()
            throws IOException {

        Page page = new Page(
                0,
                PageType.DATA
        );

        pageManager.writePage(page);

        page.getPayload()[0] = 42;
        page.getHeader().setUsedBytes(1);

        pageManager.writePage(page);

        assertEquals(
                1,
                pageManager.getPageCount()
        );

        assertEquals(
                1,
                pageManager.getHeaderPageCount()
        );

        assertTrue(
                pageManager.isPageCountConsistent()
        );
    }

    @Test
    void shouldWritePagesSequentially() throws IOException {

        Page firstPage = new Page(
                0,
                PageType.DATA
        );

        Page secondPage = new Page(
                1,
                PageType.INDEX
        );

        pageManager.writePage(firstPage);
        pageManager.writePage(secondPage);

        assertEquals(
                2,
                pageManager.getPageCount()
        );

        assertEquals(
                2,
                pageManager.getHeaderPageCount()
        );

        assertTrue(
                pageManager.isPageCountConsistent()
        );
    }

    @Test
    void shouldRejectPageGap() {

        Page invalidPage = new Page(
                2,
                PageType.DATA
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> pageManager.writePage(
                                invalidPage
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Pages must be written sequentially"
                        )
        );
    }

    @Test
    void shouldReadWrittenPage() throws IOException {

        Page originalPage = new Page(
                0,
                PageType.DATA
        );

        originalPage.getPayload()[0] = 10;
        originalPage.getPayload()[1] = 20;
        originalPage.getPayload()[2] = 30;

        originalPage.getHeader().setUsedBytes(3);
        originalPage.getHeader().setRecordCount(1);

        pageManager.writePage(originalPage);

        Page restoredPage =
                pageManager.readPage(0);

        assertEquals(
                0,
                restoredPage.getHeader().getPageId()
        );

        assertEquals(
                PageType.DATA,
                restoredPage.getHeader().getPageType()
        );

        assertEquals(
                1,
                restoredPage.getHeader().getRecordCount()
        );

        assertEquals(
                3,
                restoredPage.getHeader().getUsedBytes()
        );

        assertEquals(
                10,
                restoredPage.getPayload()[0]
        );

        assertEquals(
                20,
                restoredPage.getPayload()[1]
        );

        assertEquals(
                30,
                restoredPage.getPayload()[2]
        );
    }

    @Test
    void shouldReturnTrueWhenPageExists()
            throws IOException {

        Page page = new Page(
                0,
                PageType.DATA
        );

        pageManager.writePage(page);

        assertTrue(
                pageManager.pageExists(0)
        );
    }

    @Test
    void shouldReturnFalseWhenPageDoesNotExist()
            throws IOException {

        assertFalse(
                pageManager.pageExists(0)
        );
    }

    @Test
    void shouldRejectReadingMissingPage() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> pageManager.readPage(0)
                );

        assertEquals(
                "Page does not exist: 0",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNegativePageIdForRead() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> pageManager.readPage(-1)
                );

        assertEquals(
                "Page ID cannot be negative.",
                exception.getMessage()
        );
    }

    @Test
    void shouldCalculateFirstPageOffset() {

        assertEquals(
                DatabaseHeader.HEADER_SIZE,
                pageManager.calculatePageOffset(0)
        );
    }

    @Test
    void shouldCalculateSecondPageOffset() {

        long expectedOffset =
                DatabaseHeader.HEADER_SIZE
                        + Page.PAGE_SIZE;

        assertEquals(
                expectedOffset,
                pageManager.calculatePageOffset(1)
        );
    }

    @Test
    void shouldRejectNullPage() {

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> pageManager.writePage(null)
                );

        assertEquals(
                "Page cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullDataFile() {

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new PageManager(null)
                );

        assertEquals(
                "Data file cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldFailWhenDataFileIsClosed()
            throws IOException {

        dataFile.close();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> pageManager.getPageCount()
                );

        assertEquals(
                "Data file must be open before page operations.",
                exception.getMessage()
        );
    }

    @Test
    void shouldPersistHeaderPageCountAfterReopen()
            throws IOException {

        Page firstPage = new Page(
                0,
                PageType.DATA
        );

        Page secondPage = new Page(
                1,
                PageType.DATA
        );

        pageManager.writePage(firstPage);
        pageManager.writePage(secondPage);
        pageManager.sync();

        Path filePath =
                dataFile.getFilePath();

        dataFile.close();

        dataFile = new DataFile(filePath);
        dataFile.open();

        pageManager =
                new PageManager(dataFile);

        assertEquals(
                2,
                pageManager.getPageCount()
        );

        assertEquals(
                2,
                pageManager.getHeaderPageCount()
        );

        assertTrue(
                pageManager.isPageCountConsistent()
        );
    }
}