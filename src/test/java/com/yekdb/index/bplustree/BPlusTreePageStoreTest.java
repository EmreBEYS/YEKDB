package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageManager;
import com.yekdb.storage.record.page.PageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 12 fiziksel INDEX page ve disk persistence testleri.
 */
class BPlusTreePageStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAllocateFirstIndexPageAsZero()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            assertEquals(
                    0,
                    store.allocatePageId()
            );
        }
    }

    @Test
    void shouldCreateAndPersistLeafNode()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            BPlusTreeNodeImage<Integer> image =
                    store.createLeaf(
                            4,
                            -1,
                            -1,
                            -1,
                            List.of(
                                    10,
                                    20
                            ),
                            List.of(
                                    List.of(
                                            new RecordPointer(
                                                    1,
                                                    0
                                            )
                                    ),
                                    List.of(
                                            new RecordPointer(
                                                    2,
                                                    0
                                            )
                                    )
                            )
                    );

            assertEquals(
                    0,
                    image.getPageId()
            );

            assertTrue(
                    store.nodeExists(0)
            );
        }
    }

    @Test
    void shouldReadPersistedLeafNode()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            store.createLeaf(
                    4,
                    -1,
                    -1,
                    -1,
                    List.of(
                            10,
                            20
                    ),
                    List.of(
                            List.of(
                                    new RecordPointer(
                                            1,
                                            0
                                    )
                            ),
                            List.of(
                                    new RecordPointer(
                                            2,
                                            0
                                    )
                            )
                    )
            );

            BPlusTreeNodeImage<Integer> restored =
                    store.readNode(0);

            assertTrue(
                    restored.isLeaf()
            );

            assertEquals(
                    List.of(
                            10,
                            20
                    ),
                    restored.getKeys()
            );

            assertEquals(
                    new RecordPointer(
                            1,
                            0
                    ),
                    restored
                            .getValueBuckets()
                            .get(0)
                            .get(0)
            );
        }
    }

    @Test
    void shouldCreateAndPersistInternalNode()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            /*
             * İlk iki sayfayı child leaf olarak ayırıyoruz.
             */
            BPlusTreeNodeImage<Integer> left =
                    store.createLeaf(
                            4,
                            2,
                            -1,
                            1,
                            List.of(
                                    10,
                                    20
                            ),
                            List.of(
                                    List.of(
                                            new RecordPointer(
                                                    10,
                                                    0
                                            )
                                    ),
                                    List.of(
                                            new RecordPointer(
                                                    20,
                                                    0
                                            )
                                    )
                            )
                    );

            BPlusTreeNodeImage<Integer> right =
                    store.createLeaf(
                            4,
                            2,
                            0,
                            -1,
                            List.of(
                                    30,
                                    40
                            ),
                            List.of(
                                    List.of(
                                            new RecordPointer(
                                                    30,
                                                    0
                                            )
                                    ),
                                    List.of(
                                            new RecordPointer(
                                                    40,
                                                    0
                                            )
                                    )
                            )
                    );

            BPlusTreeNodeImage<Integer> root =
                    store.createInternal(
                            4,
                            -1,
                            List.of(30),
                            List.of(
                                    left.getPageId(),
                                    right.getPageId()
                            )
                    );

            assertEquals(
                    2,
                    root.getPageId()
            );

            BPlusTreeNodeImage<Integer> restored =
                    store.readNode(
                            root.getPageId()
                    );

            assertFalse(
                    restored.isLeaf()
            );

            assertEquals(
                    List.of(30),
                    restored.getKeys()
            );

            assertEquals(
                    List.of(
                            0,
                            1
                    ),
                    restored.getChildPageIds()
            );
        }
    }

    @Test
    void shouldUseIndexPageType()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            PageManager pageManager =
                    new PageManager(
                            dataFile
                    );

            BPlusTreePageStore<Integer> store =
                    createStore(
                            pageManager
                    );

            store.createLeaf(
                    4,
                    -1,
                    -1,
                    -1,
                    List.of(10),
                    List.of(
                            List.of(
                                    new RecordPointer(
                                            1,
                                            0
                                    )
                            )
                    )
            );

            Page page =
                    pageManager.readPage(0);

            assertEquals(
                    PageType.INDEX,
                    page.getHeader()
                            .getPageType()
            );

            assertEquals(
                    1,
                    page.getHeader()
                            .getRecordCount()
            );

            assertTrue(
                    page.getHeader()
                            .getUsedBytes() > 0
            );
        }
    }

    @Test
    void shouldMirrorNextLeafIntoPageHeader()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            PageManager pageManager =
                    new PageManager(
                            dataFile
                    );

            BPlusTreePageStore<Integer> store =
                    createStore(
                            pageManager
                    );

            BPlusTreeNodeImage<Integer> image =
                    BPlusTreeNodeImage.leaf(
                            4,
                            0,
                            -1,
                            -1,
                            42,
                            List.of(10),
                            List.of(
                                    List.of(
                                            new RecordPointer(
                                                    1,
                                                    0
                                            )
                                    )
                            )
                    );

            store.writeNode(image);

            assertEquals(
                    42,
                    pageManager
                            .readPage(0)
                            .getHeader()
                            .getNextPageId()
            );
        }
    }

    @Test
    void shouldOverwriteExistingIndexPage()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            BPlusTreeNodeImage<Integer> original =
                    store.createLeaf(
                            4,
                            -1,
                            -1,
                            -1,
                            List.of(10),
                            List.of(
                                    List.of(
                                            new RecordPointer(
                                                    1,
                                                    0
                                            )
                                    )
                            )
                    );

            BPlusTreeNodeImage<Integer> updated =
                    BPlusTreeNodeImage.leaf(
                            4,
                            original.getPageId(),
                            -1,
                            -1,
                            -1,
                            List.of(
                                    10,
                                    20
                            ),
                            List.of(
                                    List.of(
                                            new RecordPointer(
                                                    1,
                                                    0
                                            )
                                    ),
                                    List.of(
                                            new RecordPointer(
                                                    2,
                                                    0
                                            )
                                    )
                            )
                    );

            store.writeNode(updated);

            assertEquals(
                    List.of(
                            10,
                            20
                    ),
                    store.readNode(0)
                            .getKeys()
            );

            /*
             * Overwrite yeni fiziksel page oluşturmamalıdır.
             */
            assertEquals(
                    1,
                    store.allocatePageId()
            );
        }
    }

    @Test
    void shouldPersistDuplicatePointerBucket()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            RecordPointer first =
                    new RecordPointer(
                            1,
                            0
                    );

            RecordPointer second =
                    new RecordPointer(
                            2,
                            1
                    );

            BPlusTreeNodeImage<Integer> image =
                    store.createLeaf(
                            4,
                            -1,
                            -1,
                            -1,
                            List.of(100),
                            List.of(
                                    List.of(
                                            first,
                                            second
                                    )
                            )
                    );

            assertEquals(
                    List.of(
                            first,
                            second
                    ),
                    store.readNode(
                                    image.getPageId()
                            )
                            .getValueBuckets()
                            .get(0)
            );
        }
    }

    @Test
    void shouldSurviveDataFileReopen()
            throws IOException {

        Path path =
                tempDir.resolve(
                        "reopen-test.yekdb"
                );

        /*
         * İlk session.
         */
        try (DataFile first =
                     new DataFile(path)) {

            initialize(first);

            BPlusTreePageStore<Integer> store =
                    createStore(first);

            store.createLeaf(
                    4,
                    -1,
                    -1,
                    -1,
                    List.of(
                            10,
                            20,
                            30
                    ),
                    List.of(
                            List.of(
                                    new RecordPointer(
                                            10,
                                            0
                                    )
                            ),
                            List.of(
                                    new RecordPointer(
                                            20,
                                            0
                                    )
                            ),
                            List.of(
                                    new RecordPointer(
                                            30,
                                            0
                                    )
                            )
                    )
            );

            store.sync();
        }

        /*
         * İkinci session.
         */
        try (DataFile second =
                     new DataFile(path)) {

            second.open();

            BPlusTreePageStore<Integer> reopened =
                    createStore(second);

            BPlusTreeNodeImage<Integer> restored =
                    reopened.readNode(0);

            assertEquals(
                    List.of(
                            10,
                            20,
                            30
                    ),
                    restored.getKeys()
            );

            assertEquals(
                    1,
                    reopened.allocatePageId()
            );
        }
    }

    @Test
    void shouldKeepPageCountConsistent()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            PageManager pageManager =
                    new PageManager(
                            dataFile
                    );

            BPlusTreePageStore<Integer> store =
                    createStore(
                            pageManager
                    );

            for (int i = 0;
                 i < 5;
                 i++) {

                store.createLeaf(
                        4,
                        -1,
                        -1,
                        -1,
                        List.of(i),
                        List.of(
                                List.of(
                                        new RecordPointer(
                                                i,
                                                0
                                        )
                                )
                        )
                );
            }

            assertEquals(
                    5,
                    pageManager.getPageCount()
            );

            assertEquals(
                    5,
                    pageManager.getHeaderPageCount()
            );

            assertTrue(
                    pageManager.isPageCountConsistent()
            );
        }
    }

    @Test
    void shouldRejectNodeLargerThanPagePayload()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreeNodeSerializer<String> serializer =
                    new BPlusTreeNodeSerializer<>(
                            BPlusTreeKeyCodec.stringCodec()
                    );

            BPlusTreePageStore<String> store =
                    new BPlusTreePageStore<>(
                            new PageManager(
                                    dataFile
                            ),
                            serializer
                    );

            List<String> keys =
                    new ArrayList<>();

            List<List<RecordPointer>> buckets =
                    new ArrayList<>();

            for (int i = 0;
                 i < 30;
                 i++) {

                keys.add(
                        "%03d".formatted(i)
                                + "x".repeat(200)
                );

                buckets.add(
                        List.of(
                                new RecordPointer(
                                        i,
                                        0
                                )
                        )
                );
            }

            BPlusTreeNodeImage<String> image =
                    BPlusTreeNodeImage.leaf(
                            64,
                            0,
                            -1,
                            -1,
                            -1,
                            keys,
                            buckets
                    );

            assertFalse(
                    store.fitsInPage(image)
            );

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> store.writeNode(
                                    image
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .toLowerCase()
                            .contains("capacity")
            );
        }
    }

    @Test
    void shouldReportMaximumPayloadCapacity()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            assertEquals(
                    Page.PAYLOAD_SIZE,
                    store.getMaximumNodePayloadSize()
            );
        }
    }

    @Test
    void shouldReturnFalseForNonExistingNode()
            throws IOException {

        try (DataFile dataFile =
                     createDataFile()) {

            BPlusTreePageStore<Integer> store =
                    createStore(dataFile);

            assertFalse(
                    store.nodeExists(0)
            );
        }
    }

    private DataFile createDataFile()
            throws IOException {

        DataFile dataFile =
                new DataFile(
                        tempDir.resolve(
                                "bptree-"
                                        + System.nanoTime()
                                        + ".yekdb"
                        )
                );

        initialize(dataFile);

        return dataFile;
    }

    private void initialize(
            DataFile dataFile
    ) throws IOException {

        dataFile.open();

        if (dataFile.size() == 0) {

            dataFile.write(
                    0,
                    new DatabaseHeader()
                            .toBytes()
            );
        }
    }

    private BPlusTreePageStore<Integer> createStore(
            DataFile dataFile
    ) {

        return createStore(
                new PageManager(
                        dataFile
                )
        );
    }

    private BPlusTreePageStore<Integer> createStore(
            PageManager pageManager
    ) {

        return new BPlusTreePageStore<>(
                pageManager,
                new BPlusTreeNodeSerializer<>(
                        BPlusTreeKeyCodec.integerCodec()
                )
        );
    }
}