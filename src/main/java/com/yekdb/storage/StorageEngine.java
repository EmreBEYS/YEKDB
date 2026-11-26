package com.yekdb.storage;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageManager;
import com.yekdb.storage.record.page.PageSerializer;
import com.yekdb.storage.wal.WalManager;
import com.yekdb.storage.wal.WalRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB depolama alt sisteminin ana yöneticisidir.
 *
 * Sorumlulukları:
 * - veri dosyasını açmak ve kapatmak,
 * - yeni veritabanı header'ı oluşturmak,
 * - mevcut database header'ını okumak ve doğrulamak,
 * - PageManager yaşam döngüsünü yönetmek,
 * - fiziksel sayfa yazma ve okuma işlemlerini yürütmek.
 */
public final class StorageEngine {

    private final DataFile dataFile;

    private DatabaseHeader databaseHeader;

    private PageManager pageManager;

    private WalManager walManager;

    private boolean initialized;

    public StorageEngine(Path dataFilePath) {

        Objects.requireNonNull(
                dataFilePath,
                "Data file path cannot be null."
        );

        this.dataFile = new DataFile(dataFilePath);
        this.initialized = false;
    }

    /**
     * Storage Engine'i başlatır.
     *
     * Boş dosyada yeni bir DatabaseHeader oluşturulur.
     * Mevcut dosyada header okunur ve doğrulanır.
     */
    public void initialize() throws IOException {

        if (initialized) {
            return;
        }

        try {
            dataFile.open();

            if (dataFile.size() == 0) {
                createNewDatabase();
            } else {
                openExistingDatabase();
            }

            walManager = new WalManager(
                    resolveWalPath()
            );
            walManager.open();

            pageManager = new PageManager(
                    dataFile,
                    new PageSerializer(),
                    walManager
            );

            validatePhysicalPageCount();

            initialized = true;

        } catch (Exception exception) {

            closeAfterInitializationFailure();

            if (exception instanceof IOException ioException) {
                throw ioException;
            }

            throw exception;
        }
    }

