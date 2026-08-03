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
 * <p>Bu sınıf, indeks anahtarları ile kayıtların fiziksel konumlarını
 * gösteren {@link RecordPointer} nesneleri arasındaki ilişkiyi yönetir.</p>
 *
 * <p>PRIMARY ve UNIQUE indekslerde her anahtar yalnızca bir kez bulunabilir.
 * NON_UNIQUE indekslerde ise aynı anahtara birden fazla kayıt adresi
 * bağlanabilir.</p>
 *
 * @param <K> indeks anahtarının veri tipi
 */
public class Index<K extends Comparable<K>> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * İndekse ait katalog ve tanımlayıcı bilgiler.
     */
    private final IndexMetadata metadata;

    /**
     * Anahtar ve kayıt adresi ilişkilerini tutan geçici indeks yapısı.
     *
     * <p>B+ Tree altyapısı eklenene kadar bellek içi harita kullanılmaktadır.</p>
     */
    private final Map<K, List<RecordPointer>> entries;

    /**
     * Yeni bir indeks oluşturur.
     *
     * @param metadata indeks metadata bilgileri
     * @throws InvalidIndexException metadata null veya geçersizse
     */
    public Index(IndexMetadata metadata) {
        validateMetadata(metadata);

        this.metadata = metadata;
        this.entries = new LinkedHashMap<>();
    }

    /**
     * İndekse yeni bir anahtar ve kayıt adresi ekler.
     *
     * @param key indeks anahtarı
     * @param pointer kaydın fiziksel adresi
     * @throws InvalidIndexException anahtar veya pointer geçersizse
     * @throws DuplicateIndexKeyException UNIQUE veya PRIMARY indekste
     *                                    aynı anahtar zaten bulunuyorsa
     */
    public void insert(K key, RecordPointer pointer) {
        validateKey(key);
        validatePointer(pointer);

        List<RecordPointer> pointers = entries.get(key);

        if (pointers == null) {
            List<RecordPointer> newPointers = new ArrayList<>();
            newPointers.add(pointer);
            entries.put(key, newPointers);
            return;
        }

        if (metadata.getIndexType().isUnique()) {
            throw new DuplicateIndexKeyException(
                    "Tekrar eden indeks anahtarı kabul edilmedi. "
                            + "Index: " + metadata.getIndexName()
                            + ", key: " + key
            );
        }

        if (!pointers.contains(pointer)) {
            pointers.add(pointer);
        }
    }

    /**
     * Bir {@link IndexEntry} nesnesini indekse ekler.
     *
     * @param entry eklenecek indeks girdisi
     * @throws InvalidIndexException entry null veya geçersizse
     */
    public void insert(IndexEntry<K> entry) {
        if (entry == null || !entry.isValid()) {
            throw new InvalidIndexException(
                    "Geçerli bir IndexEntry sağlanmalıdır."
            );
        }

        insert(entry.getKey(), entry.getPointer());
    }

    /**
     * Verilen anahtara bağlı kayıt adreslerini arar.
     *
     * @param key aranacak anahtar
     * @return kayıt adreslerinin değiştirilemez kopyası;
     *         anahtar bulunamazsa boş liste
     */
    public List<RecordPointer> search(K key) {
        validateKey(key);

        List<RecordPointer> pointers = entries.get(key);

        if (pointers == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(pointers)
        );
    }

    /**
     * Anahtarın indeks içerisinde bulunup bulunmadığını kontrol eder.
     *
     * @param key kontrol edilecek anahtar
     * @return anahtar mevcutsa true
     */
    public boolean containsKey(K key) {
        validateKey(key);
        return entries.containsKey(key);
    }

    /**
     * Belirtilen anahtarı ve bu anahtara ait bütün kayıt adreslerini siler.
     *
     * @param key silinecek anahtar
     * @return anahtar bulunduysa ve silindiyse true
     */
    public boolean remove(K key) {
        validateKey(key);
        return entries.remove(key) != null;
    }

    /**
     * Belirli bir anahtara bağlı tek bir kayıt adresini siler.
     *
     * <p>Anahtar altında başka pointer kalmazsa anahtar da indeksten kaldırılır.</p>
     *
     * @param key indeks anahtarı
     * @param pointer silinecek kayıt adresi
     * @return pointer bulunduysa ve silindiyse true
     */
    public boolean remove(K key, RecordPointer pointer) {
        validateKey(key);
        validatePointer(pointer);

        List<RecordPointer> pointers = entries.get(key);

        if (pointers == null) {
            return false;
        }

        boolean removed = pointers.remove(pointer);

        if (pointers.isEmpty()) {
            entries.remove(key);
        }

        return removed;
    }

    /**
     * Bir anahtarın işaret ettiği eski kayıt adresini yenisiyle değiştirir.
     *
     * @param key indeks anahtarı
     * @param oldPointer eski kayıt adresi
     * @param newPointer yeni kayıt adresi
     * @return eski pointer bulunup güncellendiyse true
     */
    public boolean update(
            K key,
            RecordPointer oldPointer,
            RecordPointer newPointer
    ) {
        validateKey(key);
        validatePointer(oldPointer);
        validatePointer(newPointer);

        List<RecordPointer> pointers = entries.get(key);

        if (pointers == null) {
            return false;
        }

        int pointerIndex = pointers.indexOf(oldPointer);

        if (pointerIndex < 0) {
            return false;
        }

        if (pointers.contains(newPointer)
                && !oldPointer.equals(newPointer)) {
            return false;
        }

        pointers.set(pointerIndex, newPointer);
        return true;
    }

    /**
     * İndeks içerisindeki bütün anahtar ve pointer ilişkilerini temizler.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * İndeksteki farklı anahtar sayısını döndürür.
     *
     * @return farklı anahtar sayısı
     */
    public int size() {
        return entries.size();
    }

    /**
     * İndekste tutulan toplam kayıt adresi sayısını döndürür.
     *
     * <p>NON_UNIQUE indekslerde bu değer {@link #size()} sonucundan
     * daha büyük olabilir.</p>
     *
     * @return toplam pointer sayısı
     */
    public int pointerCount() {
        int count = 0;

        for (List<RecordPointer> pointers : entries.values()) {
            count += pointers.size();
        }

        return count;
    }

    /**
     * İndeksin boş olup olmadığını kontrol eder.
     *
     * @return indeks boşsa true
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * İndeks metadata bilgisini döndürür.
     *
     * @return indeks metadata bilgisi
     */
    public IndexMetadata getMetadata() {
        return metadata;
    }

    /**
     * İndeks içerisindeki bütün girdilerin güvenli bir kopyasını döndürür.
     *
     * @return değiştirilemez anahtar-pointer haritası
     */
    public Map<K, List<RecordPointer>> getAllEntries() {
        Map<K, List<RecordPointer>> copiedEntries =
                new LinkedHashMap<>();

        for (Map.Entry<K, List<RecordPointer>> entry
                : entries.entrySet()) {

            copiedEntries.put(
                    entry.getKey(),
                    Collections.unmodifiableList(
                            new ArrayList<>(entry.getValue())
                    )
            );
        }

        return Collections.unmodifiableMap(copiedEntries);
    }

    /**
     * İndeks içerisindeki girdileri düz bir IndexEntry listesi olarak döndürür.
     *
     * @return değiştirilemez indeks girdisi listesi
     */
    public List<IndexEntry<K>> getEntryList() {
        List<IndexEntry<K>> result = new ArrayList<>();

        for (Map.Entry<K, List<RecordPointer>> mapEntry
                : entries.entrySet()) {

            for (RecordPointer pointer : mapEntry.getValue()) {
                result.add(
                        new IndexEntry<>(
                                mapEntry.getKey(),
                                pointer
                        )
                );
            }
        }

        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    /**
     * Metadata bilgisini doğrular.
     */
    private void validateMetadata(IndexMetadata metadata) {
        if (metadata == null) {
            throw new InvalidIndexException(
                    "Index metadata null olamaz."
            );
        }

        if (!metadata.isValid()) {
            throw new InvalidIndexException(
                    "Geçersiz index metadata: " + metadata
            );
        }
    }

    /**
     * İndeks anahtarını doğrular.
     */
    private void validateKey(K key) {
        if (key == null) {
            throw new InvalidIndexException(
                    "Index anahtarı null olamaz."
            );
        }
    }

    /**
     * Kayıt adresini doğrular.
     */
    private void validatePointer(RecordPointer pointer) {
        if (pointer == null || !pointer.isValid()) {
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

        return Objects.equals(metadata, index.metadata)
                && Objects.equals(entries, index.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, entries);
    }
}