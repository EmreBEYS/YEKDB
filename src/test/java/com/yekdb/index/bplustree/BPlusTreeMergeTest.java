package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 9 merge, recursive cleanup ve root shrink testleri.
 *
 * Bu testler:
 * - leaf merge,
 * - internal merge,
 * - recursive parent cleanup,
 * - root shrink,
 * - separator yenileme,
 * - leaf chain korunması
 *
 * davranışlarını doğrular.
 */
class BPlusTreeMergeTest {

    @Test
    void shouldMergeTwoLeavesWhenBorrowIsNotPossible() {

        BPlusTree<Integer> tree =
                createFourKeyTree();

        tree.delete(10);

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
    void shouldShrinkRootAfterLeafMerge() {

        BPlusTree<Integer> tree =
                createFourKeyTree();

        assertEquals(
                2,
                tree.getHeight()
        );

        tree.delete(10);

        assertEquals(
                1,
                tree.getHeight()
        );
    }

    @Test
    void shouldPreserveSearchAfterLeafMerge() {

        BPlusTree<Integer> tree =
                createFourKeyTree();

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
                tree.containsKey(40)
        );
    }

    @Test
    void shouldPreserveSortedScanAfterLeafMerge() {

        BPlusTree<Integer> tree =
                createFourKeyTree();

        tree.delete(10);

        assertEquals(
                List.of(
                        new RecordPointer(20, 0),
                        new RecordPointer(30, 0),
                        new RecordPointer(40, 0)
                ),
                tree.scanAll()
        );
    }

    @Test
    void shouldPreserveLeafLinksAfterMultipleMerges() {

        BPlusTree<Integer> tree =
                createTree(30);

        for (int i = 1; i <= 15; i++) {
            tree.delete(i);
        }

        BPlusTreeLeafNode<Integer> leaf =
                findFirstLeaf(tree);

        BPlusTreeLeafNode<Integer> previous =
                null;

        int previousKey =
                Integer.MIN_VALUE;

        while (leaf != null) {

            assertSame(
                    previous,
                    leaf.getPreviousLeaf()
            );

            for (Integer key : leaf.getKeys()) {

                assertTrue(
                        key > previousKey
                );

                previousKey = key;
            }

            previous = leaf;
            leaf = leaf.getNextLeaf();
        }
    }

    @Test
    void shouldReduceHeightAfterRecursiveInternalMerge() {

        BPlusTree<Integer> tree =
                createTree(30);

        int initialHeight =
                tree.getHeight();

        assertTrue(
                initialHeight >= 3
        );

        for (int i = 1; i <= 25; i++) {
            tree.delete(i);
        }

        assertTrue(
                tree.getHeight()
                        < initialHeight
        );

        assertEquals(
                5,
                tree.scanAll().size()
        );
    }

    @Test
    void shouldPreserveRemainingKeysAfterRecursiveMerge() {

        BPlusTree<Integer> tree =
                createTree(50);

        for (int i = 1; i <= 40; i++) {
            assertTrue(
                    tree.delete(i)
            );
        }

        for (int i = 1; i <= 40; i++) {
            assertFalse(
                    tree.containsKey(i)
            );
        }

        for (int i = 41; i <= 50; i++) {
            assertTrue(
                    tree.containsKey(i)
            );
        }
    }

    @Test
    void shouldCollapseTreeToSingleLeafAfterHeavyDelete() {

        BPlusTree<Integer> tree =
                createTree(100);

        for (int i = 1; i <= 97; i++) {
            tree.delete(i);
        }

        assertEquals(
                1,
                tree.getHeight()
        );

        assertTrue(
                tree.getRoot().isLeaf()
        );

        assertEquals(
                List.of(
                        98,
                        99,
                        100
                ),
                tree.getRootLeaf().getKeys()
        );
    }

    @Test
    void shouldBecomeEmptyRootLeafAfterDeletingEverything() {

        BPlusTree<Integer> tree =
                createTree(100);

        for (int i = 1; i <= 100; i++) {

            assertTrue(
                    tree.delete(i)
            );
        }

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

        assertTrue(
                tree.scanAll().isEmpty()
        );
    }

    @Test
    void shouldMaintainLeafMinimumOccupancyAfterMerges() {

        BPlusTree<Integer> tree =
                createTree(80);

        for (int i = 1; i <= 45; i++) {
            tree.delete(i);
        }

        assertOccupancyValid(
                tree,
                tree.getRoot(),
                true
        );
    }

    @Test
    void shouldMaintainInternalMinimumOccupancyAfterMerges() {

        BPlusTree<Integer> tree =
                createTree(150);

        for (int i = 1; i <= 100; i++) {
            tree.delete(i);
        }

        assertOccupancyValid(
                tree,
                tree.getRoot(),
                true
        );
    }

    @Test
    void shouldKeepSeparatorsValidAfterRecursiveMerge() {

        BPlusTree<Integer> tree =
                createTree(100);

        for (int i = 1; i <= 63; i++) {
            tree.delete(i);
        }

        assertSeparatorValidity(
                tree.getRoot()
        );
    }

