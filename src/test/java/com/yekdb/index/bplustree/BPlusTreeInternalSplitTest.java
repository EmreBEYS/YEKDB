package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 5 internal split ve recursive propagation testleri.
 *
 * Bu testler:
 * - internal root overflow,
 * - internal split,
 * - yeni root oluşturma,
 * - height artışı,
 * - recursive multi-level insert,
 * - separator traversal,
 * - leaf chain korunması
 *
 * davranışlarını doğrular.
 */
class BPlusTreeInternalSplitTest {

    @Test
    void shouldIncreaseHeightWhenInternalRootOverflows() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        assertEquals(
                3,
                tree.getHeight()
        );

        assertFalse(
                tree.getRoot().isLeaf()
        );
    }

    @Test
    void shouldCreateNewRootAfterInternalSplit() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        assertEquals(
                List.of(40),
                root.getKeys()
        );

        assertEquals(
                2,
                root.getChildCount()
        );

        assertTrue(
                root.isStructurallyValid()
        );
    }

    @Test
    void shouldCreateInternalChildrenBelowNewRoot() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        assertFalse(
                root.getChild(0).isLeaf()
        );

        assertFalse(
                root.getChild(1).isLeaf()
        );
    }

    @Test
    void shouldDistributeInternalKeysAfterSplit() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        BPlusTreeInternalNode<Integer> left =
                castInternal(
                        root.getChild(0)
                );

        BPlusTreeInternalNode<Integer> right =
                castInternal(
                        root.getChild(1)
                );

        assertEquals(
                List.of(
                        15,
                        30
                ),
                left.getKeys()
        );

        assertEquals(
                List.of(50),
                right.getKeys()
        );

        assertTrue(
                left.isStructurallyValid()
        );

        assertTrue(
                right.isStructurallyValid()
        );
    }

    @Test
    void shouldPreserveAllInsertedKeysAfterInternalSplit() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        int[] keys = {
                5,
                10,
                15,
                20,
                30,
                35,
                40,
                45,
                50,
                60
        };

        for (int key : keys) {

            assertTrue(
                    tree.containsKey(key),
                    "Key should exist: " + key
            );
        }
    }

    @Test
    void shouldRouteKeysAcrossThreeLevels() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        assertEquals(
                List.of(
                        5,
                        10
                ),
                tree.findLeafForKey(5)
                        .getKeys()
        );

        assertEquals(
                List.of(
                        15,
                        20
                ),
                tree.findLeafForKey(15)
                        .getKeys()
        );

        assertEquals(
                List.of(
                        30,
                        35
                ),
                tree.findLeafForKey(30)
                        .getKeys()
        );

        assertEquals(
                List.of(
                        40,
                        45
                ),
                tree.findLeafForKey(40)
                        .getKeys()
        );

        assertEquals(
                List.of(
                        50,
                        60
                ),
                tree.findLeafForKey(50)
                        .getKeys()
        );
    }

    @Test
    void shouldKeepSeparatorSemanticsAcrossInternalLevels() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        BPlusTreeInternalNode<Integer> root =
                tree.getRootInternal();

        /*
         * Root separator sağ subtree'nin minimum key'idir.
         */
        assertEquals(
                40,
                root.getKey(0)
        );

        BPlusTreeInternalNode<Integer> left =
                castInternal(
                        root.getChild(0)
                );

        BPlusTreeInternalNode<Integer> right =
                castInternal(
                        root.getChild(1)
                );

        assertEquals(
                15,
                left.getKey(0)
        );

        assertEquals(
                30,
                left.getKey(1)
        );

        assertEquals(
                50,
                right.getKey(0)
        );
    }

    @Test
    void shouldMaintainLeafChainAfterInternalSplit() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        BPlusTreeLeafNode<Integer> leaf =
                tree.findLeafForKey(
                        Integer.MIN_VALUE
                );

        List<Integer> keys =
                new ArrayList<>();

        BPlusTreeLeafNode<Integer> previous =
                null;

        while (leaf != null) {

            assertSame(
                    previous,
                    leaf.getPreviousLeaf()
            );

            keys.addAll(
                    leaf.getKeys()
            );

            previous = leaf;
            leaf = leaf.getNextLeaf();
        }

        assertEquals(
                List.of(
                        5,
                        10,
                        15,
                        20,
                        30,
                        35,
                        40,
                        45,
                        50,
                        60
                ),
                keys
        );
    }

    @Test
    void shouldInsertIntoHeightThreeTreeWithoutChangingHeight() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        int heightBefore =
                tree.getHeight();

        tree.insert(
                55,
                new RecordPointer(
                        55,
                        0
                )
        );

        assertEquals(
                heightBefore,
                tree.getHeight()
        );

        assertTrue(
                tree.containsKey(55)
        );

        assertEquals(
                List.of(
                        50,
                        55,
                        60
                ),
                tree.findLeafForKey(55)
                        .getKeys()
        );
    }

    @Test
    void shouldSupportFurtherLeafSplitsBelowInternalNodes() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        tree.insert(
                55,
                new RecordPointer(
                        55,
                        0
                )
        );

        tree.insert(
                70,
                new RecordPointer(
                        70,
                        0
                )
        );

        assertTrue(
                tree.containsKey(70)
        );

        assertEquals(
                3,
                tree.getHeight()
        );

        BPlusTreeLeafNode<Integer> leaf =
                tree.findLeafForKey(70);

        assertTrue(
                leaf.getKeys()
                        .contains(70)
        );

        assertFalse(
                leaf.isOverflow()
        );
    }

    @Test
    void shouldKeepDuplicateKeyInsideBucketAtHeightThree() {

        BPlusTree<Integer> tree =
                createHeightThreeTree();

        RecordPointer extra =
                new RecordPointer(
                        999,
                        4
                );

        boolean newKey =
                tree.insert(
                        40,
                        extra
                );

        assertFalse(newKey);

        assertEquals(
                2,
                tree.search(40)
                        .size()
        );

        assertTrue(
                tree.search(40)
                        .contains(extra)
        );

        assertEquals(
                3,
                tree.getHeight()
        );
    }

    @Test
    void shouldHandleAscendingLargeInsertSet() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 1; i <= 100; i++) {

            tree.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        for (int i = 1; i <= 100; i++) {

            assertTrue(
                    tree.containsKey(i),
                    "Missing key: " + i
            );
        }

        assertTrue(
                tree.getHeight() >= 3
        );

        assertNoOverflow(
                tree.getRoot()
        );
    }

    @Test
    void shouldHandleDescendingLargeInsertSet() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 100; i >= 1; i--) {

            tree.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        for (int i = 1; i <= 100; i++) {

            assertTrue(
                    tree.containsKey(i),
                    "Missing key: " + i
            );
        }

        assertNoOverflow(
                tree.getRoot()
        );
    }

    @Test
    void shouldHandleRandomInsertionOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        List<Integer> values =
                new ArrayList<>();

        for (int i = 1; i <= 200; i++) {
            values.add(i);
        }

        Collections.shuffle(
                values,
                new java.util.Random(42)
        );

        for (int value : values) {

            tree.insert(
                    value,
                    new RecordPointer(
                            value,
                            0
                    )
            );
        }

        for (int i = 1; i <= 200; i++) {

            assertTrue(
                    tree.containsKey(i),
                    "Missing key: " + i
            );
        }

        assertNoOverflow(
                tree.getRoot()
        );
    }

    @Test
    void shouldPreserveInternalStructuralValidityRecursively() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 1; i <= 150; i++) {

            tree.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        assertStructuralValidity(
                tree.getRoot()
        );
    }

    /**
     * order=4 kullanarak internal root split oluşturur.
     */
    private BPlusTree<Integer> createHeightThreeTree() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        int[] keys = {
                10,
                20,
                30,
                40,
                35,
                50,
                15,
                5,
                45,
                60
        };

        for (int key : keys) {

            tree.insert(
                    key,
                    new RecordPointer(
                            key,
                            0
                    )
            );
        }

        return tree;
    }

    /**
     * Tree boyunca hiçbir node'un overflow durumda kalmadığını doğrular.
     */
    private void assertNoOverflow(
            BPlusTreeNode<Integer> node
    ) {

        assertFalse(
                node.isOverflow(),
                "Node must not remain overflowed: "
                        + node
        );

        if (!node.isLeaf()) {

            BPlusTreeInternalNode<Integer> internal =
                    castInternal(node);

            for (BPlusTreeNode<Integer> child
                    : internal.getChildren()) {

                assertNoOverflow(child);
            }
        }
    }

    /**
     * Internal ve leaf structural invariant'larını recursive doğrular.
     */
    private void assertStructuralValidity(
            BPlusTreeNode<Integer> node
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

            assertEquals(
                    leaf.getKeyCount(),
                    leaf.getValueCount()
            );

            return;
        }

        BPlusTreeInternalNode<Integer> internal =
                castInternal(node);

        assertTrue(
                internal.isStructurallyValid()
        );

        for (BPlusTreeNode<Integer> child
                : internal.getChildren()) {

            assertStructuralValidity(child);
        }
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