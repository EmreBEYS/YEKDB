package com.yekdb.index;

import com.yekdb.index.bplustree.BPlusTree;
import com.yekdb.index.bplustree.BPlusTreeInternalNode;
import com.yekdb.index.bplustree.BPlusTreeLeafNode;
import com.yekdb.index.bplustree.BPlusTreeNode;
import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.index.exception.InvalidIndexException;
import com.yekdb.storage.record.RecordId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tek bir indeks yapısını temsil eder.
 *
 * PRIMARY ve UNIQUE indekslerde her anahtar yalnızca bir kez
 * bulunabilir.
 *
 * NON_UNIQUE indekslerde aynı anahtara birden fazla
 * RecordPointer bağlanabilir.
 *
 * Sprint 00-29 Phase 10 itibarıyla indeks girdilerinin fiziksel
 * bellek içi backend'i B+ Tree'dir.
 *
 * @param <K> indeks anahtar tipi
 */
public class Index<K extends Comparable<K>>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * İndekse ait metadata bilgisi.
     */
    private final IndexMetadata metadata;

    /**
     * İndeks girdilerini sıralı olarak tutan B+ Tree backend'i.
     */
    private BPlusTree<K> tree;

    public Index(IndexMetadata metadata) {

        validateMetadata(metadata);

        this.metadata = metadata;
        this.tree = new BPlusTree<>();
    }

    /**
     * Yeni indeks girdisi ekler.
     */
    public void insert(
            K key,
            RecordPointer pointer
    ) {

        validateKey(key);
        validatePointer(pointer);

        if (metadata.getIndexType().isUnique()
                && tree.containsKey(key)) {

            throw new DuplicateIndexKeyException(
                    "Duplicate index key is not allowed. "
                            + "Index: "
                            + metadata.getIndexName()
                            + ", key: "
                            + key
            );
        }

        tree.insert(
                key,
                pointer
        );
    }

    /**
     * Canonical storage RecordId ile indeks girdisi ekler.
     */
    public void insertRecordId(
            K key,
            RecordId recordId
    ) {
        insert(
                key,
                RecordPointer.fromRecordId(recordId)
        );
    }

    /**
     * IndexEntry nesnesi üzerinden kayıt ekler.
     */
    public void insert(IndexEntry<K> entry) {

        if (entry == null || !entry.isValid()) {

            throw new InvalidIndexException(
                    "A valid IndexEntry must be provided."
            );
        }

        insert(
                entry.getKey(),
                entry.getPointer()
        );
    }

    /**
     * Anahtara ait RecordPointer listesini döndürür.
     */
    public List<RecordPointer> search(K key) {

        validateKey(key);

        return tree.search(key);
    }

    /**
     * Anahtara ait pointer'ları canonical RecordId listesi olarak döndürür.
     */
    public List<RecordId> searchRecordIds(K key) {
        return search(key)
                .stream()
                .map(RecordPointer::toRecordId)
                .toList();
    }

    /**
     * Alt ve üst sınır dahil olacak şekilde range araması yapar.
     */
    public List<RecordPointer> searchRange(
            K fromKey,
            K toKey
    ) {

        validateKey(fromKey);
        validateKey(toKey);

        return tree.searchRange(
                fromKey,
                toKey
        );
    }

    /**
     * Alt ve üst sınırların dahil edilme davranışı seçilerek range araması yapar.
     */
    public List<RecordPointer> searchRange(
            K fromKey,
            boolean fromInclusive,
            K toKey,
            boolean toInclusive
    ) {

        validateKey(fromKey);
        validateKey(toKey);

        return tree.searchRange(
                fromKey,
                fromInclusive,
                toKey,
                toInclusive
        );
    }

    /**
     * Belirtilen anahtardan büyük girdilerin pointer'larını döndürür.
     */
    public List<RecordPointer> searchGreaterThan(K key) {

        validateKey(key);

        return tree.searchGreaterThan(key);
    }

    /**
     * Belirtilen anahtardan büyük veya eşit girdilerin pointer'larını döndürür.
     */
    public List<RecordPointer> searchGreaterThanOrEqual(K key) {

        validateKey(key);

        return tree.searchGreaterThanOrEqual(key);
    }

    /**
     * Belirtilen anahtardan küçük girdilerin pointer'larını döndürür.
     */
    public List<RecordPointer> searchLessThan(K key) {

        validateKey(key);

        return tree.searchLessThan(key);
    }

    /**
     * Belirtilen anahtardan küçük veya eşit girdilerin pointer'larını döndürür.
     */
    public List<RecordPointer> searchLessThanOrEqual(K key) {

        validateKey(key);

        return tree.searchLessThanOrEqual(key);
    }

    /**
     * İndeksteki bütün pointer'ları anahtar sırasına göre döndürür.
     */
    public List<RecordPointer> scanAll() {
        return tree.scanAll();
    }

    /**
     * Anahtarın indekste bulunup bulunmadığını kontrol eder.
     */
    public boolean containsKey(K key) {

        validateKey(key);

        return tree.containsKey(key);
    }

    /**
     * Anahtarı ve ona bağlı bütün pointer'ları siler.
     */
    public boolean remove(K key) {

        validateKey(key);

        return tree.delete(key);
    }

    /**
     * Anahtara bağlı belirli bir pointer'ı siler.
     */
    public boolean remove(
            K key,
            RecordPointer pointer
    ) {

        validateKey(key);
        validatePointer(pointer);

        return tree.delete(
                key,
                pointer
        );
    }

    /**
     * Anahtara bağlı canonical RecordId ilişkisini siler.
     */
    public boolean removeRecordId(
            K key,
            RecordId recordId
    ) {
        return remove(
                key,
                RecordPointer.fromRecordId(recordId)
        );
    }

    /**
     * Var olan pointer'ı yeni pointer ile değiştirir.
     */
    public boolean update(
            K key,
            RecordPointer oldPointer,
            RecordPointer newPointer
    ) {

        validateKey(key);
        validatePointer(oldPointer);
        validatePointer(newPointer);

        List<RecordPointer> pointers =
                tree.search(key);

        if (pointers.isEmpty()) {
            return false;
        }

        if (!pointers.contains(oldPointer)) {
            return false;
        }

        if (pointers.contains(newPointer)
                && !oldPointer.equals(newPointer)) {

            return false;
        }

        if (oldPointer.equals(newPointer)) {
            return true;
        }

        boolean removed =
                tree.delete(
                        key,
                        oldPointer
                );

        if (!removed) {
            return false;
        }

        tree.insert(
                key,
                newPointer
        );

        return true;
    }

    /**
     * Canonical RecordId ilişkisini yeni fiziksel adresle günceller.
     */
    public boolean updateRecordId(
            K key,
            RecordId oldRecordId,
            RecordId newRecordId
    ) {
        return update(
                key,
                RecordPointer.fromRecordId(oldRecordId),
                RecordPointer.fromRecordId(newRecordId)
        );
    }

    /**
     * Bütün indeks girdilerini temizler.
     *
     * Aynı order değeri korunarak yeni boş B+ Tree oluşturulur.
     */
    public void clear() {
        tree = new BPlusTree<>(
                tree.getOrder()
        );
    }

    /**
     * Farklı anahtar sayısını döndürür.
     */
    public int size() {

        int count = 0;

        BPlusTreeLeafNode<K> leaf =
                firstLeaf();

        while (leaf != null) {

            count += leaf.getKeyCount();
            leaf = leaf.getNextLeaf();
        }

        return count;
    }

    /**
     * Toplam RecordPointer sayısını döndürür.
     */
    public int pointerCount() {
        return tree.scanAll().size();
    }

    public boolean isEmpty() {
        return tree.isEmpty();
    }

    public IndexMetadata getMetadata() {
        return metadata;
    }

    /**
     * B+ Tree yüksekliğini döndürür.
     *
     * Test, diagnostics ve ileride query planner maliyet hesabı için
     * kullanılabilir.
     */
    public int getTreeHeight() {
        return tree.getHeight();
    }

    /**
     * B+ Tree order değerini döndürür.
     */
    public int getTreeOrder() {
        return tree.getOrder();
    }

    /**
     * İndeks girdilerinin güvenli ve anahtara göre sıralı kopyasını döndürür.
     */
    public Map<K, List<RecordPointer>> getAllEntries() {

        Map<K, List<RecordPointer>> copiedEntries =
                new LinkedHashMap<>();

        BPlusTreeLeafNode<K> leaf =
                firstLeaf();

        while (leaf != null) {

            for (int i = 0;
                 i < leaf.getKeyCount();
                 i++) {

                copiedEntries.put(
                        leaf.getKey(i),
                        List.copyOf(
                                leaf.getPointers(i)
                        )
                );
            }

            leaf = leaf.getNextLeaf();
        }

        return Collections.unmodifiableMap(
                copiedEntries
        );
    }

    /**
     * İndeks girdilerini sıralı IndexEntry listesi olarak döndürür.
     */
    public List<IndexEntry<K>> getEntryList() {

        List<IndexEntry<K>> result =
                new ArrayList<>();

        BPlusTreeLeafNode<K> leaf =
                firstLeaf();

        while (leaf != null) {

            for (int i = 0;
                 i < leaf.getKeyCount();
                 i++) {

                K key =
                        leaf.getKey(i);

                for (RecordPointer pointer
                        : leaf.getPointers(i)) {

                    result.add(
                            new IndexEntry<>(
                                    key,
                                    pointer
                            )
                    );
                }
            }

            leaf = leaf.getNextLeaf();
        }

        return List.copyOf(result);
    }

    /**
     * Tree'nin en soldaki leaf node'unu döndürür.
     */
    @SuppressWarnings("unchecked")
    private BPlusTreeLeafNode<K> firstLeaf() {

        BPlusTreeNode<K> current =
                tree.getRoot();

        while (!current.isLeaf()) {

            BPlusTreeInternalNode<K> internal =
                    (BPlusTreeInternalNode<K>) current;

            if (internal.getChildCount() == 0) {
                throw new IllegalStateException(
                        "B+ Tree internal node cannot have zero children during index traversal."
                );
            }

            current =
                    internal.getChild(0);
        }

        return (BPlusTreeLeafNode<K>) current;
    }

    /**
     * Metadata bütünlüğünü kontrol eder.
     */
    private void validateMetadata(
            IndexMetadata metadata
    ) {

        if (metadata == null) {

            throw new InvalidIndexException(
                    "Index metadata cannot be null."
            );
        }

        if (!metadata.isValid()) {

            throw new InvalidIndexException(
                    "Invalid index metadata: "
                            + metadata
            );
        }
    }

    /**
     * Anahtar bütünlüğünü kontrol eder.
     */
    private void validateKey(K key) {

        if (key == null) {

            throw new InvalidIndexException(
                    "Index key cannot be null."
            );
        }
    }

    /**
     * RecordPointer bütünlüğünü kontrol eder.
     */
    private void validatePointer(
            RecordPointer pointer
    ) {

        if (pointer == null
                || !pointer.isValid()) {

            throw new InvalidIndexException(
                    "A valid RecordPointer must be provided."
            );
        }
    }

    @Override
    public String toString() {

        return "Index{" +
                "metadata=" + metadata +
                ", backend=BPlusTree" +
                ", treeHeight=" + getTreeHeight() +
                ", keyCount=" + size() +
                ", pointerCount=" + pointerCount() +
                '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof Index<?> index)) {
            return false;
        }

        return Objects.equals(
                metadata,
                index.metadata
        )
                && Objects.equals(
                getAllEntries(),
                index.getAllEntries()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                metadata,
                getAllEntries()
        );
    }
}
