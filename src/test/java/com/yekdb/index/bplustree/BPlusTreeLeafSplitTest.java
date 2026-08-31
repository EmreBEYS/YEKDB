package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 3 leaf split ve root split testleri.
 *
 * Bu test sınıfı Phase 4 davranışıyla uyumlu olacak şekilde güncellenmiştir.
 */
class BPlusTreeLeafSplitTest {

    @Test
    void shouldSplitRootLeafWhenOverflowOccurs() {
        BPlusTree<Integer> tree = new BPlusTree<>(4);

        tree.insert(10, new RecordPointer(1, 0));
        tree.insert(20, new RecordPointer(2, 0));
        tree.insert(30, new RecordPointer(3, 0));

        assertEquals(1, tree.getHeight());
        assertTrue(tree.getRoot().isLeaf());

        tree.insert(40, new RecordPointer(4, 0));

        assertEquals(2, tree.getHeight());
        assertFalse(tree.getRoot().isLeaf());
    }

    @Test
    void shouldCreateInternalRootAfterFirstSplit() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(1, root.getKeyCount());
        assertEquals(2, root.getChildCount());
        assertTrue(root.isStructurallyValid());
    }

    @Test
    void shouldPromoteFirstRightLeafKeyAsSeparator() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(30, root.getKey(0));
    }

    @Test
    void shouldDistributeKeysBetweenLeafNodes() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left = castLeaf(root.getChild(0));
        BPlusTreeLeafNode<Integer> right = castLeaf(root.getChild(1));

        assertEquals(List.of(10, 20), left.getKeys());
        assertEquals(List.of(30, 40), right.getKeys());
    }

    @Test
    void shouldMaintainLeafValueBucketsAfterSplit() {
        BPlusTree<Integer> tree = createSplitTree();

        assertEquals(List.of(new RecordPointer(1, 0)), tree.search(10));
        assertEquals(List.of(new RecordPointer(2, 0)), tree.search(20));
        assertEquals(List.of(new RecordPointer(3, 0)), tree.search(30));
        assertEquals(List.of(new RecordPointer(4, 0)), tree.search(40));
    }

    @Test
    void shouldLinkSplitLeafNodesForward() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left = castLeaf(root.getChild(0));
        BPlusTreeLeafNode<Integer> right = castLeaf(root.getChild(1));

        assertSame(right, left.getNextLeaf());
    }

    @Test
    void shouldLinkSplitLeafNodesBackward() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left = castLeaf(root.getChild(0));
        BPlusTreeLeafNode<Integer> right = castLeaf(root.getChild(1));

        assertSame(left, right.getPreviousLeaf());
    }

    @Test
    void shouldKeepBoundaryLeafLinksNull() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        BPlusTreeLeafNode<Integer> left = castLeaf(root.getChild(0));
        BPlusTreeLeafNode<Integer> right = castLeaf(root.getChild(1));

        assertNull(left.getPreviousLeaf());
        assertNull(right.getNextLeaf());
    }

    @Test
    void shouldFindCorrectLeafAfterSplit() {
        BPlusTree<Integer> tree = createSplitTree();

        BPlusTreeLeafNode<Integer> left = tree.findLeafForKey(20);
        BPlusTreeLeafNode<Integer> right = tree.findLeafForKey(30);

        assertEquals(List.of(10, 20), left.getKeys());
        assertEquals(List.of(30, 40), right.getKeys());
    }

    @Test
    void shouldRouteSeparatorKeyToRightLeaf() {
        BPlusTree<Integer> tree = createSplitTree();
        BPlusTreeLeafNode<Integer> leaf = tree.findLeafForKey(30);

        assertEquals(List.of(30, 40), leaf.getKeys());
    }

    @Test
    void shouldSearchMissingKeyAfterSplit() {
        BPlusTree<Integer> tree = createSplitTree();

        assertTrue(tree.search(25).isEmpty());
        assertFalse(tree.containsKey(25));
    }

    @Test
    void shouldAllowInsertIntoNonFullLeafAfterSplit() {
        BPlusTree<Integer> tree = createSplitTree();

        tree.insert(35, new RecordPointer(35, 0));

        BPlusTreeLeafNode<Integer> right = tree.findLeafForKey(35);

        assertEquals(List.of(30, 35, 40), right.getKeys());
        assertTrue(tree.containsKey(35));
    }

    @Test
    void shouldAllowDuplicateKeyInsertAfterSplit() {
        BPlusTree<Integer> tree = createSplitTree();
        RecordPointer secondPointer = new RecordPointer(99, 0);

        boolean newKey = tree.insert(30, secondPointer);

        assertFalse(newKey);
        assertEquals(2, tree.search(30).size());
        assertTrue(tree.search(30).contains(secondPointer));
    }

    @Test
    void shouldSplitChildLeafUnderExistingRootInPhaseFour() {
        BPlusTree<Integer> tree = createSplitTree();

        /*
         * Sağ leaf başlangıçta [30, 40].
         * 35 ile leaf tam kapasiteye ulaşır, 50 ise Phase 4 kapsamında
         * child leaf split işlemini tetikler.
         */
        tree.insert(35, new RecordPointer(35, 0));
        tree.insert(50, new RecordPointer(50, 0));

        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(List.of(30, 40), root.getKeys());
        assertEquals(3, root.getChildCount());
        assertEquals(List.of(10, 20), castLeaf(root.getChild(0)).getKeys());
        assertEquals(List.of(30, 35), castLeaf(root.getChild(1)).getKeys());
        assertEquals(List.of(40, 50), castLeaf(root.getChild(2)).getKeys());
        assertTrue(root.isStructurallyValid());
    }

    @Test
    void shouldSplitKeysCorrectlyForOddOverflowCount() {
        BPlusTree<Integer> tree = new BPlusTree<>(5);

        for (int i = 1; i <= 5; i++) {
            tree.insert(i * 10, new RecordPointer(i, 0));
        }

        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();
        BPlusTreeLeafNode<Integer> left = castLeaf(root.getChild(0));
        BPlusTreeLeafNode<Integer> right = castLeaf(root.getChild(1));

        assertEquals(List.of(10, 20, 30), left.getKeys());
        assertEquals(List.of(40, 50), right.getKeys());
        assertEquals(40, root.getKey(0));
        assertTrue(left.getKeyCount() >= tree.getMinLeafKeys());
        assertTrue(right.getKeyCount() >= tree.getMinLeafKeys());
    }

    @Test
    void shouldSplitCorrectlyRegardlessOfInsertionOrder() {
        BPlusTree<Integer> tree = new BPlusTree<>(4);

        tree.insert(40, new RecordPointer(4, 0));
        tree.insert(10, new RecordPointer(1, 0));
        tree.insert(30, new RecordPointer(3, 0));
        tree.insert(20, new RecordPointer(2, 0));

        BPlusTreeInternalNode<Integer> root = tree.getRootInternal();

        assertEquals(30, root.getKey(0));
        assertEquals(List.of(10, 20), castLeaf(root.getChild(0)).getKeys());
        assertEquals(List.of(30, 40), castLeaf(root.getChild(1)).getKeys());
    }

    private BPlusTree<Integer> createSplitTree() {
        BPlusTree<Integer> tree = new BPlusTree<>(4);

        tree.insert(10, new RecordPointer(1, 0));
        tree.insert(20, new RecordPointer(2, 0));
        tree.insert(30, new RecordPointer(3, 0));
        tree.insert(40, new RecordPointer(4, 0));

        return tree;
    }

    @SuppressWarnings("unchecked")
    private BPlusTreeLeafNode<Integer> castLeaf(BPlusTreeNode<Integer> node) {
        assertTrue(node.isLeaf());
        return (BPlusTreeLeafNode<Integer>) node;
    }
}
