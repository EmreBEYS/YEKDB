package com.yekdb.index;

import com.yekdb.index.exception.DuplicateIndexKeyException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 00-29 Phase 10 Index -> B+ Tree entegrasyon testleri.
 *
 * Bu testler:
 * - Index API uyumluluğunu,
 * - B+ Tree backend kullanımını,
 * - sorted traversal davranışını,
 * - range sorgularını,
 * - duplicate politikalarını,
 * - delete/update işlemlerini
 *
 * doğrular.
 */
class IndexBPlusTreeIntegrationTest {

    @Test
    void shouldCreateIndexBackedByBPlusTree() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        assertEquals(
                1,
                index.getTreeHeight()
        );

        assertEquals(
                32,
                index.getTreeOrder()
        );

        assertTrue(
                index.isEmpty()
        );
    }

    @Test
    void shouldInsertAndSearchThroughIndexApi() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        RecordPointer pointer =
                new RecordPointer(
                        10,
                        1
                );

        index.insert(
                100,
                pointer
        );

        assertEquals(
                List.of(pointer),
                index.search(100)
        );

        assertTrue(
                index.containsKey(100)
        );
    }

    @Test
    void shouldAllowDuplicateKeysInNonUniqueIndex() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        RecordPointer first =
                new RecordPointer(
                        1,
                        0
                );

        RecordPointer second =
                new RecordPointer(
                        2,
                        0
                );

        index.insert(
                10,
                first
        );

        index.insert(
                10,
                second
        );

        assertEquals(
                List.of(
                        first,
                        second
                ),
                index.search(10)
        );

        assertEquals(
                1,
                index.size()
        );

        assertEquals(
                2,
                index.pointerCount()
        );
    }

    @Test
    void shouldRejectDuplicateKeyInUniqueIndex() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.UNIQUE
                        )
                );

        index.insert(
                10,
                new RecordPointer(
                        1,
                        0
                )
        );

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> index.insert(
                        10,
                        new RecordPointer(
                                2,
                                0
                        )
                )
        );
    }

    @Test
    void shouldRejectDuplicateKeyInPrimaryIndex() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.PRIMARY
                        )
                );

        index.insert(
                10,
                new RecordPointer(
                        1,
                        0
                )
        );

        assertThrows(
                DuplicateIndexKeyException.class,
                () -> index.insert(
                        10,
                        new RecordPointer(
                                2,
                                0
                        )
                )
        );
    }

    @Test
    void shouldGrowTreeAfterManyUniqueKeys() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        for (int i = 1; i <= 100; i++) {

            index.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        assertTrue(
                index.getTreeHeight() >= 2
        );

        assertEquals(
                100,
                index.size()
        );

        assertEquals(
                100,
                index.pointerCount()
        );
    }

    @Test
    void shouldReturnEntriesInSortedKeyOrder() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        index.insert(
                40,
                new RecordPointer(4, 0)
        );

        index.insert(
                10,
                new RecordPointer(1, 0)
        );

        index.insert(
                30,
                new RecordPointer(3, 0)
        );

        index.insert(
                20,
                new RecordPointer(2, 0)
        );

        List<Integer> keys =
                new ArrayList<>(
                        index.getAllEntries()
                                .keySet()
                );

        assertEquals(
                List.of(
                        10,
                        20,
                        30,
                        40
                ),
                keys
        );
    }

    @Test
    void shouldReturnSortedIndexEntryList() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        index.insert(
                30,
                new RecordPointer(3, 0)
        );

        index.insert(
                10,
                new RecordPointer(1, 0)
        );

        index.insert(
                20,
                new RecordPointer(2, 0)
        );

        List<IndexEntry<Integer>> entries =
                index.getEntryList();

        assertEquals(
                10,
                entries.get(0)
                        .getKey()
        );

        assertEquals(
                20,
                entries.get(1)
                        .getKey()
        );

        assertEquals(
                30,
                entries.get(2)
                        .getKey()
        );
    }

    @Test
    void shouldExecuteRangeSearchThroughIndexApi() {

        Index<Integer> index =
                createIndexWithKeys(50);

        List<RecordPointer> result =
                index.searchRange(
                        20,
                        25
                );

        assertPointerPageIds(
                result,
                20,
                21,
                22,
                23,
                24,
                25
        );
    }

    @Test
    void shouldExecuteGreaterThanSearchThroughIndexApi() {

        Index<Integer> index =
                createIndexWithKeys(10);

        List<RecordPointer> result =
                index.searchGreaterThan(7);

        assertPointerPageIds(
                result,
                8,
                9,
                10
        );
    }

    @Test
    void shouldExecuteLessThanOrEqualSearchThroughIndexApi() {

        Index<Integer> index =
                createIndexWithKeys(10);

        List<RecordPointer> result =
                index.searchLessThanOrEqual(4);

        assertPointerPageIds(
                result,
                1,
                2,
                3,
                4
        );
    }

    @Test
    void shouldRemoveKeyThroughIndexApi() {

        Index<Integer> index =
                createIndexWithKeys(20);

        assertTrue(
                index.remove(10)
        );

        assertFalse(
                index.containsKey(10)
        );

        assertEquals(
                19,
                index.size()
        );
    }

    @Test
    void shouldRemoveSinglePointerFromDuplicateBucket() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        RecordPointer first =
                new RecordPointer(
                        1,
                        0
                );

        RecordPointer second =
                new RecordPointer(
                        2,
                        0
                );

        index.insert(
                10,
                first
        );

        index.insert(
                10,
                second
        );

        assertTrue(
                index.remove(
                        10,
                        first
                )
        );

        assertEquals(
                List.of(second),
                index.search(10)
        );
    }

    @Test
    void shouldUpdatePointerThroughIndexApi() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        RecordPointer oldPointer =
                new RecordPointer(
                        1,
                        0
                );

        RecordPointer newPointer =
                new RecordPointer(
                        99,
                        5
                );

        index.insert(
                10,
                oldPointer
        );

        assertTrue(
                index.update(
                        10,
                        oldPointer,
                        newPointer
                )
        );

        assertEquals(
                List.of(newPointer),
                index.search(10)
        );
    }

    @Test
    void shouldPreserveDuplicateBucketWhenUpdatingOnePointer() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        RecordPointer first =
                new RecordPointer(
                        1,
                        0
                );

        RecordPointer second =
                new RecordPointer(
                        2,
                        0
                );

        RecordPointer replacement =
                new RecordPointer(
                        3,
                        0
                );

        index.insert(
                10,
                first
        );

        index.insert(
                10,
                second
        );

        assertTrue(
                index.update(
                        10,
                        first,
                        replacement
                )
        );

        assertEquals(
                List.of(
                        second,
                        replacement
                ),
                index.search(10)
        );
    }

    @Test
    void shouldClearBPlusTreeBackend() {

        Index<Integer> index =
                createIndexWithKeys(100);

        assertTrue(
                index.getTreeHeight() >= 2
        );

        index.clear();

        assertTrue(
                index.isEmpty()
        );

        assertEquals(
                0,
                index.size()
        );

        assertEquals(
                0,
                index.pointerCount()
        );

        assertEquals(
                1,
                index.getTreeHeight()
        );
    }

    @Test
    void shouldReturnUnmodifiableEntryMap() {

        Index<Integer> index =
                createIndexWithKeys(3);

        Map<Integer, List<RecordPointer>> entries =
                index.getAllEntries();

        assertThrows(
                UnsupportedOperationException.class,
                () -> entries.put(
                        99,
                        List.of(
                                new RecordPointer(
                                        99,
                                        0
                                )
                        )
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> entries.get(1)
                        .add(
                                new RecordPointer(
                                        100,
                                        0
                                )
                        )
        );
    }

    @Test
    void shouldScanAllEntriesInKeyOrder() {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        index.insert(
                30,
                new RecordPointer(30, 0)
        );

        index.insert(
                10,
                new RecordPointer(10, 0)
        );

        index.insert(
                20,
                new RecordPointer(20, 0)
        );

        assertPointerPageIds(
                index.scanAll(),
                10,
                20,
                30
        );
    }

    private Index<Integer> createIndexWithKeys(
            int max
    ) {

        Index<Integer> index =
                new Index<>(
                        metadata(
                                IndexType.NON_UNIQUE
                        )
                );

        for (int i = 1; i <= max; i++) {

            index.insert(
                    i,
                    new RecordPointer(
                            i,
                            0
                    )
            );
        }

        return index;
    }

    private IndexMetadata metadata(
            IndexType indexType
    ) {

        return new IndexMetadata(
                100L,
                "idx_bptree_test",
                "test_db",
                "test_table",
                "test_column",
                indexType
        );
    }

    private void assertPointerPageIds(
            List<RecordPointer> pointers,
            int... expectedPageIds
    ) {

        assertEquals(
                expectedPageIds.length,
                pointers.size()
        );

        for (int i = 0;
             i < expectedPageIds.length;
             i++) {

            assertEquals(
                    expectedPageIds[i],
                    pointers.get(i)
                            .getPageId()
            );
        }
    }
}