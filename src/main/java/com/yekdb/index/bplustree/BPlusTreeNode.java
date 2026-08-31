package com.yekdb.index.bplustree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B+ Tree içerisindeki bütün node türlerinin temel sınıfıdır.
 *
 * @param <K> B+ Tree anahtar tipi
 */
public abstract class BPlusTreeNode<K extends Comparable<K>> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int order;
    protected final List<K> keys;

    protected BPlusTreeNode(int order) {
        if (order < 3) {
            throw new IllegalArgumentException("B+ Tree order must be at least 3.");
        }
        this.order = order;
        this.keys = new ArrayList<>();
    }

    public abstract boolean isLeaf();

    public int getOrder() {
        return order;
    }

    public int getMaxKeys() {
        return order - 1;
    }

    public int getKeyCount() {
        return keys.size();
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public boolean isFull() {
        return keys.size() >= getMaxKeys();
    }

    public boolean isOverflow() {
        return keys.size() > getMaxKeys();
    }

    public List<K> getKeys() {
        return Collections.unmodifiableList(keys);
    }

    public K getKey(int index) {
        return keys.get(index);
    }

    /**
     * Verilen key'in node içerisindeki binary-search sonucunu döndürür.
     */
    public int indexOfKey(K key) {
        if (key == null) {
            return -1;
        }
        return Collections.binarySearch(keys, key);
    }

    public boolean containsKey(K key) {
        return indexOfKey(key) >= 0;
    }

    protected void addKey(int index, K key) {
        if (key == null) {
            throw new IllegalArgumentException("B+ Tree key cannot be null.");
        }
        keys.add(index, key);
    }

    protected void addKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("B+ Tree key cannot be null.");
        }
        keys.add(key);
    }

    protected K removeKey(int index) {
        return keys.remove(index);
    }

    protected void clearKeys() {
        keys.clear();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "order=" + order +
                ", keys=" + keys +
                '}';
    }
}
