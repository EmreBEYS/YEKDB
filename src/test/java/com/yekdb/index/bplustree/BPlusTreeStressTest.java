package com.yekdb.index.bplustree;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.index.IndexType;
import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 00-29 Phase 15 B+ Tree stress ve edge-case testleri.
 *
 * Bu testler:
 * - binlerce insert,
 * - random insert/delete,
 * - ascending / descending workload,
 * - repeated split / merge,
 * - duplicate pointer bucket,
 * - range consistency,
 * - tree reuse,
 * - farklı order değerleri
 *
 * davranışlarını doğrular.
 */
class BPlusTreeStressTest {

    @Test
    void shouldHandleFiveThousandAscendingInserts() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(8);

        for (int i = 1; i <= 5000; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        assertEquals(
                5000,
                tree.scanAll().size()
        );

        for (int i = 1; i <= 5000; i++) {

            assertTrue(
                    tree.containsKey(i),
                    "Missing key: " + i
            );
        }

        assertTrue(
                tree.getHeight() > 1
        );

        assertTreeValid(tree);
    }

    @Test
    void shouldHandleFiveThousandDescendingInserts() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(8);

        for (int i = 5000; i >= 1; i--) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        assertEquals(
                5000,
                tree.scanAll().size()
        );

        assertPointerRange(
                tree.searchRange(
                        100,
                        200
                ),
                100,
                200
        );

