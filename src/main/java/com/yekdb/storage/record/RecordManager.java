package com.yekdb.storage.record;

import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageManager;
import com.yekdb.storage.record.page.PageType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YEKDB içerisindeki fiziksel kayıt işlemlerini yönetir.
 *
 * RecordManager:
 *
 * - Row nesnelerini serialize eder.
 * - Record nesneleri oluşturur.
 * - Kayıtları fiziksel sayfalara yerleştirir.
 * - PageManager üzerinden diske yazar.
 * - Diskte bulunan kayıtları tekrar okuyabilir.
 */
public final class RecordManager {

    /**
     * RecordSerializer formatında dataLength alanının başlangıç offseti.
     *
     * Record ID    : 8 byte
     * Deleted flag : 1 byte
     * Data length  : 4 byte
     */
    private static final int DATA_LENGTH_OFFSET =
            Long.BYTES + Byte.BYTES;

    private final PageManager pageManager;
    private final PageType recordPageType;
    private final AtomicLong nextRecordId;

    /**
     * Fiziksel PageManager bağlantısıyla RecordManager oluşturur.
     *
     * @param pageManager Sayfa yönetim katmanı
     * @param recordPageType Kayıtların tutulacağı sayfa tipi
     */
    public RecordManager(
            PageManager pageManager,
            PageType recordPageType
    ) throws IOException {

        this.pageManager = Objects.requireNonNull(
                pageManager,
                "Page manager cannot be null."
        );

        this.recordPageType = Objects.requireNonNull(
                recordPageType,
                "Record page type cannot be null."
        );

        this.nextRecordId = new AtomicLong(
                calculateNextRecordId()
        );
    }

    /**
     * Row nesnesini fiziksel kayıt olarak diske ekler.
     *
     * @param row Eklenecek satır
     * @return Oluşturulan Record
     */
    public synchronized Record insert(Row row)
            throws IOException {

        return insertInternal(row).record();
    }

    /**
     * Row nesnesini fiziksel kayıt olarak ekler ve oluşan
     * fiziksel RecordId bilgisini döndürür.
     *
     * Mevcut insert(Row) API'sini bozmadan page/slot adreslemesi
     * gereken katmanların kullanabilmesi için sağlanır.
     *
     * @param row Eklenecek satır
     * @return Kaydın fiziksel Page/Slot kimliği
     */
    public synchronized RecordId insertWithLocation(Row row)
            throws IOException {

        return insertInternal(row).getRecordId();
    }

    /**
     * Row nesnesini ekler ve fiziksel konum bilgisini döndürür.
     *
     * Özellikle storage testleri ve ileride index entegrasyonu için
     * kaydın page, slot, offset ve serialized size bilgilerine
     * doğrudan erişim sağlar.
     */
    public synchronized RecordLocation insertAndLocate(Row row)
            throws IOException {

        return insertInternal(row);
    }

    /**
     * Insert işleminin tek gerçek uygulamasıdır.
     *
     * Böylece insert(Row), insertWithLocation(Row) ve
     * insertAndLocate(Row) aynı fiziksel yazma yolunu kullanır.
     */
    private RecordLocation insertInternal(Row row)
            throws IOException {

        validateRow(row);

        long recordId = nextRecordId.getAndIncrement();

        byte[] rowBytes =
                RowSerializer.serialize(row);

        Record record = new Record(
                recordId,
                rowBytes
        );

        byte[] recordBytes =
                RecordSerializer.serialize(record);

        if (recordBytes.length > Page.PAYLOAD_SIZE) {
            throw new IllegalArgumentException(
                    "Serialized record exceeds page payload capacity. " +
                            "Record size: " + recordBytes.length +
                            ", page payload capacity: " +
                            Page.PAYLOAD_SIZE + "."
            );
        }

        Page targetPage = findPageWithEnoughSpace(
                recordBytes.length
        );

        int slotId =
                targetPage.getHeader().getRecordCount();

        int writeOffset =
                targetPage.getHeader().getUsedBytes();

        appendRecordToPage(
                targetPage,
                recordBytes
        );

        pageManager.writePage(targetPage);
        pageManager.sync();

        return new RecordLocation(
                targetPage,
                writeOffset,
                recordBytes.length,
                slotId,
                record
        );
    }

