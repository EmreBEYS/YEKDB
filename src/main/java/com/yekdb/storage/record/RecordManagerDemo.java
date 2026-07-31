package com.yekdb.storage.record;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.page.PageManager;
import com.yekdb.storage.page.PageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * RecordManager katmanının gerçek DataFile ve PageManager
 * bağlantısı üzerinden çalışma akışını gösterir.
 */
public final class RecordManagerDemo {

    private static final Path DEMO_FILE_PATH =
            Path.of(
                    "data",
                    "record-manager-demo.yekdb"
            );

    private RecordManagerDemo() {
        // Demo class cannot be instantiated.
    }

    public static void main(String[] args) {
        printHeader();

        try {
            prepareDemoFile();
            runDemo();

            System.out.println();
            System.out.println(
                    "RecordManager physical storage demo " +
                            "completed successfully."
            );
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "RecordManager demo failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private static void runDemo() throws IOException {

        try (DataFile dataFile =
                     new DataFile(DEMO_FILE_PATH)) {

            /*
             * DataFile fiziksel dosyayı açar.
             */
            dataFile.open();

            /*
             * Yeni dosyaya 128 byte DatabaseHeader yazılır.
             */
            initializeDatabaseHeader(dataFile);

            /*
             * PageManager, fiziksel sayfa okuma ve yazma işlemlerini
             * DataFile üzerinden gerçekleştirir.
             */
            PageManager pageManager =
                    new PageManager(dataFile);

            /*
             * RecordManager artık kayıtları bellekte değil,
             * PageType.DATA sayfalarında fiziksel olarak saklar.
             */
            RecordManager recordManager =
                    new RecordManager(
                            pageManager,
                            PageType.DATA
                    );

            System.out.println(
                    "[1] Physical storage initialized..."
            );

            System.out.println(
                    "Data file       : "
                            + dataFile.getFilePath().toAbsolutePath()
            );

            System.out.println(
                    "Initial pages   : "
                            + pageManager.getPageCount()
            );

            printSeparator();

            createAndInsertRows(
                    recordManager,
                    pageManager
            );

            readRows(recordManager);
            updateFirstRecord(recordManager);
            deleteSecondRecord(recordManager);
            printStatistics(recordManager, pageManager);

            /*
             * Verilerin fiziksel diske aktarılması garanti edilir.
             */
            pageManager.sync();
        }

        printSeparator();

        /*
         * Dosya kapatılıp tekrar açılarak kayıtların gerçekten
         * diskten okunabildiği doğrulanır.
         */
        verifyPersistenceAfterReopen();
    }

    private static void createAndInsertRows(
            RecordManager recordManager,
            PageManager pageManager
    ) throws IOException {

        System.out.println("[2] Creating rows...");

        Row firstRow = new Row(
                List.of(
                        1,
                        "Yunus Emre",
                        21,
                        true,
                        85.75
                )
        );

        Row secondRow = new Row(
                List.of(
                        2,
                        "Ahmet",
                        34,
                        false,
                        72.50
                )
        );

        System.out.println(
                "First row        : " + firstRow
        );

        System.out.println(
                "Second row       : " + secondRow
        );

        printSeparator();

        System.out.println(
                "[3] Inserting records into physical pages..."
        );

        Record firstRecord =
                recordManager.insert(firstRow);

        Record secondRecord =
                recordManager.insert(secondRow);

        System.out.println(
                "First record     : " + firstRecord
        );

        System.out.println(
                "Second record    : " + secondRecord
        );

        System.out.println(
                "Next record ID   : "
                        + recordManager.getNextRecordId()
        );

        System.out.println(
                "Physical pages   : "
                        + pageManager.getPageCount()
        );
    }

    private static void readRows(
            RecordManager recordManager
    ) throws IOException {

        printSeparator();

        System.out.println(
                "[4] Reading rows from physical storage..."
        );

        Row firstRow =
                recordManager.getRow(0);

        Row secondRow =
                recordManager.getRow(1);

        System.out.println(
                "Restored row 0   : " + firstRow
        );

        System.out.println(
                "Restored row 1   : " + secondRow
        );

        System.out.println(
                "Row 0 valid      : "
                        + firstRow.equals(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus Emre",
                                        21,
                                        true,
                                        85.75
                                )
                        )
                )
        );