    @Test
    void shouldHandleDescendingDeletes() {

        BPlusTree<Integer> tree =
                createTree(100);

        for (int i = 100; i >= 1; i--) {

            assertTrue(
                    tree.delete(i)
            );

            assertOccupancyValid(
                    tree,
                    tree.getRoot(),
                    true
            );
        }

        assertTrue(
                tree.isEmpty()
        );
    }

    @Test
    void shouldHandleRandomDeletes() {

        BPlusTree<Integer> tree =
                createTree(200);

        List<Integer> keys =
                new ArrayList<>();

        for (int i = 1; i <= 200; i++) {
            keys.add(i);
        }

        Collections.shuffle(
                keys,
                new java.util.Random(42)
        );

        for (Integer key : keys) {

            assertTrue(
                    tree.delete(key)
            );

            assertOccupancyValid(
                    tree,
                    tree.getRoot(),
                    true
            );
        }

        assertTrue(
                tree.isEmpty()
        );
    }

    @Test
    void shouldMergeAfterDeletingLastPointerFromBucket() {

        BPlusTree<Integer> tree =
                createFourKeyTree();

        RecordPointer extra =
                new RecordPointer(
                        100,
                        1
                );

        tree.insert(
                10,
                extra
        );

        assertTrue(
                tree.delete(
                        10,
                        new RecordPointer(
                                10,
                                0
                        )
                )
        );

        /*
         * Bucket hâlâ dolu olduğu için merge olmamalı.
         */
        assertEquals(
                2,
                tree.getHeight()
        );

        assertTrue(
                tree.delete(
                        10,
                        extra
                )
        );

        /*
         * Son pointer sonrası key tamamen kalkar ve leaf merge oluşur.
         */
        assertEquals(
                1,
                tree.getHeight()
        );

        assertFalse(
                tree.containsKey(10)
        );
    }

    @Test
    void shouldSupportMergeWithStringKeys() {

        BPlusTree<String> tree =
                new BPlusTree<>(4);

        tree.insert(
                "Ankara",
                new RecordPointer(1, 0)
        );

        tree.insert(
                "Bursa",
                new RecordPointer(2, 0)
        );

        tree.insert(
                "Istanbul",
                new RecordPointer(3, 0)
        );

        tree.insert(
                "Malatya",
                new RecordPointer(4, 0)
        );

        assertEquals(
                2,
                tree.getHeight()
        );

        tree.delete("Ankara");

        assertEquals(
                1,
                tree.getHeight()
        );

        assertEquals(
                List.of(
                        "Bursa",
                        "Istanbul",
                        "Malatya"
                ),
                tree.getRootLeaf().getKeys()
        );
    }

    private BPlusTree<Integer> createFourKeyTree() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        insert(
                tree,
                10,
                20,
                30,
                40
        );

        return tree;
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

    private void assertOccupancyValid(
            BPlusTree<Integer> tree,
            BPlusTreeNode<Integer> node,
            boolean root
    ) {

        assertFalse(
                node.isOverflow()
        );

        if (node.isLeaf()) {

            BPlusTreeLeafNode<Integer> leaf =
                    castLeaf(node);

            assertTrue(
                    leaf.isStructurallyValid()
            );

            if (!root) {

                assertTrue(
                        leaf.getKeyCount()
                                >= tree.getMinLeafKeys(),
                        "Leaf underflow detected: " + leaf
                );
            }

            return;
        }

        BPlusTreeInternalNode<Integer> internal =
                castInternal(node);

        assertTrue(
                internal.isStructurallyValid()
        );

        if (root) {

            assertTrue(
                    internal.getChildCount() >= 2
            );

        } else {

            assertTrue(
                    internal.getChildCount()
                            >= tree.getMinInternalChildren(),
                    "Internal underflow detected: " + internal
            );
        }

        for (BPlusTreeNode<Integer> child
                : internal.getChildren()) {

            assertOccupancyValid(
                    tree,
                    child,
                    false
            );
        }
    }

    private void assertSeparatorValidity(
            BPlusTreeNode<Integer> node
    ) {

        if (node.isLeaf()) {
            return;
        }

        BPlusTreeInternalNode<Integer> internal =
                castInternal(node);

        for (int i = 0;
             i < internal.getKeyCount();
             i++) {

            assertEquals(
                    minimumKey(
                            internal.getChild(i + 1)
                    ),
                    internal.getKey(i)
            );
        }

        for (BPlusTreeNode<Integer> child
                : internal.getChildren()) {

            assertSeparatorValidity(child);
        }
    }

    private Integer minimumKey(
            BPlusTreeNode<Integer> node
    ) {

        BPlusTreeNode<Integer> current =
                node;

        while (!current.isLeaf()) {

            current =
                    castInternal(current)
                            .getChild(0);
        }

        return castLeaf(current)
                .getKey(0);
    }

    private BPlusTreeLeafNode<Integer> findFirstLeaf(
            BPlusTree<Integer> tree
    ) {

        BPlusTreeNode<Integer> current =
                tree.getRoot();

        while (!current.isLeaf()) {

            current =
                    castInternal(current)
                            .getChild(0);
        }

        return castLeaf(current);
    }

    @SuppressWarnings("unchecked")
    private BPlusTreeInternalNode<Integer> castInternal(
            BPlusTreeNode<Integer> node
    ) {

        assertFalse(
                node.isLeaf()
        );

        return (BPlusTreeInternalNode<Integer>) node;
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