    /**
     * Storage Engine'i güvenli biçimde kapatır.
     */
    public void shutdown() throws IOException {

        if (!initialized && !dataFile.isOpen()) {
            return;
        }

        IOException failure = null;

        try {
            if (dataFile.isOpen()) {
                dataFile.sync();
            }
        } catch (IOException exception) {
            failure = exception;
        }

        try {
            if (dataFile.isOpen()) {
                dataFile.close();
            }
        } catch (IOException exception) {

            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }

        try {
            if (walManager != null
                    && walManager.isOpen()) {
                walManager.close();
            }
        } catch (IOException exception) {

            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        } finally {
            initialized = false;
            pageManager = null;
            walManager = null;
            databaseHeader = null;
        }

        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Sayfayı fiziksel veri dosyasına yazar.
     *
     * Yeni bir sayfa eklenirse DatabaseHeader içerisindeki
     * totalPages değeri otomatik güncellenir.
     */
    public void writePage(Page page) throws IOException {

        writePage(
                0,
                page
        );
    }

    /**
     * Sayfa degisikligini once fiziksel WAL'a kalici olarak yazar,
     * ardindan veri dosyasina uygular.
     *
     * @param transactionId degisikligi yapan transaction kimligi;
     *                      sistem yazimlari icin 0
     * @param page          yazilacak fiziksel sayfa
     * @return kayda ayrilan monoton LSN
     */
    public long writePage(
            long transactionId,
            Page page
    ) throws IOException {

        ensureInitialized();

        Objects.requireNonNull(
                page,
                "Page cannot be null."
        );

        int pageId = page.getHeader().getPageId();

        boolean newPage =
                !pageManager.pageExists(pageId);

        long lsn = pageManager.writePage(
                transactionId,
                page
        );

        if (newPage) {
            databaseHeader = readDatabaseHeader();
        }

        dataFile.sync();

        return lsn;
    }

    /**
     * Belirtilen fiziksel sayfayı diskten okur.
     */
    public Page readPage(int pageId) throws IOException {

        ensureInitialized();

        return pageManager.readPage(pageId);
    }

    /**
     * Sayfanın veri dosyasında bulunup bulunmadığını döndürür.
     */
    public boolean pageExists(int pageId) throws IOException {

        ensureInitialized();

        return pageManager.pageExists(pageId);
    }

    /**
     * Fiziksel sayfa sayısını döndürür.
     */
    public int getPageCount() throws IOException {

        ensureInitialized();

        return pageManager.getPageCount();
    }

    /**
     * Database header'ını diske yeniden yazar.
     */
    public void flushHeader() throws IOException {

        ensureInitialized();

        writeDatabaseHeader();
        dataFile.sync();
    }

    /**
     * Checkpoint zamanını günceller.
     */
    public void checkpoint() throws IOException {

        ensureInitialized();

        databaseHeader.updateLastCheckpoint(
                System.currentTimeMillis()
        );

        writeDatabaseHeader();
        dataFile.sync();
    }

    public long getFileSize() throws IOException {

        ensureInitialized();

        return dataFile.size();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public DatabaseHeader getDatabaseHeader() {

        ensureInitialized();

        return databaseHeader;
    }

    public PageManager getPageManager() {

        ensureInitialized();

        return pageManager;
    }

    public Path getDataFilePath() {
        return dataFile.getFilePath();
    }

    public Path getWalPath() {
        return resolveWalPath();
    }

    public List<WalRecord> readWalRecords()
            throws IOException {

        ensureInitialized();

        return walManager.readAll();
    }

    /**
     * Yeni ve boş bir YEKDB veri dosyası oluşturur.
     */
    private void createNewDatabase() throws IOException {

        databaseHeader = new DatabaseHeader();

        writeDatabaseHeader();

        dataFile.sync();
    }

    /**
     * Mevcut veri dosyasının DatabaseHeader bilgisini okur.
     */
    private void openExistingDatabase() throws IOException {

        if (dataFile.size() < DatabaseHeader.HEADER_SIZE) {
            throw new IllegalStateException(
                    "Data file is smaller than the YEKDB database header."
            );
        }

        databaseHeader =
                readDatabaseHeader();

        if (!databaseHeader.hasValidMagicNumber()) {
            throw new IllegalStateException(
                    "Invalid YEKDB database magic number."
            );
        }

        if (!databaseHeader.isCurrentVersion()) {
            throw new IllegalStateException(
                    "Unsupported YEKDB database version: "
                            + databaseHeader.getVersion()
            );
        }

        if (databaseHeader.getPageSize() != Page.PAGE_SIZE) {
            throw new IllegalStateException(
                    "Database page size does not match engine page size."
            );
        }
    }


    /**
     * DatabaseHeader bilgisini fiziksel veri dosyasından okur.
     *
     * @return Diskte saklanan database header
     */
    private DatabaseHeader readDatabaseHeader()
            throws IOException {

        byte[] headerBytes =
                dataFile.read(
                        0,
                        DatabaseHeader.HEADER_SIZE
                );

        return DatabaseHeader.fromBytes(
                headerBytes
        );
    }

    /**
     * Bellekteki DatabaseHeader bilgisini dosyanın
     * ilk 128 byte'ına yazar.
     */
    private void writeDatabaseHeader() throws IOException {

        dataFile.write(
                0,
                databaseHeader.toBytes()
        );
    }

    /**
     * Header içindeki sayfa sayısıyla fiziksel dosyadaki
     * sayfa sayısının uyumlu olduğunu doğrular.
     */
    private void validatePhysicalPageCount()
            throws IOException {

        int physicalPageCount =
                pageManager.getPageCount();

        int headerPageCount =
                databaseHeader.getTotalPages();

        if (physicalPageCount != headerPageCount) {
            throw new IllegalStateException(
                    "Database page count mismatch. Header reports "
                            + headerPageCount
                            + " pages, but the data file contains "
                            + physicalPageCount
                            + " physical pages."
            );
        }
    }

    private void ensureInitialized() {

        if (!initialized) {
            throw new IllegalStateException(
                    "StorageEngine must be initialized before use."
            );
        }
    }

    private void closeAfterInitializationFailure() {

        try {
            if (dataFile.isOpen()) {
                dataFile.close();
            }
        } catch (IOException ignored) {
            // İlk başlatma hatasını gizlememek için burada yutulur.
        }

        initialized = false;
        databaseHeader = null;
        pageManager = null;

        if (walManager != null) {
            try {
                walManager.close();
            } catch (IOException ignored) {
                // Ilk baslatma hatasini gizlememek icin yutulur.
            }
        }

        walManager = null;
    }

    private Path resolveWalPath() {

        Path dataPath = dataFile.getFilePath();
        String fileName = dataPath
                .getFileName()
                .toString();

        return dataPath.resolveSibling(
                fileName + WalManager.FILE_SUFFIX
        );
    }
}