    /**
     * Belirtilen kimliğe sahip aktif kaydı diskten okur.
     *
     * @param recordId Kayıt kimliği
     * @return Bulunan Record
     */
    public synchronized Record getRecord(long recordId)
            throws IOException {

        validateRecordId(recordId);

        RecordLocation location =
                findRecordLocation(recordId);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Record not found: " + recordId
            );
        }

        if (location.record().isDeleted()) {
            throw new IllegalStateException(
                    "Record has been deleted: " + recordId
            );
        }

        return location.record();
    }

    /**
     * Fiziksel RecordId üzerinden aktif kaydı okur.
     *
     * Fiziksel konum doğrulaması merkezi requirePhysicalLocation(...)
     * metodu üzerinden gerçekleştirilir.
     */
    public synchronized Record readRecord(RecordId recordId)
            throws IOException {

        RecordLocation location =
                requirePhysicalLocation(recordId);

        if (location.record().isDeleted()) {
            throw new IllegalStateException(
                    "Record has been deleted at physical location: " +
                            recordId
            );
        }

        return location.record();
    }

    /**
     * Fiziksel Page/Slot kimliğine ait RecordLocation bilgisini bulur.
     *
     * <p>Bu metod bulunamayan fiziksel adresler için {@code null}
     * döndürür. Strict validation gereken read/update/delete yolları
     * {@link #requirePhysicalLocation(RecordId)} kullanır.</p>
     */
    public synchronized RecordLocation locateRecord(RecordId recordId)
            throws IOException {

        Objects.requireNonNull(
                recordId,
                "RecordId cannot be null."
        );

        if (recordId.pageId() >= pageManager.getPageCount()) {
            return null;
        }

        Page page = pageManager.readPage(
                recordId.pageId()
        );

        if (page.getHeader().getPageType()
                != recordPageType) {
            return null;
        }

        if (recordId.slotId()
                >= page.getHeader().getRecordCount()) {
            return null;
        }

        return locateRecordInPage(
                page,
                recordId.slotId()
        );
    }

    /**
     * Daha önce okunmuş bir record page içerisinde belirtilen slotu bulur.
     *
     * <p>Page nesnesinin caller tarafından doğrulanmış olması beklenir.
     * Böylece physical read/update/delete akışlarında aynı page'in iki kez
     * diskten okunması engellenir.</p>
     */
    private RecordLocation locateRecordInPage(
            Page page,
            int slotId
    ) {

        int offset = 0;
        int currentSlotId = 0;
        int usedBytes =
                page.getHeader().getUsedBytes();

        while (offset < usedBytes) {

            int serializedSize =
                    calculateRecordSize(
                            page,
                            offset
                    );

            if (currentSlotId == slotId) {

                byte[] recordBytes =
                        Arrays.copyOfRange(
                                page.getPayload(),
                                offset,
                                offset + serializedSize
                        );

                Record record =
                        RecordSerializer.deserialize(
                                recordBytes
                        );

                return new RecordLocation(
                        page,
                        offset,
                        serializedSize,
                        currentSlotId,
                        record
                );
            }

            offset += serializedSize;
            currentSlotId++;
        }

        return null;
    }

    /**
     * Mantıksal long Record ID değerini fiziksel Page/Slot kimliğine
     * dönüştürür.
     */
    public synchronized RecordId findPhysicalRecordId(long recordId)
            throws IOException {

        validateRecordId(recordId);

        RecordLocation location =
                findRecordLocation(recordId);

        return location == null
                ? null
                : location.getRecordId();
    }

    /**
     * Belirtilen kayda ait Row nesnesini diskten okur.
     *
     * @param recordId Kayıt kimliği
     * @return Deserialize edilmiş Row
     */
    public synchronized Row getRow(long recordId)
            throws IOException {

        Record record = getRecord(recordId);

        return RowSerializer.deserialize(
                record.getData()
        );
    }

    /**
     * Belirtilen kaydın verisini mantıksal Record ID üzerinden günceller.
     *
     * @param recordId Güncellenecek mantıksal kayıt kimliği
     * @param newRow Yeni satır verisi
     */
    public synchronized void update(
            long recordId,
            Row newRow
    ) throws IOException {

        validateRecordId(recordId);
        validateRow(newRow);

        RecordLocation location =
                findRecordLocation(recordId);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Record not found: " + recordId
            );
        }

        updateAtLocation(
                location,
                newRow
        );
    }

    /**
     * Belirtilen kaydın verisini fiziksel Page/Slot RecordId üzerinden günceller.
     *
     * @param recordId Güncellenecek fiziksel kayıt kimliği
     * @param newRow Yeni satır verisi
     */
    public synchronized void update(
            RecordId recordId,
            Row newRow
    ) throws IOException {

        validateRow(newRow);

        RecordLocation location =
                requirePhysicalLocation(recordId);

        updateAtLocation(
                location,
                newRow
        );
    }

    /**
     * Logical ve physical update yollarının ortak uygulamasıdır.
     */
    private void updateAtLocation(
            RecordLocation location,
            Row newRow
    ) throws IOException {

        Objects.requireNonNull(
                location,
                "RecordLocation cannot be null."
        );

        if (location.record().isDeleted()) {
            throw new IllegalStateException(
                    "Cannot update a deleted record. Record ID: " +
                            location.record().getRecordId()
            );
        }

        byte[] rowBytes =
                RowSerializer.serialize(newRow);

        Record updatedRecord = new Record(
                location.record().getRecordId(),
                rowBytes
        );

        byte[] updatedRecordBytes =
                RecordSerializer.serialize(
                        updatedRecord
                );

        int newPageUsedBytes =
                location.page().getHeader().getUsedBytes()
                        - location.serializedSize()
                        + updatedRecordBytes.length;

        if (newPageUsedBytes > Page.PAYLOAD_SIZE) {
            throw new IllegalStateException(
                    "Updated record does not fit in its current page. " +
                            "Record ID: " +
                            location.record().getRecordId() +
                            "."
            );
        }

        replaceRecordInPage(
                location,
                updatedRecordBytes
        );

        pageManager.writePage(
                location.page()
        );

        pageManager.sync();
    }

    /**
     * Belirtilen kaydı mantıksal Record ID üzerinden siler.
     *
     * Record fiziksel olarak sayfada tutulmaya devam eder ve deleted flag
     * true yapılır.
     *
     * @param recordId Silinecek mantıksal kayıt kimliği
     */
    public synchronized void delete(long recordId)
            throws IOException {

        validateRecordId(recordId);

        RecordLocation location =
                findRecordLocation(recordId);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Record not found: " + recordId
            );
        }

        deleteAtLocation(location);
    }

    /**
     * Belirtilen kaydı fiziksel Page/Slot RecordId üzerinden siler.
     *
     * Tombstone delete kullanıldığı için kayıt payload'dan çıkarılmaz ve
     * sonraki slot numaraları değişmez.
     *
     * @param recordId Silinecek fiziksel kayıt kimliği
     */
    public synchronized void delete(RecordId recordId)
            throws IOException {

        RecordLocation location =
                requirePhysicalLocation(recordId);

        deleteAtLocation(location);
    }

    /**
     * Logical ve physical delete yollarının ortak tombstone uygulamasıdır.
     */
    private void deleteAtLocation(
            RecordLocation location
    ) throws IOException {

        Objects.requireNonNull(
                location,
                "RecordLocation cannot be null."
        );

        Record record =
                location.record();

        if (record.isDeleted()) {
            throw new IllegalStateException(
                    "Record has already been deleted: " +
                            record.getRecordId()
            );
        }

        record.markAsDeleted();

        byte[] deletedRecordBytes =
                RecordSerializer.serialize(record);

        /*
         * Tombstone delete:
         * Serialized record boyutu değişmediği için fiziksel slot ve
         * sonraki kayıtların slot numaraları korunur.
         */
        replaceRecordInPage(
                location,
                deletedRecordBytes
        );

        pageManager.writePage(
                location.page()
        );

        pageManager.sync();
    }

    /**
     * Fiziksel RecordId değerini doğrular ve karşılık gelen
     * RecordLocation bilgisini döndürür.
     *
     * <p>Page yalnızca bir kez okunur. Page type ve slot sınırı
     * doğrulandıktan sonra aynı Page nesnesi üzerinde slot çözümlemesi
     * yapılır.</p>
     */
    private RecordLocation requirePhysicalLocation(
            RecordId recordId
    ) throws IOException {

        Objects.requireNonNull(
                recordId,
                "RecordId cannot be null."
        );

        int pageCount =
                pageManager.getPageCount();

        if (recordId.pageId() >= pageCount) {
            throw new IllegalArgumentException(
                    "Physical page does not exist: " +
                            recordId.pageId()
            );
        }

        Page page =
                pageManager.readPage(
                        recordId.pageId()
                );

        if (page.getHeader().getPageType()
                != recordPageType) {

            throw new IllegalArgumentException(
                    "Physical page is not a record page. " +
                            "Page ID: " +
                            recordId.pageId() +
                            ", actual type: " +
                            page.getHeader().getPageType()
            );
        }

        if (recordId.slotId()
                >= page.getHeader().getRecordCount()) {

            throw new IllegalArgumentException(
                    "Physical slot does not exist. " +
                            "Page ID: " +
                            recordId.pageId() +
                            ", slot ID: " +
                            recordId.slotId() +
                            ", record count: " +
                            page.getHeader().getRecordCount()
            );
        }

        RecordLocation location =
                locateRecordInPage(
                        page,
                        recordId.slotId()
                );

        if (location == null) {
            throw new IllegalArgumentException(
                    "Record does not exist at physical location: " +
                            recordId
            );
        }

        return location;
    }

    /**
     * Belirtilen Record ID değerinin fiziksel olarak bulunup
     * bulunmadığını kontrol eder.
     *
     * Silinmiş kayıtlar da mevcut kabul edilir.
     */
    public synchronized boolean contains(long recordId)
            throws IOException {

        validateRecordId(recordId);

        return findRecordLocation(recordId) != null;
    }

    /**
     * Kaydın mevcut ve silinmemiş olup olmadığını kontrol eder.
     */
    public synchronized boolean isActive(long recordId)
            throws IOException {

        validateRecordId(recordId);

        RecordLocation location =
                findRecordLocation(recordId);

        return location != null
                && !location.record().isDeleted();
    }

    /**
     * Diskte bulunan tüm kayıtları döndürür.
     *
     * Silinmiş kayıtlar da listeye dahildir.
     */
    public synchronized List<Record> getAllRecords()
            throws IOException {

        List<Record> records = new ArrayList<>();

        int pageCount = pageManager.getPageCount();

        for (int pageId = 0;
             pageId < pageCount;
             pageId++) {

            Page page = pageManager.readPage(pageId);

            if (page.getHeader().getPageType()
                    != recordPageType) {
                continue;
            }

            records.addAll(
                    readRecordsFromPage(page)
            );
        }

        return Collections.unmodifiableList(records);
    }

    /**
     * Diskte bulunan aktif kayıtları döndürür.
     */
    public synchronized List<Record> getActiveRecords()
            throws IOException {

        List<Record> activeRecords = new ArrayList<>();

        for (Record record : getAllRecords()) {
            if (!record.isDeleted()) {
                activeRecords.add(record);
            }
        }

        return Collections.unmodifiableList(activeRecords);
    }

    /**
     * Silinmiş kayıtlar dahil toplam kayıt sayısını döndürür.
     */
    public synchronized int getTotalRecordCount()
            throws IOException {

        return getAllRecords().size();
    }

    /**
     * Aktif kayıt sayısını döndürür.
     */
    public synchronized int getActiveRecordCount()
            throws IOException {

        return getActiveRecords().size();
    }

    /**
     * Bir sonraki üretilecek Record ID değerini döndürür.
     */
    public long getNextRecordId() {
        return nextRecordId.get();
    }

    /**
     * Yeterli boş alanı bulunan bir kayıt sayfasını döndürür.
     *
     * Uygun sayfa bulunamazsa dosyanın sonunda yeni bir sayfa oluşturur.
     */
    private Page findPageWithEnoughSpace(
            int requiredBytes
    ) throws IOException {

        int pageCount = pageManager.getPageCount();

        for (int pageId = 0;
             pageId < pageCount;
             pageId++) {

            Page page = pageManager.readPage(pageId);

            if (page.getHeader().getPageType()
                    == recordPageType
                    && page.hasEnoughSpace(requiredBytes)) {

                return page;
            }
        }

        return new Page(
                pageCount,
                recordPageType
        );
    }

    /**
     * Serialized Record verisini sayfanın payload alanına ekler.
     */
    private void appendRecordToPage(
            Page page,
            byte[] recordBytes
    ) {

        Objects.requireNonNull(
                page,
                "Page cannot be null."
        );

        Objects.requireNonNull(
                recordBytes,
                "Record bytes cannot be null."
        );

        if (!page.hasEnoughSpace(recordBytes.length)) {
            throw new IllegalStateException(
                    "Page does not have enough free space."
            );
        }

        int writeOffset =
                page.getHeader().getUsedBytes();

        System.arraycopy(
                recordBytes,
                0,
                page.getPayload(),
                writeOffset,
                recordBytes.length
        );

        page.getHeader().setUsedBytes(
                writeOffset + recordBytes.length
        );

        page.getHeader().setRecordCount(
                page.getHeader().getRecordCount() + 1
        );
    }

    /**
     * Record ID değerine ait fiziksel sayfa ve offset bilgisini bulur.
     */
    private RecordLocation findRecordLocation(
            long recordId
    ) throws IOException {

        int pageCount = pageManager.getPageCount();

        for (int pageId = 0;
             pageId < pageCount;
             pageId++) {

            Page page = pageManager.readPage(pageId);

            if (page.getHeader().getPageType()
                    != recordPageType) {
                continue;
            }

            int offset = 0;
            int slotId = 0;
            int usedBytes =
                    page.getHeader().getUsedBytes();

            while (offset < usedBytes) {

                int serializedSize =
                        calculateRecordSize(
                                page,
                                offset
                        );

                byte[] recordBytes =
                        Arrays.copyOfRange(
                                page.getPayload(),
                                offset,
                                offset + serializedSize
                        );

                Record record =
                        RecordSerializer.deserialize(
                                recordBytes
                        );

                if (record.getRecordId() == recordId) {
                    return new RecordLocation(
                            page,
                            offset,
                            serializedSize,
                            slotId,
                            record
                    );
                }

                offset += serializedSize;
                slotId++;
            }
        }

        return null;
    }

    /**
     * Bir sayfa içerisindeki tüm kayıtları deserialize eder.
     */
    private List<Record> readRecordsFromPage(Page page) {

        List<Record> records = new ArrayList<>();

        int offset = 0;
        int usedBytes =
                page.getHeader().getUsedBytes();

        while (offset < usedBytes) {

            int serializedSize =
                    calculateRecordSize(
                            page,
                            offset
                    );

            byte[] recordBytes =
                    Arrays.copyOfRange(
                            page.getPayload(),
                            offset,
                            offset + serializedSize
                    );

            records.add(
                    RecordSerializer.deserialize(
                            recordBytes
                    )
            );

            offset += serializedSize;
        }

        if (records.size()
                != page.getHeader().getRecordCount()) {

            throw new IllegalStateException(
                    "Page record count does not match the " +
                            "number of serialized records. Page ID: " +
                            page.getHeader().getPageId() + "."
            );
        }

        return records;
    }

    /**
     * Belirtilen offsetteki serialized Record boyutunu hesaplar.
     */
    private int calculateRecordSize(
            Page page,
            int offset
    ) {

        int usedBytes =
                page.getHeader().getUsedBytes();

        int remainingBytes =
                usedBytes - offset;

        if (remainingBytes
                < RecordSerializer.HEADER_SIZE) {

            throw new IllegalStateException(
                    "Incomplete record header in page " +
                            page.getHeader().getPageId() + "."
            );
        }

        int dataLength = ByteBuffer.wrap(
                page.getPayload(),
                offset + DATA_LENGTH_OFFSET,
                Integer.BYTES
        ).getInt();

        if (dataLength < 0) {
            throw new IllegalStateException(
                    "Negative record data length in page " +
                            page.getHeader().getPageId() + "."
            );
        }

        int serializedSize;

        try {
            serializedSize = Math.addExact(
                    RecordSerializer.HEADER_SIZE,
                    dataLength
            );
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Serialized record size exceeds supported range.",
                    exception
            );
        }

        if (serializedSize > remainingBytes) {
            throw new IllegalStateException(
                    "Incomplete serialized record in page " +
                            page.getHeader().getPageId() + "."
            );
        }

        return serializedSize;
    }

    /**
     * Sayfa içerisindeki bir kaydı yeni serialized veriyle değiştirir.
     *
     * Değişken kayıt boyutu nedeniyle sonraki kayıtlar kaydırılır.
     */
    private void replaceRecordInPage(
            RecordLocation location,
            byte[] updatedRecordBytes
    ) {

        Page page = location.page();
        byte[] payload = page.getPayload();

        int usedBytes =
                page.getHeader().getUsedBytes();

        int oldRecordEnd =
                location.offset()
                        + location.serializedSize();

        int trailingBytes =
                usedBytes - oldRecordEnd;

        byte[] rebuiltPayload =
                new byte[Page.PAYLOAD_SIZE];

        System.arraycopy(
                payload,
                0,
                rebuiltPayload,
                0,
                location.offset()
        );

        System.arraycopy(
                updatedRecordBytes,
                0,
                rebuiltPayload,
                location.offset(),
                updatedRecordBytes.length
        );

        System.arraycopy(
                payload,
                oldRecordEnd,
                rebuiltPayload,
                location.offset()
                        + updatedRecordBytes.length,
                trailingBytes
        );

        System.arraycopy(
                rebuiltPayload,
                0,
                payload,
                0,
                payload.length
        );

        page.getHeader().setUsedBytes(
                usedBytes
                        - location.serializedSize()
                        + updatedRecordBytes.length
        );
    }

    /**
     * Diskte bulunan en büyük Record ID değerini kullanarak
     * sonraki ID değerini hesaplar.
     */
    private long calculateNextRecordId()
            throws IOException {

        long highestRecordId = -1;

        for (Record record : getAllRecords()) {
            highestRecordId = Math.max(
                    highestRecordId,
                    record.getRecordId()
            );
        }

        if (highestRecordId == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Record ID range has been exhausted."
            );
        }

        return highestRecordId + 1;
    }

    private void validateRow(Row row) {
        if (row == null) {
            throw new IllegalArgumentException(
                    "Row cannot be null."
            );
        }
    }

    private void validateRecordId(long recordId) {
        if (recordId < 0) {
            throw new IllegalArgumentException(
                    "Record ID cannot be negative: " +
                            recordId
            );
        }
    }


}