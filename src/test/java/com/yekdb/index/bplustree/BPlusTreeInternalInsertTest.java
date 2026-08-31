package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 4 internal parent insert ve leaf split propagation testleri.
 */
class BPlusTreeInternalInsertTest {

    @Test
    void shouldSplitRightLeafUnderExistingRoot() {
        BPlusTree<Integer> tree = createInitialSplitTree();

        tree.insert(35, new RecordPointer(35, 0));
        tree.insert(50, new RecordPointer(50, 0));

        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(2, root.getKeyCount());
        assertEquals(3, root.getChildCount());
        assertEquals(List.of(30, 40), root.getKeys());
        assertTrue(root.isStructurallyValid());
    }

    @Test
    void shouldCreateThreeLeafChildrenAfterRightSplit() {
        BPlusTree<Integer> tree = createThreeLeafTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(List.of(10, 20), leaf(root, 0).getKeys());
        assertEquals(List.of(30, 35), leaf(root, 1).getKeys());
        assertEquals(List.of(40, 50), leaf(root, 2).getKeys());
    }

    @Test
    void shouldMaintainSeparatorSemanticsAfterSplit() {
        BPlusTree<Integer> tree = createThreeLeafTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(leaf(root, 1).getKey(0), root.getKey(0));
        assertEquals(leaf(root, 2).getKey(0), root.getKey(1));
    }

    @Test
    void shouldSearchEveryKeyAfterParentInsertion() {
        BPlusTree<Integer> tree = createThreeLeafTree();

        assertTrue(tree.containsKey(10));
        assertTrue(tree.containsKey(20));
        assertTrue(tree.containsKey(30));
        assertTrue(tree.containsKey(35));
        assertTrue(tree.containsKey(40));
        assertTrue(tree.containsKey(50));
        assertTrue(tree.search(999).isEmpty());
    }

    @Test
    void shouldRouteKeysToCorrectLeafAfterParentInsertion() {
        BPlusTree<Integer> tree = createThreeLeafTree();

        assertEquals(List.of(10, 20), tree.findLeafForKey(25).getKeys());
        assertEquals(List.of(30, 35), tree.findLeafForKey(30).getKeys());
        assertEquals(List.of(40, 50), tree.findLeafForKey(40).getKeys());
    }

    @Test
    void shouldMaintainForwardLeafChainAfterParentInsertion() {
        BPlusTree<Integer> tree = createThreeLeafTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        BPlusTreeLeafNode<Integer> first = leaf(root, 0);
        BPlusTreeLeafNode<Integer> second = leaf(root, 1);
        BPlusTreeLeafNode<Integer> third = leaf(root, 2);

        assertSame(second, first.getNextLeaf());
        assertSame(third, second.getNextLeaf());
        assertNull(third.getNextLeaf());
    }

    @Test
    void shouldMaintainBackwardLeafChainAfterParentInsertion() {
        BPlusTree<Integer> tree = createThreeLeafTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        BPlusTreeLeafNode<Integer> first = leaf(root, 0);
        BPlusTreeLeafNode<Integer> second = leaf(root, 1);
        BPlusTreeLeafNode<Integer> third = leaf(root, 2);

        assertNull(first.getPreviousLeaf());
        assertSame(first, second.getPreviousLeaf());
        assertSame(second, third.getPreviousLeaf());
    }

    @Test
    void shouldSplitLeftLeafUnderExistingRoot() {
        BPlusTree<Integer> tree = createInitialSplitTree();

        tree.insert(15, new RecordPointer(15, 0));
        tree.insert(5, new RecordPointer(5, 0));

        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(3, root.getChildCount());
        assertEquals(2, root.getKeyCount());
        assertEquals(List.of(15, 30), root.getKeys());
        assertEquals(List.of(5, 10), leaf(root, 0).getKeys());
        assertEquals(List.of(15, 20), leaf(root, 1).getKeys());
        assertEquals(List.of(30, 40), leaf(root, 2).getKeys());
    }

