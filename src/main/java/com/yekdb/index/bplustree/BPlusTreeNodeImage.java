package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Disk sayfasına yazılmadan önce bir B+ Tree node'unun
 * taşınabilir binary modelini temsil eder.
 *
 * Canlı tree node referansları yerine page id referansları taşır.
 * Böylece Phase 12'de fiziksel Page katmanına bağlanabilir.
 */
public final class BPlusTreeNodeImage<K extends Comparable<K>> {

    public static final int NO_PAGE_ID = -1;

    private final boolean leaf;

    private final int order;

    private final int pageId;

    private final int parentPageId;

    private final int previousLeafPageId;

    private final int nextLeafPageId;

    private final List<K> keys;

    private final List<List<RecordPointer>> valueBuckets;

    private final List<Integer> childPageIds;

    private BPlusTreeNodeImage(
            boolean leaf,
            int order,
            int pageId,
            int parentPageId,
            int previousLeafPageId,
            int nextLeafPageId,
            List<K> keys,
            List<List<RecordPointer>> valueBuckets,
            List<Integer> childPageIds
    ) {

        validateCommon(
                order,
                pageId,
                parentPageId,
                keys
        );

        this.leaf = leaf;
        this.order = order;
        this.pageId = pageId;
        this.parentPageId = parentPageId;
        this.previousLeafPageId =
                previousLeafPageId;
        this.nextLeafPageId =
                nextLeafPageId;

        this.keys =
                List.copyOf(keys);

        this.valueBuckets =
                copyBuckets(valueBuckets);

        this.childPageIds =
                List.copyOf(childPageIds);

        validateStructure();
    }

    public static <K extends Comparable<K>>
    BPlusTreeNodeImage<K> leaf(
            int order,
            int pageId,
            int parentPageId,
            int previousLeafPageId,
            int nextLeafPageId,
            List<K> keys,
            List<List<RecordPointer>> valueBuckets
    ) {

        return new BPlusTreeNodeImage<>(
                true,
                order,
                pageId,
                parentPageId,
                previousLeafPageId,
                nextLeafPageId,
                keys,
                valueBuckets,
                List.of()
        );
    }

    public static <K extends Comparable<K>>
    BPlusTreeNodeImage<K> internal(
            int order,
            int pageId,
            int parentPageId,
            List<K> keys,
            List<Integer> childPageIds
    ) {

        return new BPlusTreeNodeImage<>(
                false,
                order,
                pageId,
                parentPageId,
                NO_PAGE_ID,
                NO_PAGE_ID,
                keys,
                List.of(),
                childPageIds
        );
    }

    public boolean isLeaf() {
        return leaf;
    }

    public int getOrder() {
        return order;
    }

    public int getPageId() {
        return pageId;
    }

    public int getParentPageId() {
        return parentPageId;
    }

    public int getPreviousLeafPageId() {
        return previousLeafPageId;
    }

    public int getNextLeafPageId() {
        return nextLeafPageId;
    }

    public List<K> getKeys() {
        return keys;
    }

    public List<List<RecordPointer>> getValueBuckets() {
        return valueBuckets;
    }

    public List<Integer> getChildPageIds() {
        return childPageIds;
    }

    public int getKeyCount() {
        return keys.size();
    }

    private static <K extends Comparable<K>>
    void validateCommon(
            int order,
            int pageId,
            int parentPageId,
            List<K> keys
    ) {

        if (order < BPlusTree.MIN_ORDER) {

            throw new IllegalArgumentException(
                    "B+ Tree order must be at least "
                            + BPlusTree.MIN_ORDER
                            + "."
            );
        }

        if (pageId < 0) {

            throw new IllegalArgumentException(
                    "Page id cannot be negative."
            );
        }

        if (parentPageId < NO_PAGE_ID) {

            throw new IllegalArgumentException(
                    "Parent page id is invalid."
            );
        }

        if (keys == null) {

            throw new IllegalArgumentException(
                    "Key list cannot be null."
            );
        }

        K previous = null;

        for (K key : keys) {

            if (key == null) {

                throw new IllegalArgumentException(
                        "B+ Tree key cannot be null."
                );
            }

            if (previous != null
                    && previous.compareTo(key) >= 0) {

                throw new IllegalArgumentException(
                        "B+ Tree keys must be strictly sorted."
                );
            }

            previous = key;
        }
    }

    private void validateStructure() {

        if (keys.size() > order - 1) {

            throw new IllegalArgumentException(
                    "Node key count exceeds B+ Tree order capacity."
            );
        }

        if (leaf) {

            if (previousLeafPageId < NO_PAGE_ID
                    || nextLeafPageId < NO_PAGE_ID) {

                throw new IllegalArgumentException(
                        "Leaf sibling page id is invalid."
                );
            }

            if (!childPageIds.isEmpty()) {

                throw new IllegalArgumentException(
                        "Leaf node cannot contain child page ids."
                );
            }

            if (valueBuckets.size()
                    != keys.size()) {

                throw new IllegalArgumentException(
                        "Leaf key and value bucket counts must match."
                );
            }

            for (List<RecordPointer> bucket
                    : valueBuckets) {

                if (bucket.isEmpty()) {

                    throw new IllegalArgumentException(
                            "Leaf value bucket cannot be empty."
                    );
                }

                for (RecordPointer pointer
                        : bucket) {

                    if (pointer == null
                            || !pointer.isValid()) {

                        throw new IllegalArgumentException(
                                "Leaf bucket contains an invalid RecordPointer."
                        );
                    }
                }
            }

            return;
        }

        if (!valueBuckets.isEmpty()) {

            throw new IllegalArgumentException(
                    "Internal node cannot contain value buckets."
            );
        }

        if (keys.isEmpty()
                && childPageIds.isEmpty()) {

            return;
        }

        if (childPageIds.size()
                != keys.size() + 1) {

            throw new IllegalArgumentException(
                    "Internal child count must equal key count plus one."
            );
        }

        for (Integer childPageId
                : childPageIds) {

            if (childPageId == null
                    || childPageId < 0) {

                throw new IllegalArgumentException(
                        "Internal child page id is invalid."
                );
            }
        }
    }

    private static List<List<RecordPointer>>
    copyBuckets(
            List<List<RecordPointer>> buckets
    ) {

        if (buckets == null) {

            throw new IllegalArgumentException(
                    "Value bucket list cannot be null."
            );
        }

        List<List<RecordPointer>> copy =
                new ArrayList<>(
                        buckets.size()
                );

        for (List<RecordPointer> bucket
                : buckets) {

            if (bucket == null) {

                throw new IllegalArgumentException(
                        "Leaf value bucket cannot be null."
                );
            }

            copy.add(
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    bucket
                            )
                    )
            );
        }

        return Collections.unmodifiableList(
                copy
        );
    }
}