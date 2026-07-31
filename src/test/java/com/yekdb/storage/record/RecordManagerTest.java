package com.yekdb.storage.record;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.page.Page;
import com.yekdb.storage.page.PageManager;
import com.yekdb.storage.page.PageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordManagerTest {

    @TempDir
    Path tempDirectory;

    private Path databasePath;
    private DataFile dataFile;
    private PageManager pageManager;
    private RecordManager recordManager;

    @BeforeEach
    void setUp() throws IOException {

        databasePath =
                tempDirectory.resolve(
                        "record-manager-test.ydb"
                );

        dataFile =
                new DataFile(databasePath);

        dataFile.open();

        DatabaseHeader databaseHeader =
                new DatabaseHeader();

        dataFile.write(
                0,
                databaseHeader.toBytes()
        );

        pageManager =
                new PageManager(dataFile);

        recordManager =
                new RecordManager(
                        pageManager,
                        PageType.DATA
                );
    }

    @AfterEach
    void tearDown() throws IOException {

        if (dataFile != null
                && dataFile.isOpen()) {

            dataFile.close();
        }
    }

    @Test
    void shouldStartWithNoRecords() throws IOException {

        assertEquals(
                0,
                recordManager.getTotalRecordCount()
        );

        assertEquals(
                0,
                recordManager.getActiveRecordCount()
        );

        assertEquals(
                0,
                recordManager.getNextRecordId()
        );

        assertTrue(
                recordManager.getAllRecords().isEmpty()
        );

        assertTrue(
                recordManager.getActiveRecords().isEmpty()
        );
    }

    @Test
    void shouldInsertSingleRow() throws IOException {

        Row row = createRow(
                1,
                "Emre",
                true
        );

        Record record =
                recordManager.insert(row);

        assertEquals(
                0,
                record.getRecordId()
        );

        assertFalse(record.isDeleted());

        assertEquals(
                row,
                RowSerializer.deserialize(
                        record.getData()
                )
        );

        assertEquals(
                1,
                pageManager.getPageCount()
        );

        assertEquals(
                1,
                pageManager.getHeaderPageCount()
        );
    }

    @Test
    void shouldAssignSequentialRecordIds()
            throws IOException {

        Record firstRecord =
                recordManager.insert(
                        createRow(
                                1,
                                "First",
                                true
                        )
                );

        Record secondRecord =
                recordManager.insert(
                        createRow(
                                2,
                                "Second",
                                true
                        )
                );

        Record thirdRecord =
                recordManager.insert(
                        createRow(
                                3,
                                "Third",
                                false
                        )
                );

        assertEquals(
                0,
                firstRecord.getRecordId()
        );

        assertEquals(
                1,
                secondRecord.getRecordId()
        );

        assertEquals(
                2,
                thirdRecord.getRecordId()
        );

        assertEquals(
                3,
                recordManager.getNextRecordId()
        );
    }

    @Test
    void shouldReadInsertedRecord()
            throws IOException {

        Record insertedRecord =
                recordManager.insert(
                        createRow(
                                10,
                                "YEKDB",
                                true
                        )
                );

        Record restoredRecord =
                recordManager.getRecord(
                        insertedRecord.getRecordId()
                );

        assertEquals(
                insertedRecord.getRecordId(),
                restoredRecord.getRecordId()
        );

        assertArrayEquals(
                insertedRecord.getData(),
                restoredRecord.getData()
        );

        assertFalse(
                restoredRecord.isDeleted()
        );
    }

    @Test
    void shouldReadInsertedRow()
            throws IOException {

        Row originalRow =
                createRow(
                        15,
                        "Physical Storage",
                        true
                );

        Record record =
                recordManager.insert(originalRow);

        Row restoredRow =
                recordManager.getRow(
                        record.getRecordId()
                );

        assertEquals(
                originalRow,
                restoredRow
        );
    }

    @Test
    void shouldPreserveAllSupportedRowTypes()
            throws IOException {

        Row originalRow =
                new Row(
                        List.of(
                                42,
                                9_000_000_000L,
                                95.75,
                                true,
                                "İnönü Üniversitesi"
                        )
                );

        Record record =
                recordManager.insert(originalRow);

        Row restoredRow =
                recordManager.getRow(
                        record.getRecordId()
                );

        assertEquals(
                originalRow,
                restoredRow
        );
    }

    @Test
    void shouldReportExistingRecord()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Existing",
                                true
                        )
                );

        assertTrue(
                recordManager.contains(
                        record.getRecordId()
                )
        );

        assertTrue(
                recordManager.isActive(
                        record.getRecordId()
                )
        );

        assertFalse(
                recordManager.contains(500)
        );

        assertFalse(
                recordManager.isActive(500)
        );
    }

    @Test
    void shouldReturnAllRecords()
            throws IOException {

        recordManager.insert(
                createRow(
                        1,
                        "First",
                        true
                )
        );

        recordManager.insert(
                createRow(
                        2,
                        "Second",
                        true
                )
        );

        recordManager.insert(
                createRow(
                        3,
                        "Third",
                        false
                )
        );

        List<Record> records =
                recordManager.getAllRecords();

        assertEquals(
                3,
                records.size()
        );

        assertEquals(
                0,
                records.get(0).getRecordId()
        );

        assertEquals(
                1,
                records.get(1).getRecordId()
        );

        assertEquals(
                2,
                records.get(2).getRecordId()
        );
    }

    @Test
    void shouldReturnUnmodifiableRecordLists()
            throws IOException {

        recordManager.insert(
                createRow(
                        1,
                        "Protected",
                        true
                )
        );

        List<Record> allRecords =
                recordManager.getAllRecords();

        List<Record> activeRecords =
                recordManager.getActiveRecords();

        assertThrows(
                UnsupportedOperationException.class,
                () -> allRecords.clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> activeRecords.clear()
        );
    }

    @Test
    void shouldUpdateRecordWithSameSize()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Old",
                                true
                        )
                );

        Row updatedRow =
                createRow(
                        1,
                        "New",
                        false
                );

        recordManager.update(
                record.getRecordId(),
                updatedRow
        );

        assertEquals(
                updatedRow,
                recordManager.getRow(
                        record.getRecordId()
                )
        );

        assertEquals(
                1,
                recordManager.getTotalRecordCount()
        );
    }

    @Test
    void shouldUpdateRecordWithLargerData()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "A",
                                true
                        )
                );

        Row largerRow =
                createRow(
                        1,
                        "A considerably larger updated value",
                        false
                );

        recordManager.update(
                record.getRecordId(),
                largerRow
        );

        assertEquals(
                largerRow,
                recordManager.getRow(
                        record.getRecordId()
                )
        );
    }

    @Test
    void shouldUpdateRecordWithSmallerData()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "A considerably long initial value",
                                true
                        )
                );

        Row smallerRow =
                createRow(
                        1,
                        "Short",
                        false
                );

        recordManager.update(
                record.getRecordId(),
                smallerRow
        );

        assertEquals(
                smallerRow,
                recordManager.getRow(
                        record.getRecordId()
                )
        );
    }

    @Test
    void shouldPreserveFollowingRecordAfterGrowingUpdate()
            throws IOException {

        Record firstRecord =
                recordManager.insert(
                        createRow(
                                1,
                                "A",
                                true
                        )
                );

        Row secondRow =
                createRow(
                        2,
                        "Second record",
                        false
                );

        Record secondRecord =
                recordManager.insert(secondRow);

        Row largerFirstRow =
                createRow(
                        1,
                        "This first record is now much larger",
                        true
                );

        recordManager.update(
                firstRecord.getRecordId(),
                largerFirstRow
        );

        assertEquals(
                largerFirstRow,
                recordManager.getRow(
                        firstRecord.getRecordId()
                )
        );

        assertEquals(
                secondRow,
                recordManager.getRow(
                        secondRecord.getRecordId()
                )
        );
    }

    @Test
    void shouldPreserveFollowingRecordAfterShrinkingUpdate()
            throws IOException {

        Record firstRecord =
                recordManager.insert(
                        createRow(
                                1,
                                "This is initially a long value",
                                true
                        )
                );

        Row secondRow =
                createRow(
                        2,
                        "Following record",
                        false
                );

        Record secondRecord =
                recordManager.insert(secondRow);

        Row smallerFirstRow =
                createRow(
                        1,
                        "A",
                        true
                );

        recordManager.update(
                firstRecord.getRecordId(),
                smallerFirstRow
        );

        assertEquals(
                smallerFirstRow,
                recordManager.getRow(
                        firstRecord.getRecordId()
                )
        );

        assertEquals(
                secondRow,
                recordManager.getRow(
                        secondRecord.getRecordId()
                )
        );
    }

    @Test
    void shouldLogicallyDeleteRecord()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Delete me",
                                true
                        )
                );

        recordManager.delete(
                record.getRecordId()
        );

        assertTrue(
                recordManager.contains(
                        record.getRecordId()
                )
        );

        assertFalse(
                recordManager.isActive(
                        record.getRecordId()
                )
        );

        assertEquals(
                1,
                recordManager.getTotalRecordCount()
        );

        assertEquals(
                0,
                recordManager.getActiveRecordCount()
        );
    }

    @Test
    void shouldExcludeDeletedRecordFromActiveRecords()
            throws IOException {

        Record firstRecord =
                recordManager.insert(
                        createRow(
                                1,
                                "First",
                                true
                        )
                );

        Record secondRecord =
                recordManager.insert(
                        createRow(
                                2,
                                "Second",
                                true
                        )
                );

        recordManager.delete(
                firstRecord.getRecordId()
        );

        List<Record> allRecords =
                recordManager.getAllRecords();

        List<Record> activeRecords =
                recordManager.getActiveRecords();

        assertEquals(
                2,
                allRecords.size()
        );

        assertEquals(
                1,
                activeRecords.size()
        );

        assertTrue(
                allRecords.get(0).isDeleted()
        );

        assertEquals(
                secondRecord.getRecordId(),
                activeRecords.get(0).getRecordId()
        );
    }

    @Test
    void shouldRejectReadingDeletedRecord()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Deleted",
                                true
                        )
                );

        recordManager.delete(
                record.getRecordId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> recordManager.getRecord(
                        record.getRecordId()
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> recordManager.getRow(
                        record.getRecordId()
                )
        );
    }

    @Test
    void shouldRejectUpdatingDeletedRecord()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Deleted",
                                true
                        )
                );

        recordManager.delete(
                record.getRecordId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> recordManager.update(
                        record.getRecordId(),
                        createRow(
                                1,
                                "Updated",
                                false
                        )
                )
        );
    }

    @Test
    void shouldRejectDeletingRecordTwice()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Delete once",
                                true
                        )
                );

        recordManager.delete(
                record.getRecordId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> recordManager.delete(
                        record.getRecordId()
                )
        );
    }

    @Test
    void shouldCreateMultiplePagesWhenNecessary()
            throws IOException {

        String largeValue =
                "X".repeat(2_000);

        for (int index = 0;
             index < 5;
             index++) {

            recordManager.insert(
                    new Row(
                            List.of(
                                    index,
                                    largeValue
                            )
                    )
            );
        }

        assertTrue(
                pageManager.getPageCount() >= 3
        );

        assertEquals(
                pageManager.getPageCount(),
                pageManager.getHeaderPageCount()
        );

        assertEquals(
                5,
                recordManager.getTotalRecordCount()
        );

        for (long recordId = 0;
             recordId < 5;
             recordId++) {

            assertTrue(
                    recordManager.contains(recordId)
            );
        }
    }

    @Test
    void shouldIgnorePagesWithDifferentPageType()
            throws IOException {

        Page indexPage =
                new Page(
                        0,
                        PageType.INDEX
                );

        pageManager.writePage(indexPage);

        RecordManager dataRecordManager =
                new RecordManager(
                        pageManager,
                        PageType.DATA
                );

        assertEquals(
                0,
                dataRecordManager.getTotalRecordCount()
        );

        Record record =
                dataRecordManager.insert(
                        createRow(
                                1,
                                "Data page",
                                true
                        )
                );

        assertEquals(
                0,
                record.getRecordId()
        );

        assertEquals(
                2,
                pageManager.getPageCount()
        );

        assertEquals(
                PageType.INDEX,
                pageManager.readPage(0)
                        .getHeader()
                        .getPageType()
        );

        assertEquals(
                PageType.DATA,
                pageManager.readPage(1)
                        .getHeader()
                        .getPageType()
        );
    }

    @Test
    void shouldPreserveRecordsAfterReopeningFile()
            throws IOException {

        Row firstRow =
                createRow(
                        1,
                        "Persistent first",
                        true
                );

        Row secondRow =
                createRow(
                        2,
                        "Persistent second",
                        false
                );

        Record firstRecord =
                recordManager.insert(firstRow);

        Record secondRecord =
                recordManager.insert(secondRow);

        dataFile.close();

        dataFile =
                new DataFile(databasePath);

        dataFile.open();

        pageManager =
                new PageManager(dataFile);

        recordManager =
                new RecordManager(
                        pageManager,
                        PageType.DATA
                );

        assertEquals(
                firstRow,
                recordManager.getRow(
                        firstRecord.getRecordId()
                )
        );

        assertEquals(
                secondRow,
                recordManager.getRow(
                        secondRecord.getRecordId()
                )
        );

        assertEquals(
                2,
                recordManager.getTotalRecordCount()
        );
    }

    @Test
    void shouldContinueRecordIdAfterReopeningFile()
            throws IOException {

        recordManager.insert(
                createRow(
                        1,
                        "First",
                        true
                )
        );

        recordManager.insert(
                createRow(
                        2,
                        "Second",
                        true
                )
        );

        dataFile.close();

        dataFile =
                new DataFile(databasePath);

        dataFile.open();

        pageManager =
                new PageManager(dataFile);

        recordManager =
                new RecordManager(
                        pageManager,
                        PageType.DATA
                );

        assertEquals(
                2,
                recordManager.getNextRecordId()
        );

        Record thirdRecord =
                recordManager.insert(
                        createRow(
                                3,
                                "Third",
                                true
                        )
                );

        assertEquals(
                2,
                thirdRecord.getRecordId()
        );

        assertEquals(
                3,
                recordManager.getNextRecordId()
        );
    }

    @Test
    void shouldPreserveDeletedStateAfterReopeningFile()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Persistent deletion",
                                true
                        )
                );

        recordManager.delete(
                record.getRecordId()
        );

        dataFile.close();

        dataFile =
                new DataFile(databasePath);

        dataFile.open();

        pageManager =
                new PageManager(dataFile);

        recordManager =
                new RecordManager(
                        pageManager,
                        PageType.DATA
                );

        assertTrue(
                recordManager.contains(
                        record.getRecordId()
                )
        );

        assertFalse(
                recordManager.isActive(
                        record.getRecordId()
                )
        );

        assertEquals(
                1,
                recordManager.getTotalRecordCount()
        );

        assertEquals(
                0,
                recordManager.getActiveRecordCount()
        );
    }

    @Test
    void shouldRejectNullRowDuringInsert() {

        assertThrows(
                IllegalArgumentException.class,
                () -> recordManager.insert(null)
        );
    }

    @Test
    void shouldRejectNullRowDuringUpdate()
            throws IOException {

        Record record =
                recordManager.insert(
                        createRow(
                                1,
                                "Existing",
                                true
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> recordManager.update(
                        record.getRecordId(),
                        null
                )
        );
    }

    @Test
    void shouldRejectNegativeRecordIds() {

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.getRecord(-1)
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.getRow(-1)
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.update(
                                -1,
                                createRow(
                                        1,
                                        "Invalid",
                                        true
                                )
                        )
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.delete(-1)
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.contains(-1)
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.isActive(-1)
                )
        );
    }

    @Test
    void shouldRejectMissingRecordOperations() {

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.getRecord(500)
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.getRow(500)
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.update(
                                500,
                                createRow(
                                        1,
                                        "Missing",
                                        true
                                )
                        )
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> recordManager.delete(500)
                )
        );
    }

    @Test
    void shouldRejectRecordLargerThanPagePayload()
            throws IOException {

        String oversizedValue =
                "X".repeat(
                        Page.PAYLOAD_SIZE
                );

        Row oversizedRow =
                new Row(
                        List.of(
                                oversizedValue
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> recordManager.insert(
                        oversizedRow
                )
        );

        assertEquals(
                0,
                recordManager.getTotalRecordCount()
        );
    }

    @Test
    void shouldRejectNullPageManager() {

        assertThrows(
                NullPointerException.class,
                () -> new RecordManager(
                        null,
                        PageType.DATA
                )
        );
    }

    @Test
    void shouldRejectNullRecordPageType() {

        assertThrows(
                NullPointerException.class,
                () -> new RecordManager(
                        pageManager,
                        null
                )
        );
    }

    private Row createRow(
            int id,
            String name,
            boolean active
    ) {

        return new Row(
                List.of(
                        id,
                        name,
                        active
                )
        );
    }
}