    @Test
    void shouldKeepDuplicateKeyInsideExistingBucketWithoutSplit() {
        BPlusTree<Integer> tree = createThreeLeafTree();
        int childCountBefore = tree.getRootInternal().getChildCount();
        RecordPointer extra = new RecordPointer(999, 1);

        boolean newKey = tree.insert(40, extra);

        assertFalse(newKey);
        assertEquals(childCountBefore, tree.getRootInternal().getChildCount());
        assertEquals(2, tree.search(40).size());
        assertTrue(tree.search(40).contains(extra));
    }

    @Test
    void shouldKeepTreeHeightAtTwoDuringPhaseFour() {
        BPlusTree<Integer> tree = createThreeLeafTree();

        assertEquals(2, tree.getHeight());
        assertFalse(tree.getRoot().isLeaf());
    }

    @Test
    void shouldFillInternalRootToMaximumCapacity() {
        BPlusTree<Integer> tree = createThreeLeafTree();

        tree.insert(15, new RecordPointer(15, 0));
        tree.insert(5, new RecordPointer(5, 0));

        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(3, root.getKeyCount());
        assertEquals(4, root.getChildCount());
        assertTrue(root.isFull());
        assertFalse(root.isOverflow());
        assertTrue(root.isStructurallyValid());
    }

    @Test
    void shouldSplitFullInternalRootAndIncreaseHeight() {
        BPlusTree<Integer> tree = createFullInternalRootTree();
        BPlusTreeInternalNode<Integer> oldRoot = tree.getRootInternal();

        assertTrue(oldRoot.isFull());
        assertEquals(2, tree.getHeight());

        /*
         * En sağ leaf [40, 50] durumundadır. 45 ile leaf tam kapasiteye
         * ulaşır, 60 ise leaf split + internal root split işlemini tetikler.
         */
        tree.insert(45, new RecordPointer(45, 0));
        tree.insert(60, new RecordPointer(60, 0));

        assertEquals(3, tree.getHeight());
        assertFalse(tree.getRoot().isLeaf());
        assertNotSame(oldRoot, tree.getRoot());

        BPlusTreeInternalNode<Integer> newRoot = tree.getRootInternal();

        assertEquals(List.of(40), newRoot.getKeys());
        assertEquals(2, newRoot.getChildCount());
        assertTrue(newRoot.isStructurallyValid());
        assertTrue(tree.containsKey(60));
        assertFalse(newRoot.isOverflow());
    }

    @Test
    void shouldPreserveAllLeafStructuralInvariants() {
        BPlusTree<Integer> tree = createFullInternalRootTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        for (BPlusTreeNode<Integer> child : root.getChildren()) {
            BPlusTreeLeafNode<Integer> leaf = castLeaf(child);

            assertTrue(leaf.isStructurallyValid());
            assertEquals(leaf.getKeyCount(), leaf.getValueCount());
            assertFalse(leaf.isOverflow());
        }

        assertTrue(root.isStructurallyValid());
    }

    private BPlusTree<Integer> createInitialSplitTree() {
        BPlusTree<Integer> tree = new BPlusTree<>(4);

        tree.insert(10, new RecordPointer(10, 0));
        tree.insert(20, new RecordPointer(20, 0));
        tree.insert(30, new RecordPointer(30, 0));
        tree.insert(40, new RecordPointer(40, 0));

        return tree;
    }

    private BPlusTree<Integer> createThreeLeafTree() {
        BPlusTree<Integer> tree = createInitialSplitTree();

        tree.insert(35, new RecordPointer(35, 0));
        tree.insert(50, new RecordPointer(50, 0));

        return tree;
    }

    private BPlusTree<Integer> createFullInternalRootTree() {
        BPlusTree<Integer> tree = createThreeLeafTree();

        tree.insert(15, new RecordPointer(15, 0));
        tree.insert(5, new RecordPointer(5, 0));

        return tree;
    }

    private BPlusTreeLeafNode<Integer> leaf(BPlusTreeInternalNode<Integer> parent, int index) {
        return castLeaf(parent.getChild(index));
    }

    @SuppressWarnings("unchecked")
    private BPlusTreeLeafNode<Integer> castLeaf(BPlusTreeNode<Integer> node) {
        assertTrue(node.isLeaf());
        return (BPlusTreeLeafNode<Integer>) node;
    }
}
