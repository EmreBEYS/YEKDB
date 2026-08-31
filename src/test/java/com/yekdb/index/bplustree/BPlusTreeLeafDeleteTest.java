package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 7 leaf delete temel testleri.
 *
 * Bu testler:
 * - key bucket silme,
 * - tek pointer silme,
 * - son pointer sonrası key kaldırma,
 * - separator güncelleme,
 * - underflow'un Phase 8 öncesi korunması,
 * - range/search tutarlılığı
 *
 * davranışlarını doğrular.
 */
class BPlusTreeLeafDeleteTest {

    @Test
    void shouldDeleteExistingKeyFromRootLeaf() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        tree.insert(
                20,
                new RecordPointer(20, 0)
        );

        assertTrue(
                tree.delete(10)
        );

        assertFalse(
                tree.containsKey(10)
        );

        assertTrue(
                tree.containsKey(20)
        );

        assertEquals(
                List.of(20),
                tree.getRootLeaf().getKeys()
        );
    }

    @Test
    void shouldReturnFalseWhenDeletingMissingKey() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        assertFalse(
                tree.delete(999)
        );

        assertTrue(
                tree.containsKey(10)
        );
    }

    @Test
    void shouldDeleteEntireDuplicateBucket() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        tree.insert(
                10,
                new RecordPointer(2, 0)
        );

        assertEquals(
                2,
                tree.search(10).size()
        );

        assertTrue(
                tree.delete(10)
        );

        assertFalse(
                tree.containsKey(10)
        );
    }

    @Test
    void shouldDeleteOnlySpecifiedPointer() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer first =
                new RecordPointer(1, 0);

        RecordPointer second =
                new RecordPointer(2, 0);

        tree.insert(
                10,
                first
        );

        tree.insert(
                10,
                second
        );

        assertTrue(
                tree.delete(
                        10,
                        first
                )
        );

        assertEquals(
                List.of(second),
                tree.search(10)
        );

        assertTrue(
                tree.containsKey(10)
        );
    }

    @Test
    void shouldRemoveKeyWhenLastPointerIsDeleted() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer pointer =
                new RecordPointer(1, 0);

        tree.insert(
                10,
                pointer
        );

        assertTrue(
                tree.delete(
                        10,
                        pointer
                )
        );

        assertFalse(
                tree.containsKey(10)
        );

        assertTrue(
                tree.getRootLeaf()
                        .isEmpty()
        );
    }

    @Test
    void shouldReturnFalseWhenPointerDoesNotExist() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        tree.insert(
                10,
                new RecordPointer(1, 0)
        );

        assertFalse(
                tree.delete(
                        10,
                        new RecordPointer(999, 0)
                )
        );

        assertTrue(
                tree.containsKey(10)
        );
    }

    @Test
    void shouldRejectNullKeyDuringDelete() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.delete(null)
        );
    }

    @Test
    void shouldRejectNullPointerDuringDelete() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.delete(
                        10,
                        null
                )
        );
    }

    @Test
    void shouldRejectInvalidPointerDuringDelete() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        RecordPointer invalid =
                new RecordPointer(
                        -1,
                        -1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.delete(
                        10,
                        invalid
                )
        );
    }

    @Test
    void shouldUpdateSeparatorWhenLeafMinimumChanges() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        tree.insert(
                20,
                new RecordPointer(20, 0)
        );

        tree.insert(
                30,
                new RecordPointer(30, 0)
        );

        tree.insert(
                40,
                new RecordPointer(40, 0)
        );

        /*
         * Sağ leaf'i minimum occupancy'nin üzerinde tutuyoruz.
         * Böylece bu test Phase 9 merge davranışına girmeden
         * yalnızca separator yenilemesini doğrular.
         */
        tree.insert(
                35,
                new RecordPointer(35, 0)
        );

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        assertEquals(
                30,
                root.getKey(0)
        );

        assertTrue(
                tree.delete(30)
        );

        assertEquals(
                35,
                root.getKey(0)
        );

        assertFalse(
                tree.containsKey(30)
        );

        assertTrue(
                tree.containsKey(40)
        );
    }

    @Test
    void shouldNotChangeSeparatorWhenNonMinimumKeyIsDeleted() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        tree.insert(
                20,
                new RecordPointer(20, 0)
        );

        tree.insert(
                30,
                new RecordPointer(30, 0)
        );

        tree.insert(
                40,
                new RecordPointer(40, 0)
        );

        /*
         * Sağ leaf'i minimum occupancy'nin üzerinde tutuyoruz.
         * Böylece non-minimum key silme testi merge yerine yalnızca
         * separator'ın değişmemesini doğrular.
         */
        tree.insert(
                35,
                new RecordPointer(35, 0)
        );

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        int separatorBefore =
                root.getKey(0);

        assertTrue(
                tree.delete(40)
        );

        assertEquals(
                separatorBefore,
                root.getKey(0)
        );

        assertTrue(
                tree.containsKey(30)
        );
    }

    @Test
    void shouldMergeLeafWhenBorrowIsNotPossible() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        tree.insert(
                20,
                new RecordPointer(20, 0)
        );

        tree.insert(
                30,
                new RecordPointer(30, 0)
        );

        tree.insert(
                40,
                new RecordPointer(40, 0)
        );

        assertEquals(
                2,
                tree.getHeight()
        );

        assertTrue(
                tree.delete(10)
        );

        /*
         * İki leaf de borrow için minimum occupancy'de olduğundan
         * Phase 9 merge uygulanır.
         */
        assertEquals(
                1,
                tree.getHeight()
        );

        assertTrue(
                tree.getRoot().isLeaf()
        );

        assertEquals(
                List.of(
                        20,
                        30,
                        40
                ),
                tree.getRootLeaf().getKeys()
        );
    }

    @Test
    void shouldPreserveLeafStructuralValidityAfterDelete() {

        BPlusTree<Integer> tree =
                createTree(30);

        tree.delete(15);
        tree.delete(16);
        tree.delete(17);

        BPlusTreeLeafNode<Integer> leaf =
                tree.findLeafForKey(18);

        assertTrue(
                leaf.isStructurallyValid()
        );

        assertEquals(
                leaf.getKeyCount(),
                leaf.getValueCount()
        );
    }

    @Test
    void shouldExcludeDeletedKeysFromRangeScan() {

        BPlusTree<Integer> tree =
                createTree(20);

        tree.delete(8);
        tree.delete(10);
        tree.delete(12);

        List<RecordPointer> result =
                tree.searchRange(
                        7,
                        13
                );

        assertEquals(
                4,
                result.size()
        );

        assertEquals(
                List.of(
                        new RecordPointer(7, 0),
                        new RecordPointer(9, 0),
                        new RecordPointer(11, 0),
                        new RecordPointer(13, 0)
                ),
                result
        );
    }

    @Test
    void shouldExcludeDeletedKeysFromFullScan() {

        BPlusTree<Integer> tree =
                createTree(10);

        tree.delete(3);
        tree.delete(6);
        tree.delete(9);

        List<RecordPointer> result =
                tree.scanAll();

        assertEquals(
                7,
                result.size()
        );

        assertFalse(
                result.contains(
                        new RecordPointer(3, 0)
                )
        );

        assertFalse(
                result.contains(
                        new RecordPointer(6, 0)
                )
        );

        assertFalse(
                result.contains(
                        new RecordPointer(9, 0)
                )
        );
    }

    @Test
    void shouldDeleteKeysFromDeepTree() {

        BPlusTree<Integer> tree =
                createTree(100);

        int heightBefore =
                tree.getHeight();

        assertTrue(
                heightBefore >= 3
        );

        assertTrue(tree.delete(25));
        assertTrue(tree.delete(50));
        assertTrue(tree.delete(75));

        assertFalse(tree.containsKey(25));
        assertFalse(tree.containsKey(50));
        assertFalse(tree.containsKey(75));

        assertTrue(tree.containsKey(24));
        assertTrue(tree.containsKey(26));

        /*
         * Phase 7'de merge olmadığı için tree yüksekliği değişmez.
         */
        assertEquals(
                heightBefore,
                tree.getHeight()
        );
    }

    @Test
    void shouldAllowRootLeafToBecomeEmpty() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        tree.delete(10);

        assertTrue(
                tree.isEmpty()
        );

        assertEquals(
                1,
                tree.getHeight()
        );

        assertTrue(
                tree.getRoot().isLeaf()
        );
    }

    private BPlusTree<Integer> createTree(
            int max
    ) {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 1; i <= max; i++) {

            tree.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        return tree;
    }
}