package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 1 temel yapı testleri.
 *
 * Bu test sınıfı:
 * - order doğrulamasını,
 * - root başlangıç yapısını,
 * - node tiplerini,
 * - kapasite hesaplarını,
 * - leaf bağlantılarını,
 * - internal node invariant'larını
 *
 * doğrular.
 */
class BPlusTreeFoundationTest {

    @Test
    void shouldCreateTreeWithDefaultOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>();

        assertEquals(
                BPlusTree.DEFAULT_ORDER,
                tree.getOrder()
        );

        assertEquals(
                BPlusTree.DEFAULT_ORDER - 1,
                tree.getMaxKeys()
        );

        assertNotNull(
                tree.getRoot()
        );

        assertTrue(
                tree.getRoot().isLeaf()
        );

        assertTrue(
                tree.isEmpty()
        );

        assertEquals(
                1,
                tree.getHeight()
        );
    }

    @Test
    void shouldCreateTreeWithCustomOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        assertEquals(
                5,
                tree.getOrder()
        );

        assertEquals(
                4,
                tree.getMaxKeys()
        );
    }

    @Test
    void shouldRejectOrderLowerThanMinimum() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new BPlusTree<Integer>(2)
                );

        assertTrue(
                exception.getMessage()
                        .contains("at least")
        );
    }

    @Test
    void shouldCreateLeafRootInitially() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        BPlusTreeNode<Integer> root =
                tree.getRoot();

        assertNotNull(root);

        assertInstanceOf(
                BPlusTreeLeafNode.class,
                root
        );

        assertTrue(
                root.isLeaf()
        );

        assertEquals(
                0,
                root.getKeyCount()
        );
    }

    @Test
    void shouldReturnRootAsLeaf() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        BPlusTreeLeafNode<Integer> root =
                tree.getRootLeaf();

        assertNotNull(root);

        assertSame(
                tree.getRoot(),
                root
        );
    }

    @Test
    void shouldRejectRootInternalAccessWhenRootIsLeaf() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        tree::getRootInternal
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("internal")
        );
    }

    @Test
    void shouldCalculateLeafMinimumKeys() {

        BPlusTree<Integer> order4 =
                new BPlusTree<>(4);

        /*
         * maxKeys = 3
         * ceil(3 / 2) = 2
         */
        assertEquals(
                2,
                order4.getMinLeafKeys()
        );

        BPlusTree<Integer> order5 =
                new BPlusTree<>(5);

        /*
         * maxKeys = 4
         * ceil(4 / 2) = 2
         */
        assertEquals(
                2,
                order5.getMinLeafKeys()
        );

        BPlusTree<Integer> order6 =
                new BPlusTree<>(6);

        /*
         * maxKeys = 5
         * ceil(5 / 2) = 3
         */
        assertEquals(
                3,
                order6.getMinLeafKeys()
        );
    }

    @Test
    void shouldCalculateInternalMinimumOccupancy() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        /*
         * ceil(5 / 2) = 3 child
         */
        assertEquals(
                3,
                tree.getMinInternalChildren()
        );

        /*
         * minKeys = minChildren - 1
         */
        assertEquals(
                2,
                tree.getMinInternalKeys()
        );
    }

    @Test
    void shouldCreateLeafNode() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        assertTrue(
                leaf.isLeaf()
        );

        assertEquals(
                4,
                leaf.getOrder()
        );

        assertEquals(
                3,
                leaf.getMaxKeys()
        );

        assertEquals(
                0,
                leaf.getKeyCount()
        );

        assertEquals(
                0,
                leaf.getValueCount()
        );

        assertTrue(
                leaf.isStructurallyValid()
        );
    }

    @Test
    void shouldCreateInternalNode() {

        BPlusTreeInternalNode<Integer> internal =
                new BPlusTreeInternalNode<>(4);

        assertFalse(
                internal.isLeaf()
        );

        assertEquals(
                4,
                internal.getOrder()
        );

        assertEquals(
                3,
                internal.getMaxKeys()
        );

        assertEquals(
                0,
                internal.getKeyCount()
        );

        assertEquals(
                0,
                internal.getChildCount()
        );

        assertTrue(
                internal.isStructurallyValid()
        );
    }

    @Test
    void shouldLinkLeafNodesInBothDirections() {

        BPlusTreeLeafNode<Integer> first =
                new BPlusTreeLeafNode<>(4);

        BPlusTreeLeafNode<Integer> second =
                new BPlusTreeLeafNode<>(4);

        first.setNextLeaf(second);
        second.setPreviousLeaf(first);

        assertSame(
                second,
                first.getNextLeaf()
        );

        assertSame(
                first,
                second.getPreviousLeaf()
        );

        assertNull(
                first.getPreviousLeaf()
        );

        assertNull(
                second.getNextLeaf()
        );
    }

    @Test
    void shouldMaintainLeafKeyValueSynchronization() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        RecordPointer pointer =
                new RecordPointer(
                        1,
                        (short) 2
                );

        leaf.addEntry(
                0,
                10,
                pointer
        );

        assertEquals(
                1,
                leaf.getKeyCount()
        );

        assertEquals(
                1,
                leaf.getValueCount()
        );

        assertEquals(
                10,
                leaf.getKey(0)
        );

        assertEquals(
                List.of(pointer),
                leaf.getPointers(0)
        );

        assertTrue(
                leaf.isStructurallyValid()
        );
    }

    @Test
    void shouldRejectNullLeafKey() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        RecordPointer pointer =
                new RecordPointer(
                        1,
                        (short) 0
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> leaf.addEntry(
                                0,
                                null,
                                pointer
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("key")
        );
    }

    @Test
    void shouldRejectInvalidLeafPointer() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> leaf.addEntry(
                                0,
                                10,
                                (RecordPointer) null
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("recordpointer")
        );
    }

    @Test
    void shouldMaintainInternalNodeStructure() {

        BPlusTreeInternalNode<Integer> internal =
                new BPlusTreeInternalNode<>(4);

        BPlusTreeLeafNode<Integer> left =
                new BPlusTreeLeafNode<>(4);

        BPlusTreeLeafNode<Integer> right =
                new BPlusTreeLeafNode<>(4);

        internal.addChild(left);

        /*
         * Henüz separator olmadığı için:
         *
         * children = 1
         * keys = 0
         *
         * geçerli internal yapı.
         */
        assertTrue(
                internal.isStructurallyValid()
        );

        internal.addSeparatorKey(
                0,
                50
        );

        internal.addChild(right);

        assertEquals(
                1,
                internal.getKeyCount()
        );

        assertEquals(
                2,
                internal.getChildCount()
        );

        assertEquals(
                50,
                internal.getKey(0)
        );

        assertSame(
                left,
                internal.getChild(0)
        );

        assertSame(
                right,
                internal.getChild(1)
        );

        assertTrue(
                internal.isStructurallyValid()
        );
    }

    @Test
    void shouldDetectInvalidInternalStructure() {

        BPlusTreeInternalNode<Integer> internal =
                new BPlusTreeInternalNode<>(4);

        internal.addSeparatorKey(
                0,
                50
        );

        /*
         * keys = 1
         * children = 0
         *
         * Beklenen children = 2 olduğundan geçersizdir.
         */
        assertFalse(
                internal.isStructurallyValid()
        );
    }

    @Test
    void shouldDetectFullNode() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        RecordPointer p1 =
                new RecordPointer(
                        1,
                        (short) 0
                );

        RecordPointer p2 =
                new RecordPointer(
                        2,
                        (short) 0
                );

        RecordPointer p3 =
                new RecordPointer(
                        3,
                        (short) 0
                );

        leaf.addEntry(
                0,
                10,
                p1
        );

        leaf.addEntry(
                1,
                20,
                p2
        );

        leaf.addEntry(
                2,
                30,
                p3
        );

        assertEquals(
                3,
                leaf.getKeyCount()
        );

        assertTrue(
                leaf.isFull()
        );

        assertFalse(
                leaf.isOverflow()
        );
    }

    @Test
    void shouldDetectNodeOverflow() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        for (int i = 0; i < 4; i++) {

            leaf.addEntry(
                    i,
                    i * 10,
                    new RecordPointer(
                            i + 1,
                            (short) 0
                    )
            );
        }

        assertEquals(
                4,
                leaf.getKeyCount()
        );

        assertTrue(
                leaf.isFull()
        );

        assertTrue(
                leaf.isOverflow()
        );
    }

    @Test
    void shouldFindExistingKeyUsingBinarySearch() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(5);

        leaf.addEntry(
                0,
                10,
                new RecordPointer(
                        1,
                        (short) 0
                )
        );

        leaf.addEntry(
                1,
                20,
                new RecordPointer(
                        2,
                        (short) 0
                )
        );

        leaf.addEntry(
                2,
                30,
                new RecordPointer(
                        3,
                        (short) 0
                )
        );

        assertTrue(
                leaf.containsKey(20)
        );

        assertEquals(
                1,
                leaf.indexOfKey(20)
        );

        assertFalse(
                leaf.containsKey(25)
        );
    }

    @Test
    void shouldKeepDuplicatePointersOutOfSameBucket() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        RecordPointer pointer =
                new RecordPointer(
                        15,
                        (short) 3
                );

        leaf.addEntry(
                0,
                100,
                pointer
        );

        leaf.addPointer(
                0,
                pointer
        );

        assertEquals(
                1,
                leaf.getPointers(0).size()
        );
    }

    @Test
    void shouldProtectNodeKeyListFromExternalModification() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        leaf.addEntry(
                0,
                10,
                new RecordPointer(
                        1,
                        (short) 0
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> leaf.getKeys().add(20)
        );
    }

    @Test
    void shouldProtectLeafPointerListFromExternalModification() {

        BPlusTreeLeafNode<Integer> leaf =
                new BPlusTreeLeafNode<>(4);

        leaf.addEntry(
                0,
                10,
                new RecordPointer(
                        1,
                        (short) 0
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> leaf.getPointers(0)
                        .add(
                                new RecordPointer(
                                        2,
                                        (short) 0
                                )
                        )
        );
    }

    @Test
    void shouldReplaceRootWithInternalNode() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        BPlusTreeInternalNode<Integer> newRoot =
                new BPlusTreeInternalNode<>(4);

        BPlusTreeLeafNode<Integer> left =
                new BPlusTreeLeafNode<>(4);

        BPlusTreeLeafNode<Integer> right =
                new BPlusTreeLeafNode<>(4);

        newRoot.addChild(left);

        newRoot.addSeparatorKey(
                0,
                50
        );

        newRoot.addChild(right);

        tree.setRoot(newRoot);

        assertFalse(
                tree.getRoot().isLeaf()
        );

        assertSame(
                newRoot,
                tree.getRootInternal()
        );

        assertEquals(
                2,
                tree.getHeight()
        );

        assertFalse(
                tree.isEmpty()
        );
    }

    @Test
    void shouldRejectRootWithDifferentOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        BPlusTreeLeafNode<Integer> invalidRoot =
                new BPlusTreeLeafNode<>(5);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> tree.setRoot(
                                invalidRoot
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("order")
        );
    }
}