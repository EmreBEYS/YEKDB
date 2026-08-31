package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageHeader;
import com.yekdb.storage.record.page.PageManager;
import com.yekdb.storage.record.page.PageType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * B+ Tree node image nesnelerinin YEKDB fiziksel INDEX sayfalarına
 * yazılması ve yeniden okunmasından sorumludur.
 *
 * Phase 11 serializer katmanı ile mevcut PageManager arasında
 * köprü görevi görür.
 *
 * Her B+ Tree node'u tam olarak bir INDEX page payload alanında
 * saklanır.
 *
 * @param <K> B+ Tree key tipi
 */
public final class BPlusTreePageStore<
        K extends Comparable<K>> {

    private final PageManager pageManager;

    private final BPlusTreeNodeSerializer<K> serializer;

    public BPlusTreePageStore(
            PageManager pageManager,
            BPlusTreeNodeSerializer<K> serializer
    ) {

        this.pageManager =
                Objects.requireNonNull(
                        pageManager,
                        "Page manager cannot be null."
                );

        this.serializer =
                Objects.requireNonNull(
                        serializer,
                        "B+ Tree node serializer cannot be null."
                );
    }

    /**
     * Veri dosyasının sonundaki bir sonraki kullanılabilir
     * fiziksel page id değerini döndürür.
     */
    public int allocatePageId()
            throws IOException {

        return pageManager.getPageCount();
    }

    /**
     * Yeni bir leaf node için page id ayırır,
     * node image oluşturur ve fiziksel diske yazar.
     */
    public BPlusTreeNodeImage<K> createLeaf(
            int order,
            int parentPageId,
            int previousLeafPageId,
            int nextLeafPageId,
            List<K> keys,
            List<List<RecordPointer>> valueBuckets
    ) throws IOException {

        int pageId =
                allocatePageId();

        BPlusTreeNodeImage<K> image =
                BPlusTreeNodeImage.leaf(
                        order,
                        pageId,
                        parentPageId,
                        previousLeafPageId,
                        nextLeafPageId,
                        keys,
                        valueBuckets
                );

        writeNode(image);

        return image;
    }

    /**
     * Yeni bir internal node için page id ayırır,
     * node image oluşturur ve fiziksel diske yazar.
     */
    public BPlusTreeNodeImage<K> createInternal(
            int order,
            int parentPageId,
            List<K> keys,
            List<Integer> childPageIds
    ) throws IOException {

        int pageId =
                allocatePageId();

        BPlusTreeNodeImage<K> image =
                BPlusTreeNodeImage.internal(
                        order,
                        pageId,
                        parentPageId,
                        keys,
                        childPageIds
                );

        writeNode(image);

        return image;
    }

    /**
     * Bir B+ Tree node image nesnesini INDEX page olarak yazar.
     *
     * Var olan page id gönderilirse overwrite yapılabilir.
     * Yeni page id ise PageManager sıralı allocation kuralını uygular.
     */
    public void writeNode(
            BPlusTreeNodeImage<K> image
    ) throws IOException {

        Objects.requireNonNull(
                image,
                "B+ Tree node image cannot be null."
        );

        byte[] serialized =
                serializer.serialize(image);

        validatePayloadSize(
                serialized.length
        );

        Page page =
                new Page(
                        image.getPageId(),
                        PageType.INDEX
                );

        System.arraycopy(
                serialized,
                0,
                page.getPayload(),
                0,
                serialized.length
        );

        /*
         * Bir INDEX page içerisinde bir B+ Tree node image
         * saklandığını ifade eder.
         */
        page.getHeader()
                .setRecordCount(1);

        page.getHeader()
                .setUsedBytes(
                        serialized.length
                );

        /*
         * Leaf node için PageHeader.nextPageId alanını da
         * next leaf bilgisinin fiziksel mirror'ı olarak kullanıyoruz.
         *
         * Internal node'larda zincir bağlantısı olmadığı için -1.
         */
        page.getHeader()
                .setNextPageId(
                        image.isLeaf()
                                ? image.getNextLeafPageId()
                                : PageHeader.NO_NEXT_PAGE
                );

        pageManager.writePage(page);
    }

    /**
     * Fiziksel INDEX page üzerinden B+ Tree node image okur.
     */
    public BPlusTreeNodeImage<K> readNode(
            int pageId
    ) throws IOException {

        validatePageId(pageId);

        Page page =
                pageManager.readPage(pageId);

        validateIndexPage(page);

        int usedBytes =
                page.getHeader()
                        .getUsedBytes();

        byte[] serialized =
                Arrays.copyOf(
                        page.getPayload(),
                        usedBytes
                );

        BPlusTreeNodeImage<K> image =
                serializer.deserialize(
                        serialized
                );

        if (image.getPageId() != pageId) {

            throw new IllegalStateException(
                    "B+ Tree node page ID does not match physical page ID."
            );
        }

        validateHeaderMirror(
                page,
                image
        );

        return image;
    }

    /**
     * Belirtilen page'in geçerli bir B+ Tree INDEX node'u
     * olup olmadığını kontrol eder.
     */
    public boolean nodeExists(
            int pageId
    ) throws IOException {

        validatePageId(pageId);

        if (!pageManager.pageExists(pageId)) {
            return false;
        }

        Page page =
                pageManager.readPage(pageId);

        return page.getHeader()
                .getPageType()
                == PageType.INDEX;
    }

    /**
     * PageManager üzerinden fiziksel disk sync işlemi yapar.
     */
    public void sync()
            throws IOException {

        pageManager.sync();
    }

    /**
     * Bir node image'ın fiziksel INDEX page payload alanına
     * sığıp sığmadığını döndürür.
     */
    public boolean fitsInPage(
            BPlusTreeNodeImage<K> image
    ) {

        Objects.requireNonNull(
                image,
                "B+ Tree node image cannot be null."
        );

        return serializer
                .calculateSerializedSize(image)
                <= Page.PAYLOAD_SIZE;
    }

    /**
     * B+ Tree node payload'ı için kullanılabilir maksimum
     * fiziksel byte kapasitesini döndürür.
     */
    public int getMaximumNodePayloadSize() {
        return Page.PAYLOAD_SIZE;
    }

    private void validateIndexPage(
            Page page
    ) {

        if (page.getHeader()
                .getPageType()
                != PageType.INDEX) {

            throw new IllegalStateException(
                    "Page is not a B+ Tree INDEX page."
            );
        }

        if (page.getHeader()
                .getRecordCount()
                != 1) {

            throw new IllegalStateException(
                    "B+ Tree INDEX page must contain exactly one node record."
            );
        }

        int usedBytes =
                page.getHeader()
                        .getUsedBytes();

        if (usedBytes <= 0) {

            throw new IllegalStateException(
                    "B+ Tree INDEX page payload is empty."
            );
        }

        if (usedBytes > Page.PAYLOAD_SIZE) {

            throw new IllegalStateException(
                    "B+ Tree INDEX page payload exceeds page capacity."
            );
        }
    }

    /**
     * Page header ile node image içerisindeki redundant fiziksel
     * referansların tutarlı olduğunu doğrular.
     */
    private void validateHeaderMirror(
            Page page,
            BPlusTreeNodeImage<K> image
    ) {

        int headerNextPageId =
                page.getHeader()
                        .getNextPageId();

        if (image.isLeaf()) {

            if (headerNextPageId
                    != image.getNextLeafPageId()) {

                throw new IllegalStateException(
                        "Leaf next-page reference does not match page header."
                );
            }

            return;
        }

        if (headerNextPageId
                != PageHeader.NO_NEXT_PAGE) {

            throw new IllegalStateException(
                    "Internal B+ Tree page cannot have a next-page reference."
            );
        }
    }

    private void validatePayloadSize(
            int payloadSize
    ) {

        if (payloadSize <= 0) {

            throw new IllegalArgumentException(
                    "B+ Tree node payload must contain data."
            );
        }

        if (payloadSize > Page.PAYLOAD_SIZE) {

            throw new IllegalArgumentException(
                    "B+ Tree node payload exceeds INDEX page capacity. "
                            + "Payload: "
                            + payloadSize
                            + " bytes, capacity: "
                            + Page.PAYLOAD_SIZE
                            + " bytes."
            );
        }
    }

    private void validatePageId(
            int pageId
    ) {

        if (pageId < 0) {

            throw new IllegalArgumentException(
                    "Page ID cannot be negative."
            );
        }
    }
}