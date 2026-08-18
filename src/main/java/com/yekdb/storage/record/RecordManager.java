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

        appendRecordToPage(
                targetPage,
                recordBytes
        );

        pageManager.writePage(targetPage);
        pageManager.sync();

        return record;
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
     * Belirtilen kaydın verisini günceller.
     *
     * Yeni kayıt aynı fiziksel sayfaya sığmalıdır.
     *
     * @param recordId Güncellenecek kayıt kimliği
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

        if (location.record().isDeleted()) {
            throw new IllegalStateException(
                    "Cannot update a deleted record: " + recordId
            );
        }

        byte[] rowBytes =
                RowSerializer.serialize(newRow);

        Record updatedRecord = new Record(
                recordId,
                rowBytes
        );

        byte[] updatedRecordBytes =
                RecordSerializer.serialize(updatedRecord);

        int newPageUsedBytes =
                location.page().getHeader().getUsedBytes()
                        - location.serializedSize()
                        + updatedRecordBytes.length;

        if (newPageUsedBytes > Page.PAYLOAD_SIZE) {
            throw new IllegalStateException(
                    "Updated record does not fit in its current page. " +
                            "Record ID: " + recordId + "."
            );
        }

        replaceRecordInPage(
                location,
                updatedRecordBytes
        );

        pageManager.writePage(location.page());
        pageManager.sync();
    }

    /**
     * Belirtilen kaydı mantıksal olarak siler.
     *
     * Record fiziksel olarak sayfada kalır ve deleted flag değeri
     * true yapılır.
     *
     * @param recordId Silinecek kayıt kimliği
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

        if (location.record().isDeleted()) {
            throw new IllegalStateException(
                    "Record has already been deleted: " + recordId
            );
        }

        location.record().markAsDeleted();

        byte[] deletedRecordBytes =
                RecordSerializer.serialize(
                        location.record()
                );

        System.arraycopy(
                deletedRecordBytes,
                0,
                location.page().getPayload(),
                location.offset(),
                deletedRecordBytes.length
        );

        pageManager.writePage(location.page());
        pageManager.sync();
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
                            record
                    );
                }

                offset += serializedSize;
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

    /**
     * Bir kaydın fiziksel konum bilgisini taşır.
     */
    private record RecordLocation(
            Page page,
            int offset,
            int serializedSize,
            Record record
    ) {
    }
}