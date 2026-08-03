package com.yekdb.index;

import java.io.Serializable;
import java.util.Objects;

/**
 * Index içerisinde bulunan tek bir girdiyi temsil eder.
 *
 * Her IndexEntry bir anahtar (key) ile
 * ilgili RecordPointer bilgisini tutar.
 *
 * Örneğin:
 *
 * Key : 1001
 * Pointer : (Page 5, Slot 12)
 *
 * Daha sonra B+ Tree node'ları da bu nesneyi kullanacaktır.
 *
 * @param <K> Index anahtarının veri tipi
 */
public class IndexEntry<K extends Comparable<K>> implements Comparable<IndexEntry<K>>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Index anahtarı.
     */
    private K key;

    /**
     * Kaydın fiziksel adresi.
     */
    private RecordPointer pointer;

    /**
     * Boş constructor.
     */
    public IndexEntry() {
    }

    /**
     * Parametreli constructor.
     *
     * @param key Index anahtarı
     * @param pointer Kayıt adresi
     */
    public IndexEntry(K key, RecordPointer pointer) {
        this.key = key;
        this.pointer = pointer;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public RecordPointer getPointer() {
        return pointer;
    }

    public void setPointer(RecordPointer pointer) {
        this.pointer = pointer;
    }

    /**
     * Girdinin kullanılabilir olup olmadığını kontrol eder.
     */
    public boolean isValid() {
        return key != null &&
                pointer != null &&
                pointer.isValid();
    }

    @Override
    public int compareTo(IndexEntry<K> other) {
        return key.compareTo(other.key);
    }

    @Override
    public String toString() {
        return "IndexEntry{" +
                "key=" + key +
                ", pointer=" + pointer +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;

        if (!(o instanceof IndexEntry<?> that))
            return false;

        return Objects.equals(key, that.key) &&
                Objects.equals(pointer, that.pointer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, pointer);
    }

}