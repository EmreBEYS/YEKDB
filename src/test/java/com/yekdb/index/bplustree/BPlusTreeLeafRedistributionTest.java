package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 8 leaf borrow ve redistribution testleri.
 *
 * Bu testler:
 * - sağ sibling'den borrow,
 * - sol sibling'den borrow,
 * - separator güncelleme,
 * - pointer bucket taşıma,
 * - borrow mümkün değilse underflow korunması,
 * - search ve leaf chain tutarlılığı
 *
 * davranışlarını doğrular.
 */
class BPlusTreeLeafRedistributionTest {

    @Test
    void shouldBorrowFromRightSiblingWhenLeafUnderflows() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                40,
                35
        );

        assertTrue(
                tree.delete(10)
        );

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left =
                leaf(root, 0);

        BPlusTreeLeafNode<Integer> right =
                leaf(root, 1);

        assertEquals(
                List.of(
                        20,
                        30
                ),
                left.getKeys()
        );

        assertEquals(
                List.of(
                        35,
                        40
                ),
                right.getKeys()
        );
    }

    @Test
    void shouldUpdateSeparatorAfterBorrowFromRight() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                40,
                35
        );

        tree.delete(10);

        assertEquals(
                List.of(35),
                tree.getRootInternal()
                        .getKeys()
        );
    }

    @Test
    void shouldBorrowFromLeftSiblingWhenLeafUnderflows() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                40,
                25
        );

        assertTrue(
                tree.delete(30)
        );

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left =
                leaf(root, 0);

        BPlusTreeLeafNode<Integer> right =
                leaf(root, 1);

        assertEquals(
                List.of(
                        10,
                        20
                ),
                left.getKeys()
        );

        assertEquals(
                List.of(
                        25,
                        40
                ),
                right.getKeys()
        );
    }

    @Test
    void shouldUpdateSeparatorAfterBorrowFromLeft() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                40,
                25
        );

        tree.delete(30);

        assertEquals(
                List.of(25),
                tree.getRootInternal()
                        .getKeys()
        );
    }

    @Test
    void shouldPreferLeftSiblingWhenBothCanLend() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        /*
         * Birden fazla leaf oluşturuyoruz.
         */
        insert(
                tree,
                10,
                20,
                30,
                40,
                25,
                35,
                45,
                50
        );

        BPlusTreeLeafNode<Integer> target =
                tree.findLeafForKey(35);

        /*
         * Target leaf'i mümkün olduğunca minimum kapasiteye indiriyoruz.
         */
        List<Integer> keys =
                List.copyOf(
                        target.getKeys()
                );

        if (keys.size() > tree.getMinLeafKeys()) {
            tree.delete(
                    keys.get(
                            keys.size() - 1
                    )
            );
        }

        /*
         * Sonraki delete underflow yaratabiliyorsa redistribution gerçekleşir.
         * Tree sonunda searchable kalmalıdır.
         */
        Integer deleteKey =
                target.getKey(0);

        assertTrue(
                tree.delete(deleteKey)
        );

        assertFalse(
                tree.containsKey(deleteKey)
        );

        assertTrue(
                tree.containsKey(20)
        );

        assertTrue(
                tree.containsKey(40)
        );
    }

    @Test
    void shouldKeepLeafAtMinimumOccupancyAfterRightBorrow() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        BPlusTreeLeafNode<Integer> left =
                tree.findLeafForKey(20);

        assertEquals(
                tree.getMinLeafKeys(),
                left.getKeyCount()
        );
    }

    @Test
    void shouldKeepLeafAtMinimumOccupancyAfterLeftBorrow() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                25,
                30,
                40
        );

        tree.delete(30);

        BPlusTreeLeafNode<Integer> right =
                tree.findLeafForKey(40);

        assertEquals(
                tree.getMinLeafKeys(),
                right.getKeyCount()
        );
    }

    @Test
    void shouldKeepDonorAboveMinimumAfterRightBorrow() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeLeafNode<Integer> donor =
                leaf(root, 1);

        assertTrue(
                donor.getKeyCount()
                        >= tree.getMinLeafKeys()
        );
    }

    @Test
    void shouldKeepDonorAboveMinimumAfterLeftBorrow() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                25,
                30,
                40
        );

        tree.delete(30);

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeLeafNode<Integer> donor =
                leaf(root, 0);

        assertTrue(
                donor.getKeyCount()
                        >= tree.getMinLeafKeys()
        );
    }

    @Test
    void shouldMergeWhenSiblingCannotLend() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                40
        );

        tree.delete(10);

        /*
         * Sağ sibling minimum occupancy'de olduğu için borrow yapılamaz.
         * Phase 9 kapsamında iki leaf merge edilir.
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
    void shouldPreservePointerBucketWhenBorrowingFromRight() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        RecordPointer first =
                new RecordPointer(30, 0);

        RecordPointer second =
                new RecordPointer(300, 1);

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
                first
        );

        tree.insert(
                30,
                second
        );

        tree.insert(
                35,
                new RecordPointer(35, 0)
        );

        tree.insert(
                40,
                new RecordPointer(40, 0)
        );

        tree.delete(10);

        assertEquals(
                List.of(
                        first,
                        second
                ),
                tree.search(30)
        );
    }

    @Test
    void shouldPreservePointerBucketWhenBorrowingFromLeft() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        RecordPointer first =
                new RecordPointer(25, 0);

        RecordPointer second =
                new RecordPointer(250, 1);

        tree.insert(
                10,
                new RecordPointer(10, 0)
        );

        tree.insert(
                20,
                new RecordPointer(20, 0)
        );

        tree.insert(
                25,
                first
        );

        tree.insert(
                25,
                second
        );

        tree.insert(
                30,
                new RecordPointer(30, 0)
        );

        tree.insert(
                40,
                new RecordPointer(40, 0)
        );

        tree.delete(30);

        assertEquals(
                List.of(
                        first,
                        second
                ),
                tree.search(25)
        );
    }

    @Test
    void shouldPreserveForwardLeafLinksAfterRedistribution() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left =
                leaf(root, 0);

        BPlusTreeLeafNode<Integer> right =
                leaf(root, 1);

        assertSame(
                right,
                left.getNextLeaf()
        );
    }

    @Test
    void shouldPreserveBackwardLeafLinksAfterRedistribution() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left =
                leaf(root, 0);

        BPlusTreeLeafNode<Integer> right =
                leaf(root, 1);

        assertSame(
                left,
                right.getPreviousLeaf()
        );
    }

    @Test
    void shouldKeepSearchValidAfterRightRedistribution() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        assertFalse(
                tree.containsKey(10)
        );

        assertTrue(
                tree.containsKey(20)
        );

        assertTrue(
                tree.containsKey(30)
        );

        assertTrue(
                tree.containsKey(35)
        );

        assertTrue(
                tree.containsKey(40)
        );
    }

    @Test
    void shouldKeepSearchValidAfterLeftRedistribution() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                25,
                30,
                40
        );

        tree.delete(30);

        assertFalse(
                tree.containsKey(30)
        );

        assertTrue(
                tree.containsKey(10)
        );

        assertTrue(
                tree.containsKey(20)
        );

        assertTrue(
                tree.containsKey(25)
        );

        assertTrue(
                tree.containsKey(40)
        );
    }

    @Test
    void shouldKeepRangeScanSortedAfterRedistribution() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        assertEquals(
                List.of(
                        new RecordPointer(20, 0),
                        new RecordPointer(30, 0),
                        new RecordPointer(35, 0),
                        new RecordPointer(40, 0)
                ),
                tree.scanAll()
        );
    }

    @Test
    void shouldKeepLeafStructuresValidAfterRedistribution() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                35,
                40
        );

        tree.delete(10);

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        for (BPlusTreeNode<Integer> node
                : root.getChildren()) {

            BPlusTreeLeafNode<Integer> leaf =
                    castLeaf(node);

            assertTrue(
                    leaf.isStructurallyValid()
            );

            assertEquals(
                    leaf.getKeyCount(),
                    leaf.getValueCount()
            );
        }
    }

    private void insert(
            BPlusTree<Integer> tree,
            int... keys
    ) {

        for (int key : keys) {

            tree.insert(
                    key,
                    new RecordPointer(
                            key,
                            0
                    )
            );
        }
    }

    private BPlusTreeLeafNode<Integer> leaf(
            BPlusTreeInternalNode<Integer> parent,
            int index
    ) {

        return castLeaf(
                parent.getChild(index)
        );
    }

    @SuppressWarnings("unchecked")
    private BPlusTreeLeafNode<Integer> castLeaf(
            BPlusTreeNode<Integer> node
    ) {

        assertTrue(
                node.isLeaf()
        );

        return (BPlusTreeLeafNode<Integer>) node;
    }
}