        assertTreeValid(tree);
    }

    @Test
    void shouldHandleDeterministicRandomInsertOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(6);

        List<Integer> values =
                range(
                        1,
                        3000
                );

        Collections.shuffle(
                values,
                new Random(42)
        );

        for (Integer value : values) {

            tree.insert(
                    value,
                    pointer(value)
            );
        }

        for (int i = 1; i <= 3000; i++) {

            assertTrue(
                    tree.containsKey(i)
            );
        }

        assertEquals(
                3000,
                tree.scanAll().size()
        );

        assertTreeValid(tree);
    }

    @Test
    void shouldHandleRandomInsertAndDeleteWorkload() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(5);

        List<Integer> values =
                range(
                        1,
                        2000
                );

        Collections.shuffle(
                values,
                new Random(100)
        );

        for (Integer value : values) {

            tree.insert(
                    value,
                    pointer(value)
            );
        }

        List<Integer> deleteOrder =
                new ArrayList<>(values);

        Collections.shuffle(
                deleteOrder,
                new Random(200)
        );

        Set<Integer> deleted =
                new HashSet<>();

        for (int i = 0; i < 1000; i++) {

            int key =
                    deleteOrder.get(i);

            assertTrue(
                    tree.delete(key)
            );

            deleted.add(key);
        }

        for (int i = 1; i <= 2000; i++) {

            assertEquals(
                    !deleted.contains(i),
                    tree.containsKey(i),
                    "Unexpected presence for key: "
                            + i
            );
        }

        assertEquals(
                1000,
                tree.scanAll().size()
        );

        assertTreeValid(tree);
    }

    @Test
    void shouldCollapseCompletelyAfterDeletingAllKeys() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 1; i <= 1000; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        assertTrue(
                tree.getHeight() >= 3
        );

        for (int i = 1; i <= 1000; i++) {

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
    void shouldCollapseCompletelyWithReverseDeleteOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 1; i <= 1000; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        for (int i = 1000; i >= 1; i--) {

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

        assertTreeValid(tree);
    }

    @Test
    void shouldSupportRepeatedGrowAndShrinkCycles() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int cycle = 0;
             cycle < 10;
             cycle++) {

            int offset =
                    cycle * 1000;

            for (int i = 1;
                 i <= 500;
                 i++) {

                tree.insert(
                        offset + i,
                        pointer(
                                offset + i
                        )
                );
            }

            assertEquals(
                    500,
                    tree.scanAll().size()
            );

            assertTrue(
                    tree.getHeight() > 1
            );

            for (int i = 1;
                 i <= 500;
                 i++) {

                assertTrue(
                        tree.delete(
                                offset + i
                        )
                );
            }

            assertTrue(
                    tree.isEmpty()
            );

            assertEquals(
                    1,
                    tree.getHeight()
            );
        }
    }

    @Test
    void shouldHandleLargeDuplicatePointerBucket() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(8);

        for (int i = 1; i <= 1000; i++) {

            tree.insert(
                    50,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        assertEquals(
                1,
                tree.getRoot()
                        .getKeyCount()
        );

        assertEquals(
                1000,
                tree.search(50)
                        .size()
        );

        assertEquals(
                1000,
                tree.scanAll()
                        .size()
        );
    }

    @Test
    void shouldDeleteIndividualPointersFromLargeBucket() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(8);

        for (int i = 1; i <= 500; i++) {

            tree.insert(
                    10,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        for (int i = 1; i <= 499; i++) {

            assertTrue(
                    tree.delete(
                            10,
                            new RecordPointer(
                                    i,
                                    0
                            )
                    )
            );
        }

        assertTrue(
                tree.containsKey(10)
        );

        assertEquals(
                List.of(
                        new RecordPointer(
                                500,
                                0
                        )
                ),
                tree.search(10)
        );

        assertTrue(
                tree.delete(
                        10,
                        new RecordPointer(
                                500,
                                0
                        )
                )
        );

        assertFalse(
                tree.containsKey(10)
        );
    }

    @Test
    void shouldMaintainRangeConsistencyAfterHeavyDeletes() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(6);

        for (int i = 1; i <= 2000; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        /*
         * Çift sayıları siliyoruz.
         */
        for (int i = 2; i <= 2000; i += 2) {

            tree.delete(i);
        }

        List<RecordPointer> result =
                tree.searchRange(
                        100,
                        200
                );

        assertEquals(
                50,
                result.size()
        );

        for (RecordPointer pointer : result) {

            assertTrue(
                    pointer.getPageId() % 2 != 0
            );

            assertTrue(
                    pointer.getPageId() >= 101
            );

            assertTrue(
                    pointer.getPageId() <= 199
            );
        }

        assertTreeValid(tree);
    }

    @Test
    void shouldWorkWithMinimumSupportedOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(
                        BPlusTree.MIN_ORDER
                );

        for (int i = 1; i <= 1000; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        for (int i = 1; i <= 500; i++) {

            tree.delete(i);
        }

        for (int i = 501; i <= 1000; i++) {

            assertTrue(
                    tree.containsKey(i)
            );
        }

        assertTreeValid(tree);
    }

    @Test
    void shouldWorkWithLargeOrder() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(128);

        for (int i = 1; i <= 10000; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        assertEquals(
                10000,
                tree.scanAll().size()
        );

        assertPointerRange(
                tree.searchRange(
                        9000,
                        9100
                ),
                9000,
                9100
        );

        assertTreeValid(tree);
    }

    @Test
    void shouldMaintainIndexWrapperUnderStress() {

        Index<Integer> index =
                new Index<>(
                        new IndexMetadata(
                                1L,
                                "idx_stress",
                                "stress_db",
                                "stress_table",
                                "value",
                                IndexType.NON_UNIQUE
                        )
                );

        for (int i = 1; i <= 3000; i++) {

            index.insert(
                    i,
                    pointer(i)
            );
        }

        assertEquals(
                3000,
                index.size()
        );

        assertEquals(
                3000,
                index.pointerCount()
        );

        for (int i = 1; i <= 1500; i++) {

            assertTrue(
                    index.remove(i)
            );
        }

        assertEquals(
                1500,
                index.size()
        );

        assertEquals(
                1500,
                index.pointerCount()
        );

        assertPointerRange(
                index.searchRange(
                        1501,
                        1600
                ),
                1501,
                1600
        );
    }

    @Test
    void shouldMaintainNonUniqueIndexBucketsUnderStress() {

        Index<Integer> index =
                new Index<>(
                        new IndexMetadata(
                                1L,
                                "idx_non_unique_stress",
                                "stress_db",
                                "stress_table",
                                "group_id",
                                IndexType.NON_UNIQUE
                        )
                );

        /*
         * 100 farklı key.
         * Her key için 20 pointer.
         */
        for (int key = 1;
             key <= 100;
             key++) {

            for (int i = 0;
                 i < 20;
                 i++) {

                index.insert(
                        key,
                        new RecordPointer(
                                key * 1000 + i,
                                0
                        )
                );
            }
        }

        assertEquals(
                100,
                index.size()
        );

        assertEquals(
                2000,
                index.pointerCount()
        );

        for (int key = 1;
             key <= 100;
             key++) {

            assertEquals(
                    20,
                    index.search(key)
                            .size()
            );
        }
    }

    @Test
    void shouldRemainReusableAfterCompleteDeletion() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 1; i <= 500; i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        for (int i = 1; i <= 500; i++) {

            tree.delete(i);
        }

        assertTrue(
                tree.isEmpty()
        );

        /*
         * Tamamen boşalmış tree yeniden kullanılabilmeli.
         */
        for (int i = 1001;
             i <= 1500;
             i++) {

            tree.insert(
                    i,
                    pointer(i)
            );
        }

        assertEquals(
                500,
                tree.scanAll().size()
        );

        assertFalse(
                tree.containsKey(1)
        );

        assertTrue(
                tree.containsKey(1001)
        );

        assertTrue(
                tree.containsKey(1500)
        );

        assertTreeValid(tree);
    }

    /**
     * Tree boyunca overflow, structural invariant ve
     * separator kurallarını recursive doğrular.
     */
    private void assertTreeValid(
            BPlusTree<Integer> tree
    ) {

        assertNodeValid(
                tree,
                tree.getRoot(),
                true
        );
    }

    private void assertNodeValid(
            BPlusTree<Integer> tree,
            BPlusTreeNode<Integer> node,
            boolean root
    ) {

        assertFalse(
                node.isOverflow(),
                "Overflow node detected: "
                        + node
        );

        /*
         * Key'ler strictly ascending olmalıdır.
         */
        for (int i = 1;
             i < node.getKeyCount();
             i++) {

            assertTrue(
                    node.getKey(i - 1)
                            .compareTo(
                                    node.getKey(i)
                            ) < 0,
                    "Node keys are not strictly sorted: "
                            + node
            );
        }

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

            if (!root) {

                assertTrue(
                        leaf.getKeyCount()
                                >= tree.getMinLeafKeys(),
                        "Leaf underflow detected: "
                                + leaf
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
                    "Internal underflow detected: "
                            + internal
            );
        }

        /*
         * Separator her zaman sağ child subtree'sinin
         * minimum key'i olmalıdır.
         */
        for (int i = 0;
             i < internal.getKeyCount();
             i++) {

            assertEquals(
                    minimumKey(
                            internal.getChild(
                                    i + 1
                            )
                    ),
                    internal.getKey(i),
                    "Invalid separator at index "
                            + i
            );
        }

        for (BPlusTreeNode<Integer> child
                : internal.getChildren()) {

            assertNodeValid(
                    tree,
                    child,
                    false
            );
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

        BPlusTreeLeafNode<Integer> leaf =
                castLeaf(current);

        assertFalse(
                leaf.isEmpty()
        );

        return leaf.getKey(0);
    }

    private void assertPointerRange(
            List<RecordPointer> pointers,
            int from,
            int to
    ) {

        assertEquals(
                to - from + 1,
                pointers.size()
        );

        for (int i = 0;
             i < pointers.size();
             i++) {

            assertEquals(
                    from + i,
                    pointers.get(i)
                            .getPageId()
            );
        }
    }

    private List<Integer> range(
            int from,
            int to
    ) {

        List<Integer> result =
                new ArrayList<>(
                        to - from + 1
                );

        for (int i = from;
             i <= to;
             i++) {

            result.add(i);
        }

        return result;
    }

    private RecordPointer pointer(
            int id
    ) {

        return new RecordPointer(
                id,
                0
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

    @SuppressWarnings("unchecked")
    private BPlusTreeInternalNode<Integer> castInternal(
            BPlusTreeNode<Integer> node
    ) {

        assertFalse(
                node.isLeaf()
        );

        return (BPlusTreeInternalNode<Integer>) node;
    }
}