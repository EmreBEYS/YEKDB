package com.yekdb.storage.page;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;

import java.io.IOException;
import java.util.Objects;

/**
 * YEKDB fiziksel sayfalarının veri dosyasına yazılması
 * ve veri dosyasından okunmasından sorumludur.
 *
 * Disk düzeni:
 *
 * +-----------------------------+
 * | Database Header - 128 byte  |
 * +-----------------------------+
 * | Page 0 - 4096 byte          |
 * +-----------------------------+
 * | Page 1 - 4096 byte          |
 * +-----------------------------+
 * | Page 2 - 4096 byte          |
 * +-----------------------------+
 */
public final class PageManager {

    private final DataFile dataFile;

    private final PageSerializer pageSerializer;

    public PageManager(DataFile dataFile) {
        this(
                dataFile,
                new PageSerializer()
        );
    }

    public PageManager(
            DataFile dataFile,
            PageSerializer pageSerializer
    ) {
        this.dataFile = Objects.requireNonNull(
                dataFile,
                "Data file cannot be null."
        );

        this.pageSerializer = Objects.requireNonNull(
                pageSerializer,
                "Page serializer cannot be null."
        );
    }

    /**
     * Bir sayfayı veri dosyasındaki fiziksel konumuna yazar.
     *
     * Var olan sayfalar güncellenebilir. Yeni sayfalar yalnızca
     * dosyanın sonuna sıralı olarak eklenebilir.
     */
    public void writePage(Page page) throws IOException {

        Objects.requireNonNull(
                page,
                "Page cannot be null."
        );

        ensureDataFileReady();

        int pageId = page.getHeader().getPageId();
        int pageCount = getPageCount();

        /*
         * Mevcut sayfa güncellenebilir:
         * pageId < pageCount
         *
         * Yeni sayfa dosyanın sonuna eklenebilir:
         * pageId == pageCount
         *
         * Arada boş sayfa bırakılamaz:
         * pageId > pageCount
         */
        if (pageId > pageCount) {
            throw new IllegalArgumentException(
                    "Pages must be written sequentially. " +
                            "Expected page ID at most " + pageCount +
                            ", but received " + pageId + "."
            );
        }

        byte[] pageBytes =
                pageSerializer.serialize(page);

        long pageOffset =
                calculatePageOffset(pageId);

        dataFile.write(
                pageOffset,
                pageBytes
        );
    }

    /**
     * Belirtilen kimliğe sahip sayfayı veri dosyasından okur.
     */
    public Page readPage(int pageId) throws IOException {

        validatePageId(pageId);
        ensureDataFileReady();

        if (!pageExists(pageId)) {
            throw new IllegalArgumentException(
                    "Page does not exist: " + pageId
            );
        }

        long pageOffset =
                calculatePageOffset(pageId);

        byte[] pageBytes = dataFile.read(
                pageOffset,
                Page.PAGE_SIZE
        );

        Page page =
                pageSerializer.deserialize(pageBytes);

        /*
         * Disk konumu ile sayfa header kimliği aynı olmalıdır.
         * Bu kontrol dosya bozulmalarını tespit etmeye yardımcı olur.
         */
        if (page.getHeader().getPageId() != pageId) {
            throw new IllegalStateException(
                    "Page ID mismatch. Requested page "
                            + pageId
                            + " but physical page contains ID "
                            + page.getHeader().getPageId()
                            + "."
            );
        }

        return page;
    }

    /**
     * Belirtilen sayfanın dosyada mevcut olup olmadığını döndürür.
     */
    public boolean pageExists(int pageId) throws IOException {

        validatePageId(pageId);
        ensureDataFileReady();

        return pageId < getPageCount();
    }

    /**
     * Veri dosyasında bulunan fiziksel sayfa sayısını döndürür.
     */
    public int getPageCount() throws IOException {

        ensureDataFileReady();

        long pageAreaSize =
                dataFile.size() - DatabaseHeader.HEADER_SIZE;

        if (pageAreaSize < 0) {
            throw new IllegalStateException(
                    "Data file is smaller than the database header."
            );
        }

        if (pageAreaSize % Page.PAGE_SIZE != 0) {
            throw new IllegalStateException(
                    "Invalid data file size. Page area is not aligned " +
                            "to the physical page size."
            );
        }

        long pageCount =
                pageAreaSize / Page.PAGE_SIZE;

        if (pageCount > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Page count exceeds the supported integer range."
            );
        }

        return (int) pageCount;
    }

    /**
     * Sayfanın veri dosyası içerisindeki fiziksel offsetini hesaplar.
     */
    public long calculatePageOffset(int pageId) {

        validatePageId(pageId);

        try {
            long pageAreaOffset = Math.multiplyExact(
                    (long) pageId,
                    Page.PAGE_SIZE
            );

            return Math.addExact(
                    DatabaseHeader.HEADER_SIZE,
                    pageAreaOffset
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Page offset exceeds the supported range.",
                    exception
            );
        }
    }

    /**
     * Veri dosyasındaki değişiklikleri fiziksel diske aktarır.
     */
    public void sync() throws IOException {

        ensureDataFileReady();

        dataFile.sync();
    }

    private void ensureDataFileReady() throws IOException {

        if (!dataFile.isOpen()) {
            throw new IllegalStateException(
                    "Data file must be open before page operations."
            );
        }

        if (dataFile.size() < DatabaseHeader.HEADER_SIZE) {
            throw new IllegalStateException(
                    "Database header has not been written yet."
            );
        }
    }

    private void validatePageId(int pageId) {

        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative."
            );
        }
    }
}