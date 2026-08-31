package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B+ Tree leaf node yapısıdır.
 *
 * Leaf node key'leri, key'lere ait RecordPointer bucket'larını ve
 * sıralı leaf traversal bağlantılarını tutar.
 *
 * @param <K> index anahtar tipi
 */
public class BPlusTreeLeafNode<K extends Comparable<K>> extends BPlusTreeNode<K> {

    private static final long serialVersionUID = 1L;

    private final List<List<RecordPointer>> values;
    private BPlusTreeLeafNode<K> nextLeaf;
    private BPlusTreeLeafNode<K> previousLeaf;

    public BPlusTreeLeafNode(int order) {
        super(order);
        this.values = new ArrayList<>();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    public int getValueCount() {
        return values.size();
    }

    public List<RecordPointer> getPointers(int index) {
        return Collections.unmodifiableList(values.get(index));
    }

    /**
     * Leaf değerlerinin tamamının dışarıdan değiştirilemeyen kopyasını döndürür.
     */
    public List<List<RecordPointer>> getValues() {
        List<List<RecordPointer>> result = new ArrayList<>(values.size());
        for (List<RecordPointer> pointers : values) {
            result.add(List.copyOf(pointers));
        }
        return Collections.unmodifiableList(result);
    }

    public BPlusTreeLeafNode<K> getNextLeaf() {
        return nextLeaf;
    }

    public void setNextLeaf(BPlusTreeLeafNode<K> nextLeaf) {
        this.nextLeaf = nextLeaf;
    }

    public BPlusTreeLeafNode<K> getPreviousLeaf() {
        return previousLeaf;
    }

    public void setPreviousLeaf(BPlusTreeLeafNode<K> previousLeaf) {
        this.previousLeaf = previousLeaf;
    }

    /**
     * Key'in leaf içerisindeki konumunu binary search ile bulur.
     */
    int findKeyIndex(K key) {
        validateKey(key);
        return Collections.binarySearch(keys, key);
    }

    /**
     * Key mevcutsa index'ini, mevcut değilse sıralı ekleme konumunu döndürür.
     */
    int findInsertionIndex(K key) {
        int result = findKeyIndex(key);
        return result >= 0 ? result : -result - 1;
    }

    /**
     * Key için bulunan pointer listesini döndürür.
     * Key bulunamazsa boş liste döner.
     */
    public List<RecordPointer> findPointers(K key) {
        int index = findKeyIndex(key);
        if (index < 0) {
            return List.of();
        }
        return getPointers(index);
    }

    /**
     * Leaf içerisine key'i sıralı olarak ekler.
     * Key zaten mevcutsa yeni key oluşturmak yerine pointer aynı bucket'a eklenir.
     *
     * @return yeni key eklendiyse true, mevcut bucket güncellendiyse false
     */
    boolean insertSorted(K key, RecordPointer pointer) {
        validatePointer(pointer);

        int searchResult = findKeyIndex(key);
        if (searchResult >= 0) {
            addPointer(searchResult, pointer);
            return false;
        }

        int insertionIndex = -searchResult - 1;
        addEntry(insertionIndex, key, pointer);
        return true;
    }

    void addEntry(int index, K key, List<RecordPointer> pointers) {
        validateKey(key);
        if (pointers == null || pointers.isEmpty()) {
            throw new IllegalArgumentException("Leaf pointer list cannot be empty.");
        }
        for (RecordPointer pointer : pointers) {
            validatePointer(pointer);
        }

        addKey(index, key);
        values.add(index, new ArrayList<>(pointers));
    }

    void addEntry(int index, K key, RecordPointer pointer) {
        validatePointer(pointer);
        addEntry(index, key, List.of(pointer));
    }

    void addPointer(int keyIndex, RecordPointer pointer) {
        validatePointer(pointer);
        List<RecordPointer> pointers = values.get(keyIndex);
        if (!pointers.contains(pointer)) {
            pointers.add(pointer);
        }
    }

    /**
     * Belirtilen key bucket'ından tek bir RecordPointer siler.
     *
     * @return pointer bucket içinde bulunup silindiyse true
     */
    boolean removePointer(int keyIndex, RecordPointer pointer) {
        validatePointer(pointer);
        return values.get(keyIndex).remove(pointer);
    }

    List<RecordPointer> removeEntry(int index) {
        removeKey(index);
        return values.remove(index);
    }

    void clearEntries() {
        clearKeys();
        values.clear();
    }

    public boolean isStructurallyValid() {
        return keys.size() == values.size();
    }

    private void validateKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Leaf key cannot be null.");
        }
    }

    private void validatePointer(RecordPointer pointer) {
        if (pointer == null || !pointer.isValid()) {
            throw new IllegalArgumentException("A valid RecordPointer must be provided.");
        }
    }

    @Override
    public String toString() {
        return "BPlusTreeLeafNode{" +
                "keys=" + keys +
                ", valueCount=" + values.size() +
                ", hasPrevious=" + (previousLeaf != null) +
                ", hasNext=" + (nextLeaf != null) +
                '}';
    }
}
