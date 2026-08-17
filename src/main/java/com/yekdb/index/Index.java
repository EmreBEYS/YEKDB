package com.yekdb.index;

import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.index.exception.InvalidIndexException;

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
     * B+ Tree implementasyonu gelene kadar kullanılan
     * bellek içi indeks yapısı.
     */
    private final Map<K, List<RecordPointer>> entries;

    public Index(IndexMetadata metadata) {

        validateMetadata(metadata);

        this.metadata = metadata;
        this.entries = new LinkedHashMap<>();
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

        List<RecordPointer> pointers =
                entries.get(key);

        if (pointers == null) {

            List<RecordPointer> newPointers =
                    new ArrayList<>();

            newPointers.add(pointer);

            entries.put(
                    key,
                    newPointers
            );

            return;
        }

        if (metadata.getIndexType().isUnique()) {

            throw new DuplicateIndexKeyException(
                    "Tekrar eden indeks anahtarı kabul edilmedi. "
                            + "Index: "
                            + metadata.getIndexName()
                            + ", key: "
                            + key
            );
        }

        if (!pointers.contains(pointer)) {
            pointers.add(pointer);
        }
    }

    /**
     * IndexEntry nesnesi üzerinden kayıt ekler.
     */
    public void insert(IndexEntry<K> entry) {

        if (entry == null || !entry.isValid()) {

            throw new InvalidIndexException(
                    "Geçerli bir IndexEntry sağlanmalıdır."
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

        List<RecordPointer> pointers =
                entries.get(key);

        if (pointers == null) {
            return List.of();
        }

        return List.copyOf(pointers);
    }

    /**
     * Anahtarın indekste bulunup bulunmadığını kontrol eder.
     */
    public boolean containsKey(K key) {

        validateKey(key);

        return entries.containsKey(key);
    }

    /**
     * Anahtarı ve ona bağlı bütün pointer'ları siler.
     */
    public boolean remove(K key) {

        validateKey(key);

        return entries.remove(key) != null;
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

        List<RecordPointer> pointers =
                entries.get(key);

        if (pointers == null) {
            return false;
        }

        boolean removed =
                pointers.remove(pointer);

        if (pointers.isEmpty()) {
            entries.remove(key);
        }

        return removed;
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
                entries.get(key);

        if (pointers == null) {
            return false;
        }

        int pointerIndex =
                pointers.indexOf(oldPointer);

        if (pointerIndex < 0) {
            return false;
        }

        if (pointers.contains(newPointer)
                && !oldPointer.equals(newPointer)) {

            return false;
        }

        pointers.set(
                pointerIndex,
                newPointer
        );

        return true;
    }

    /**
     * Bütün indeks girdilerini temizler.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Farklı anahtar sayısını döndürür.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Toplam RecordPointer sayısını döndürür.
     */
    public int pointerCount() {

        int count = 0;

        for (List<RecordPointer> pointers
                : entries.values()) {

            count += pointers.size();
        }

        return count;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public IndexMetadata getMetadata() {
        return metadata;
    }

    /**
     * İndeks girdilerinin güvenli kopyasını döndürür.
     */
    public Map<K, List<RecordPointer>> getAllEntries() {

        Map<K, List<RecordPointer>> copiedEntries =
                new LinkedHashMap<>();

        for (Map.Entry<K, List<RecordPointer>> entry
                : entries.entrySet()) {

            copiedEntries.put(
                    entry.getKey(),
                    List.copyOf(
                            entry.getValue()
                    )
            );
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

        for (Map.Entry<K, List<RecordPointer>> mapEntry
                : entries.entrySet()) {

            for (RecordPointer pointer
                    : mapEntry.getValue()) {

                result.add(
                        new IndexEntry<>(
                                mapEntry.getKey(),
                                pointer
                        )
                );
            }
        }

        Collections.sort(result);

        return List.copyOf(result);
    }

    /**
     * Metadata bütünlüğünü kontrol eder.
     */
    private void validateMetadata(
            IndexMetadata metadata
    ) {

        if (metadata == null) {

            throw new InvalidIndexException(
                    "Index metadata null olamaz."
            );
        }

        if (!metadata.isValid()) {

            throw new InvalidIndexException(
                    "Geçersiz index metadata: "
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
                    "Index anahtarı null olamaz."
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
                    "Geçerli bir RecordPointer sağlanmalıdır."
            );
        }
    }

    @Override
    public String toString() {

        return "Index{" +
                "metadata=" + metadata +
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
                entries,
                index.entries
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                metadata,
                entries
        );
    }
}