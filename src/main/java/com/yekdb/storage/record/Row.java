package com.yekdb.storage.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB içerisinde bir tablonun mantıksal satırını temsil eder.
 *
 * Row nesnesi, sütunlara ait değerleri sıralı olarak saklar.
 *
 * Örnek:
 *
 * Row row = new Row(List.of(
 *         1,
 *         "Emre",
 *         21,
 *         true
 * ));
 *
 * Değerlerin sırası, tablo sütunlarının sırasıyla aynı olmalıdır.
 */
public class Row {

    private final List<Object> values;

    /**
     * Boş bir Row oluşturur.
     */
    public Row() {
        this.values = new ArrayList<>();
    }

    /**
     * Verilen değerlerle yeni bir Row oluşturur.
     *
     * @param values Satır içerisinde saklanacak değerler
     */
    public Row(List<?> values) {
        if (values == null) {
            throw new IllegalArgumentException(
                    "Row değer listesi null olamaz."
            );
        }

        this.values = new ArrayList<>(values.size());

        for (Object value : values) {
            validateValue(value);
            this.values.add(value);
        }
    }

    /**
     * Satırın sonuna yeni bir değer ekler.
     *
     * @param value Eklenecek sütun değeri
     */
    public void addValue(Object value) {
        validateValue(value);
        values.add(value);
    }

    /**
     * Belirtilen indeksteki sütun değerini döndürür.
     *
     * @param index Sütun indeksi
     * @return Sütun değeri
     */
    public Object getValue(int index) {
        validateIndex(index);
        return values.get(index);
    }

    /**
     * Belirtilen indeksteki değeri istenen Java tipinde döndürür.
     *
     * Örnek:
     *
     * int id = row.getValue(0, Integer.class);
     * String name = row.getValue(1, String.class);
     *
     * @param index Sütun indeksi
     * @param type Beklenen Java tipi
     * @return Tip dönüşümü yapılmış sütun değeri
     * @param <T> Beklenen değer tipi
     */
    public <T> T getValue(int index, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Beklenen değer tipi null olamaz."
            );
        }

        Object value = getValue(index);

        if (!type.isInstance(value)) {
            throw new IllegalStateException(
                    "Sütun değeri beklenen tipte değil. " +
                            "Beklenen: " + type.getSimpleName() +
                            ", mevcut: " + value.getClass().getSimpleName()
            );
        }

        return type.cast(value);
    }

    /**
     * Belirtilen indeksteki sütun değerini günceller.
     *
     * @param index Güncellenecek sütun indeksi
     * @param newValue Yeni sütun değeri
     */
    public void setValue(int index, Object newValue) {
        validateIndex(index);
        validateValue(newValue);

        values.set(index, newValue);
    }

    /**
     * Row içerisindeki değerlerin değiştirilemez görünümünü döndürür.
     *
     * Dışarıdan alınan liste üzerinden Row içeriği değiştirilemez.
     *
     * @return Değiştirilemez değer listesi
     */
    public List<Object> getValues() {
        return Collections.unmodifiableList(
                new ArrayList<>(values)
        );
    }

    /**
     * Satırda bulunan sütun değeri sayısını döndürür.
     *
     * @return Sütun sayısı
     */
    public int size() {
        return values.size();
    }

    /**
     * Satırın boş olup olmadığını kontrol eder.
     *
     * @return Satır boşsa true
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Row içerisinde belirli bir değerin bulunup bulunmadığını kontrol eder.
     *
     * @param value Aranacak değer
     * @return Değer bulunuyorsa true
     */
    public boolean contains(Object value) {
        return values.contains(value);
    }

    /**
     * Row içerisindeki tüm değerleri temizler.
     */
    public void clear() {
        values.clear();
    }

    /**
     * Belirtilen sütun indeksinin geçerli olup olmadığını kontrol eder.
     *
     * @param index Kontrol edilecek indeks
     */
    private void validateIndex(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException(
                    "Geçersiz sütun indeksi: " + index +
                            ". Geçerli aralık: 0-" +
                            (values.size() - 1)
            );
        }
    }

    /**
     * Row içerisine eklenecek değerin desteklenen bir tipte
     * olup olmadığını kontrol eder.
     *
     * Sprint 00-08 kapsamında desteklenen tipler:
     *
     * - Integer
     * - Long
     * - Double
     * - Boolean
     * - String
     *
     * @param value Kontrol edilecek değer
     */
    private void validateValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Row sütun değeri null olamaz."
            );
        }

        if (!(value instanceof Integer)
                && !(value instanceof Long)
                && !(value instanceof Double)
                && !(value instanceof Boolean)
                && !(value instanceof String)) {

            throw new IllegalArgumentException(
                    "Desteklenmeyen Row değeri tipi: " +
                            value.getClass().getName()
            );
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Row row)) {
            return false;
        }

        return Objects.equals(values, row.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "Row{" +
                "values=" + values +
                '}';
    }
}