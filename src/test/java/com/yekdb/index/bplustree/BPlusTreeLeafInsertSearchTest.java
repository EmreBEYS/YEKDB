package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 2 leaf insert ve exact search testleri.
 *
 * Bu testler:
 * - sıralı insert,
 * - binary-search tabanlı exact search,
 * - duplicate key bucket yönetimi,
 * - duplicate pointer engelleme,
 * - bulunamayan key davranışı,
 * - overflow hazırlığı
 *
 * davranışlarını doğrular.
 */
class BPlusTreeLeafInsertSearchTest {

    @Test
    void shouldInsertSingleKeyIntoLeafRoot() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        RecordPointer pointer =
                new RecordPointer(1, 0);

        boolean newKey =
                tree.insert(
                        10,
                        pointer
                );

        assertTrue(newKey);

        assertFalse(
                tree.isEmpty()
        );

        assertEquals(
                1,
                tree.getRoot().getKeyCount()
        );

        assertEquals(
                10,
                tree.getRoot().getKey(0)
        );

        assertEquals(
                List.of(pointer),
                tree.search(10)
        );
    }

    @Test
    void shouldKeepKeysSortedAfterUnorderedInsert() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                30,
                new RecordPointer(3, 0)
        );

        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        tree.insert(
                40,
                new RecordPointer(4, 0)
        );

        tree.insert(
                20,
                new RecordPointer(2, 0)
        );

        assertEquals(
                List.of(
                        10,
                        20,
                        30,
                        40
                ),
                tree.getRoot().getKeys()
        );
    }

    @Test
    void shouldSearchExistingKey() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer pointer =
                new RecordPointer(5, 3);

        tree.insert(
                100,
                pointer
        );

        List<RecordPointer> result =
                tree.search(100);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                pointer,
                result.get(0)
        );
    }

    @Test
    void shouldReturnEmptyListWhenKeyDoesNotExist() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        List<RecordPointer> result =
                tree.search(999);

        assertNotNull(result);

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldReportWhetherKeyExists() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                50,
                new RecordPointer(2, 1)
        );

        assertTrue(
                tree.containsKey(50)
        );

        assertFalse(
                tree.containsKey(51)
        );
    }

    @Test
    void shouldAppendPointerToExistingKeyBucket() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer first =
                new RecordPointer(1, 0);

        RecordPointer second =
                new RecordPointer(2, 0);

        boolean firstInsert =
                tree.insert(
                        100,
                        first
                );

        boolean secondInsert =
                tree.insert(
                        100,
                        second
                );

        /*
         * İlk işlem yeni key oluşturur.
         */
        assertTrue(firstInsert);

        /*
         * İkinci işlem mevcut key bucket'ını günceller.
         */
        assertFalse(secondInsert);

        assertEquals(
                1,
                tree.getRoot().getKeyCount()
        );

        assertEquals(
                List.of(
                        first,
                        second
                ),
                tree.search(100)
        );
    }

    @Test
    void shouldNotInsertSamePointerTwiceIntoSameBucket() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer pointer =
                new RecordPointer(1, 5);

        tree.insert(
                100,
                pointer
        );

        tree.insert(
                100,
                pointer
        );

        List<RecordPointer> pointers =
                tree.search(100);

        assertEquals(
                1,
                pointers.size()
        );

        assertEquals(
                pointer,
                pointers.get(0)
        );
    }

    @Test
    void shouldMaintainIndependentBucketsForDifferentKeys() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer p1 =
                new RecordPointer(1, 0);

        RecordPointer p2 =
                new RecordPointer(2, 0);

        RecordPointer p3 =
                new RecordPointer(3, 0);

        tree.insert(
                10,
                p1
        );

        tree.insert(
                20,
                p2
        );

        tree.insert(
                30,
                p3
        );

        assertEquals(
                List.of(p1),
                tree.search(10)
        );

        assertEquals(
                List.of(p2),
                tree.search(20)
        );

        assertEquals(
                List.of(p3),
                tree.search(30)
        );
    }

    @Test
    void shouldInsertAtBeginningMiddleAndEnd() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(6);

        tree.insert(
                30,
                new RecordPointer(3, 0)
        );

        /*
         * Baştan ekleme.
         */
        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        /*
         * Ortaya ekleme.
         */
        tree.insert(
                20,
                new RecordPointer(2, 0)
        );

        /*
         * Sona ekleme.
         */
        tree.insert(
                40,
                new RecordPointer(4, 0)
        );

        assertEquals(
                List.of(
                        10,
                        20,
                        30,
                        40
                ),
                tree.getRoot().getKeys()
        );
    }

    @Test
    void shouldFillLeafUntilSplitThreshold() {

        /*
         * order = 4
         *
         * maxKeys = 3
         *
         * Üçüncü unique key sonrasında leaf tam kapasitededir,
         * fakat henüz overflow oluşmamıştır.
         */
        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        tree.insert(
                20,
                new RecordPointer(2, 0)
        );

        tree.insert(
                30,
                new RecordPointer(3, 0)
        );

        assertTrue(
                tree.getRoot().isLeaf()
        );

        assertEquals(
                3,
                tree.getRoot().getKeyCount()
        );

        assertEquals(
                List.of(
                        10,
                        20,
                        30
                ),
                tree.getRoot().getKeys()
        );

        assertTrue(
                tree.getRoot().isFull()
        );

        assertFalse(
                tree.getRoot().isOverflow()
        );

        assertEquals(
                1,
                tree.getHeight()
        );
    }

    @Test
    void shouldNotOverflowWhenDuplicateKeyIsInserted() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        tree.insert(
                20,
                new RecordPointer(2, 0)
        );

        tree.insert(
                30,
                new RecordPointer(3, 0)
        );

        /*
         * Unique key sayısı zaten maksimumda.
         * Aynı key'e pointer eklemek node kapasitesini artırmamalı.
         */
        tree.insert(
                20,
                new RecordPointer(99, 0)
        );

        assertEquals(
                3,
                tree.getRoot().getKeyCount()
        );

        assertTrue(
                tree.getRoot().isFull()
        );

        assertFalse(
                tree.getRoot().isOverflow()
        );

        assertEquals(
                2,
                tree.search(20).size()
        );
    }

    @Test
    void shouldRejectNullKeyDuringInsert() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> tree.insert(
                                null,
                                new RecordPointer(1, 0)
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("key")
        );
    }

    @Test
    void shouldRejectNullPointerDuringInsert() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> tree.insert(
                                10,
                                null
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("recordpointer")
        );
    }

    @Test
    void shouldRejectInvalidPointerDuringInsert() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        RecordPointer invalidPointer =
                new RecordPointer(
                        -1,
                        -1
                );

        assertFalse(
                invalidPointer.isValid()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.insert(
                        10,
                        invalidPointer
                )
        );
    }

    @Test
    void shouldRejectNullKeyDuringSearch() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.search(null)
        );
    }

    @Test
    void shouldSupportStringKeys() {

        BPlusTree<String> tree =
                new BPlusTree<>(5);

        tree.insert(
                "Malatya",
                new RecordPointer(3, 0)
        );

        tree.insert(
                "Ankara",
                new RecordPointer(1, 0)
        );

        tree.insert(
                "Istanbul",
                new RecordPointer(2, 0)
        );

        assertEquals(
                List.of(
                        "Ankara",
                        "Istanbul",
                        "Malatya"
                ),
                tree.getRoot().getKeys()
        );

        assertTrue(
                tree.containsKey("Malatya")
        );
    }
    @Test
    void shouldKeepLeafStructureValidAfterMultipleInserts() {

        /*
         * 10 unique key test edeceğimiz için order=11 kullanıyoruz.
         *
         * maxKeys = 10
         *
         * Böylece bu test split davranışına girmeden
         * leaf key/value senkronizasyonunu doğrular.
         */
        BPlusTree<Integer> tree =
                new BPlusTree<>(11);

        for (int i = 9; i >= 0; i--) {

            tree.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        BPlusTreeLeafNode<Integer> leaf =
                tree.getRootLeaf();

        assertEquals(
                leaf.getKeyCount(),
                leaf.getValueCount()
        );

        assertTrue(
                leaf.isStructurallyValid()
        );

        assertEquals(
                List.of(
                        0, 1, 2, 3, 4,
                        5, 6, 7, 8, 9
                ),
                leaf.getKeys()
        );

        assertTrue(
                leaf.isFull()
        );

        assertFalse(
                leaf.isOverflow()
        );
    }
}