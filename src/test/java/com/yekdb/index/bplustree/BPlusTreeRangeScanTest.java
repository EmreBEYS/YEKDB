package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 6 range scan testleri.
 *
 * Bu testler:
 * - inclusive range,
 * - exclusive range,
 * - greater-than,
 * - less-than,
 * - leaf sınırlarını aşan traversal,
 * - duplicate bucket'lar,
 * - full scan
 *
 * davranışlarını doğrular.
 */
class BPlusTreeRangeScanTest {

    @Test
    void shouldSearchInclusiveRangeInsideSingleLeaf() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        5,
                        7
                );

        assertPointerIds(
                result,
                5,
                6,
                7
        );
    }

    @Test
    void shouldSearchInclusiveRangeAcrossMultipleLeaves() {

        BPlusTree<Integer> tree =
                createTree(40);

        List<RecordPointer> result =
                tree.searchRange(
                        8,
                        17
                );

        assertPointerIds(
                result,
                8,
                9,
                10,
                11,
                12,
                13,
                14,
                15,
                16,
                17
        );
    }

    @Test
    void shouldRespectExclusiveLowerBound() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        5,
                        false,
                        10,
                        true
                );

        assertPointerIds(
                result,
                6,
                7,
                8,
                9,
                10
        );
    }

    @Test
    void shouldRespectExclusiveUpperBound() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        5,
                        true,
                        10,
                        false
                );

        assertPointerIds(
                result,
                5,
                6,
                7,
                8,
                9
        );
    }

    @Test
    void shouldRespectBothExclusiveBounds() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        5,
                        false,
                        10,
                        false
                );

        assertPointerIds(
                result,
                6,
                7,
                8,
                9
        );
    }

    @Test
    void shouldReturnSingleKeyForEqualInclusiveBounds() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        10,
                        10
                );

        assertPointerIds(
                result,
                10
        );
    }

    @Test
    void shouldReturnEmptyRangeForEqualExclusiveBounds() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        10,
                        false,
                        10,
                        false
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldReturnEmptyRangeWhenNoKeysMatch() {

        BPlusTree<Integer> tree =
                createTree(20);

        List<RecordPointer> result =
                tree.searchRange(
                        100,
                        200
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldRejectReversedRange() {

        BPlusTree<Integer> tree =
                createTree(20);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> tree.searchRange(
                                20,
                                10
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("range")
        );
    }

    @Test
    void shouldSearchGreaterThan() {

        BPlusTree<Integer> tree =
                createTree(10);

        List<RecordPointer> result =
                tree.searchGreaterThan(7);

        assertPointerIds(
                result,
                8,
                9,
                10
        );
    }

    @Test
    void shouldSearchGreaterThanOrEqual() {

        BPlusTree<Integer> tree =
                createTree(10);

        List<RecordPointer> result =
                tree.searchGreaterThanOrEqual(7);

        assertPointerIds(
                result,
                7,
                8,
                9,
                10
        );
    }

    @Test
    void shouldSearchLessThan() {

        BPlusTree<Integer> tree =
                createTree(10);

        List<RecordPointer> result =
                tree.searchLessThan(4);

        assertPointerIds(
                result,
                1,
                2,
                3
        );
    }

    @Test
    void shouldSearchLessThanOrEqual() {

        BPlusTree<Integer> tree =
                createTree(10);

        List<RecordPointer> result =
                tree.searchLessThanOrEqual(4);

        assertPointerIds(
                result,
                1,
                2,
                3,
                4
        );
    }

    @Test
    void shouldReturnEmptyGreaterThanResultAboveMaximum() {

        BPlusTree<Integer> tree =
                createTree(10);

        assertTrue(
                tree.searchGreaterThan(10)
                        .isEmpty()
        );
    }

    @Test
    void shouldReturnEmptyLessThanResultBelowMinimum() {

        BPlusTree<Integer> tree =
                createTree(10);

        assertTrue(
                tree.searchLessThan(1)
                        .isEmpty()
        );
    }

    @Test
    void shouldScanAllPointersInAscendingKeyOrder() {

        BPlusTree<Integer> tree =
                createTree(50);

        List<RecordPointer> result =
                tree.scanAll();

        assertEquals(
                50,
                result.size()
        );

        for (int i = 0; i < 50; i++) {

            assertEquals(
                    i + 1,
                    result.get(i).getPageId()
            );
        }
    }

    @Test
    void shouldScanAllOnEmptyTree() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        List<RecordPointer> result =
                tree.scanAll();

        assertNotNull(result);

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldIncludeEveryPointerFromDuplicateBucket() {

        BPlusTree<Integer> tree =
                createTree(10);

        RecordPointer extra1 =
                new RecordPointer(
                        100,
                        1
                );

        RecordPointer extra2 =
                new RecordPointer(
                        101,
                        2
                );

        tree.insert(
                5,
                extra1
        );

        tree.insert(
                5,
                extra2
        );

        List<RecordPointer> result =
                tree.searchRange(
                        5,
                        5
                );

        assertEquals(
                3,
                result.size()
        );

        assertEquals(
                new RecordPointer(
                        5,
                        0
                ),
                result.get(0)
        );

        assertEquals(
                extra1,
                result.get(1)
        );

        assertEquals(
                extra2,
                result.get(2)
        );
    }

    @Test
    void shouldPreserveDuplicateBucketOrderDuringFullScan() {

        BPlusTree<Integer> tree =
                createTree(6);

        RecordPointer second =
                new RecordPointer(
                        200,
                        1
                );

        tree.insert(
                3,
                second
        );

        List<RecordPointer> result =
                tree.scanAll();

        int firstIndex =
                result.indexOf(
                        new RecordPointer(
                                3,
                                0
                        )
                );

        int secondIndex =
                result.indexOf(second);

        assertTrue(
                firstIndex >= 0
        );

        assertEquals(
                firstIndex + 1,
                secondIndex
        );
    }

    @Test
    void shouldWorkAcrossDeepTree() {

        BPlusTree<Integer> tree =
                createTree(250);

        assertTrue(
                tree.getHeight() >= 3
        );

        List<RecordPointer> result =
                tree.searchRange(
                        73,
                        129
                );

        assertEquals(
                57,
                result.size()
        );

        assertEquals(
                73,
                result.get(0).getPageId()
        );

        assertEquals(
                129,
                result.get(
                        result.size() - 1
                ).getPageId()
        );
    }

    @Test
    void shouldWorkWithMissingBoundaryKeys() {

        BPlusTree<Integer> tree =
                new BPlusTree<>(4);

        for (int i = 10; i <= 100; i += 10) {

            tree.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        List<RecordPointer> result =
                tree.searchRange(
                        25,
                        65
                );

        assertPointerIds(
                result,
                30,
                40,
                50,
                60
        );
    }

    @Test
    void shouldSupportStringRangeScan() {

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

        tree.insert(
                "Samsun",
                new RecordPointer(5, 0)
        );

        List<RecordPointer> result =
                tree.searchRange(
                        "Bursa",
                        "Malatya"
                );

        assertPointerIds(
                result,
                2,
                3,
                4
        );
    }

    @Test
    void shouldRejectNullRangeStart() {

        BPlusTree<Integer> tree =
                createTree(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.searchRange(
                        null,
                        5
                )
        );
    }

    @Test
    void shouldRejectNullRangeEnd() {

        BPlusTree<Integer> tree =
                createTree(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.searchRange(
                        1,
                        null
                )
        );
    }

    @Test
    void shouldRejectNullComparisonKey() {

        BPlusTree<Integer> tree =
                createTree(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.searchGreaterThan(
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.searchLessThanOrEqual(
                        null
                )
        );
    }

    /**
     * 1..max arasındaki key'lerle test tree'si oluşturur.
     */
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

    /**
     * Pointer pageId değerlerinin beklenen sırada olduğunu doğrular.
     */
    private void assertPointerIds(
            List<RecordPointer> pointers,
            int... expectedIds
    ) {

        assertEquals(
                expectedIds.length,
                pointers.size()
        );

        List<Integer> actual =
                new ArrayList<>();

        for (RecordPointer pointer : pointers) {
            actual.add(
                    pointer.getPageId()
            );
        }

        List<Integer> expected =
                new ArrayList<>();

        for (int id : expectedIds) {
            expected.add(id);
        }

        assertEquals(
                expected,
                actual
        );
    }
}