        System.out.println(
                "Row 1 valid      : "
                        + secondRow.equals(
                        new Row(
                                List.of(
                                        2,
                                        "Ahmet",
                                        34,
                                        false,
                                        72.50
                                )
                        )
                )
        );
    }

    private static void updateFirstRecord(
            RecordManager recordManager
    ) throws IOException {

        printSeparator();

        System.out.println(
                "[5] Updating first physical record..."
        );

        Row updatedRow = new Row(
                List.of(
                        1,
                        "Yunus Emre Kul",
                        22,
                        true,
                        91.25
                )
        );

        recordManager.update(
                0,
                updatedRow
        );

        Row restoredRow =
                recordManager.getRow(0);

        System.out.println(
                "Updated row      : " + restoredRow
        );

        System.out.println(
                "Update valid     : "
                        + updatedRow.equals(restoredRow)
        );
    }

    private static void deleteSecondRecord(
            RecordManager recordManager
    ) throws IOException {

        printSeparator();

        System.out.println(
                "[6] Logically deleting second record..."
        );

        recordManager.delete(1);

        System.out.println(
                "Record exists    : "
                        + recordManager.contains(1)
        );

        System.out.println(
                "Record active    : "
                        + recordManager.isActive(1)
        );
    }

    private static void printStatistics(
            RecordManager recordManager,
            PageManager pageManager
    ) throws IOException {

        printSeparator();

        System.out.println(
                "[7] Physical record statistics..."
        );

        System.out.println(
                "Total records    : "
                        + recordManager.getTotalRecordCount()
        );

        System.out.println(
                "Active records   : "
                        + recordManager.getActiveRecordCount()
        );

        System.out.println(
                "Page count       : "
                        + pageManager.getPageCount()
        );

        System.out.println(
                "All records      : "
                        + recordManager.getAllRecords()
        );

        System.out.println(
                "Active list      : "
                        + recordManager.getActiveRecords()
        );
    }

    private static void verifyPersistenceAfterReopen()
            throws IOException {

        System.out.println(
                "[8] Reopening physical data file..."
        );

        try (DataFile dataFile =
                     new DataFile(DEMO_FILE_PATH)) {

            dataFile.open();

            PageManager pageManager =
                    new PageManager(dataFile);

            RecordManager recordManager =
                    new RecordManager(
                            pageManager,
                            PageType.DATA
                    );

            Row persistedRow =
                    recordManager.getRow(0);

            System.out.println(
                    "Persisted row   : " + persistedRow
            );

            System.out.println(
                    "Record 0 active : "
                            + recordManager.isActive(0)
            );

            System.out.println(
                    "Record 1 exists : "
                            + recordManager.contains(1)
            );

            System.out.println(
                    "Record 1 active : "
                            + recordManager.isActive(1)
            );

            System.out.println(
                    "Next record ID  : "
                            + recordManager.getNextRecordId()
            );

            System.out.println(
                    "Page count      : "
                            + pageManager.getPageCount()
            );
        }
    }

    /**
     * Demo her çalıştırıldığında önceki test dosyasını siler.
     */
    private static void prepareDemoFile()
            throws IOException {

        Files.deleteIfExists(DEMO_FILE_PATH);
    }

    /**
     * Boş veri dosyasına YEKDB DatabaseHeader yazar.
     */
    private static void initializeDatabaseHeader(
            DataFile dataFile
    ) throws IOException {

        if (dataFile.size() == 0) {
            DatabaseHeader databaseHeader =
                    new DatabaseHeader();

            dataFile.write(
                    0,
                    databaseHeader.toBytes()
            );

            dataFile.sync();
        }
    }

    private static void printHeader() {
        System.out.println();
        System.out.println(
                "=========================================="
        );
        System.out.println(
                " YEKDB - Physical Record Manager Demo"
        );
        System.out.println(
                " Sprint 00-08"
        );
        System.out.println(
                "=========================================="
        );
        System.out.println();
    }

    private static void printSeparator() {
        System.out.println();
        System.out.println(
                "------------------------------------------"
        );
        System.out.println();
    }
}