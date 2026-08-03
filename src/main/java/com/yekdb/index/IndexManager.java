package com.yekdb.index;

import com.yekdb.index.exception.DuplicateIndexException;
import com.yekdb.index.exception.IndexNotFoundException;
import com.yekdb.index.exception.InvalidIndexException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YEKDB içerisindeki indeksleri yöneten merkez sınıftır.
 *
 * <p>IndexManager; indeks oluşturma, silme, arama, listeleme
 * ve indeks girdileri üzerinde işlem yapma görevlerini üstlenir.</p>
 */
public class IndexManager implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Yeni oluşturulacak indeksler için kimlik üretir.
     */
    private final AtomicLong indexIdGenerator;

    /**
     * İndeks adı ile indeks nesnesi arasındaki ilişkiyi tutar.
     */
    private final Map<String, Index<?>> indexes;

    /**
     * Boş bir IndexManager oluşturur.
     */
    public IndexManager() {
        this.indexIdGenerator = new AtomicLong(1L);
        this.indexes = new LinkedHashMap<>();
    }

    /**
     * Yeni bir indeks oluşturur.
     *
     * @param indexName indeks adı
     * @param databaseName veritabanı adı
     * @param tableName tablo adı
     * @param columnName indekslenecek kolon adı
     * @param indexType indeks türü
     * @param <K> indeks anahtar tipi
     * @return oluşturulan indeks
     */
    public <K extends Comparable<K>> Index<K> createIndex(
            String indexName,
            String databaseName,
            String tableName,
            String columnName,
            IndexType indexType
    ) {
        validateName(indexName, "Index adı");
        validateName(databaseName, "Veritabanı adı");
        validateName(tableName, "Tablo adı");
        validateName(columnName, "Kolon adı");

        if (indexType == null) {
            throw new InvalidIndexException(
                    "Index türü null olamaz."
            );
        }

        if (indexes.containsKey(indexName)) {
            throw new DuplicateIndexException(
                    "Aynı isimde bir index zaten mevcut: " + indexName
            );
        }

        long indexId = indexIdGenerator.getAndIncrement();

        IndexMetadata metadata = new IndexMetadata(
                indexId,
                indexName,
                databaseName,
                tableName,
                columnName,
                indexType
        );

        Index<K> index = new Index<>(metadata);
        indexes.put(indexName, index);

        return index;
    }

    /**
     * Hazır bir metadata nesnesi üzerinden indeks oluşturur.
     *
     * @param metadata indeks metadata bilgisi
     * @param <K> indeks anahtar tipi
     * @return oluşturulan indeks
     */
    public <K extends Comparable<K>> Index<K> createIndex(
            IndexMetadata metadata
    ) {
        if (metadata == null || !metadata.isValid()) {
            throw new InvalidIndexException(
                    "Geçerli bir IndexMetadata sağlanmalıdır."
            );
        }

        String indexName = metadata.getIndexName();

        if (indexes.containsKey(indexName)) {
            throw new DuplicateIndexException(
                    "Aynı isimde bir index zaten mevcut: " + indexName
            );
        }

        Index<K> index = new Index<>(metadata);
        indexes.put(indexName, index);

        indexIdGenerator.updateAndGet(
                current -> Math.max(current, metadata.getIndexId() + 1)
        );

        return index;
    }

    /**
     * Belirtilen indeks adını siler.
     *
     * @param indexName silinecek indeks adı
     * @return silinen indeks
     */
    public Index<?> dropIndex(String indexName) {
        validateName(indexName, "Index adı");

        Index<?> removedIndex = indexes.remove(indexName);

        if (removedIndex == null) {
            throw new IndexNotFoundException(
                    "Silinecek index bulunamadı: " + indexName
            );
        }

        return removedIndex;
    }

    /**
     * Belirtilen indeks nesnesini döndürür.
     *
     * @param indexName indeks adı
     * @return indeks nesnesi
     */
    public Index<?> getIndex(String indexName) {
        validateName(indexName, "Index adı");

        Index<?> index = indexes.get(indexName);

        if (index == null) {
            throw new IndexNotFoundException(
                    "Index bulunamadı: " + indexName
            );
        }

        return index;
    }

    /**
     * Belirtilen anahtar tipinde indeks döndürür.
     *
     * <p>Java generic tür silme nedeniyle çalışma zamanında anahtar tipi
     * doğrulanamaz. Yanlış tip kullanımı işlem sırasında hata oluşturabilir.</p>
     */
    @SuppressWarnings("unchecked")
    public <K extends Comparable<K>> Index<K> getTypedIndex(
            String indexName
    ) {
        return (Index<K>) getIndex(indexName);
    }

    /**
     * Bir indeksin kayıtlı olup olmadığını kontrol eder.
     *
     * @param indexName indeks adı
     * @return indeks varsa true
     */
    public boolean indexExists(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            return false;
        }

        return indexes.containsKey(indexName);
    }

    /**
     * Belirtilen veritabanı ve tabloya ait indeksleri listeler.
     *
     * @param databaseName veritabanı adı
     * @param tableName tablo adı
     * @return tabloya ait değiştirilemez indeks listesi
     */
    public List<Index<?>> getIndexesForTable(
            String databaseName,
            String tableName
    ) {
        validateName(databaseName, "Veritabanı adı");
        validateName(tableName, "Tablo adı");

        List<Index<?>> result = new ArrayList<>();

        for (Index<?> index : indexes.values()) {
            IndexMetadata metadata = index.getMetadata();

            if (metadata.belongsToTable(databaseName, tableName)) {
                result.add(index);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Belirtilen tablo ve kolona ait indeksleri listeler.
     */
    public List<Index<?>> getIndexesForColumn(
            String databaseName,
            String tableName,
            String columnName
    ) {
        validateName(databaseName, "Veritabanı adı");
        validateName(tableName, "Tablo adı");
        validateName(columnName, "Kolon adı");

        List<Index<?>> result = new ArrayList<>();

        for (Index<?> index : indexes.values()) {
            IndexMetadata metadata = index.getMetadata();

            if (metadata.belongsToTable(databaseName, tableName)
                    && metadata.belongsToColumn(columnName)) {
                result.add(index);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Belirtilen indekse yeni anahtar ve pointer ekler.
     */
    public <K extends Comparable<K>> void insertEntry(
            String indexName,
            K key,
            RecordPointer pointer
    ) {
        Index<K> index = getTypedIndex(indexName);
        index.insert(key, pointer);
    }

    /**
     * Belirtilen indeks içerisinde anahtar arar.
     */
    public <K extends Comparable<K>> List<RecordPointer> search(
            String indexName,
            K key
    ) {
        Index<K> index = getTypedIndex(indexName);
        return index.search(key);
    }

    /**
     * Belirtilen indeksten anahtar ve bütün pointer ilişkilerini siler.
     */
    public <K extends Comparable<K>> boolean deleteEntry(
            String indexName,
            K key
    ) {
        Index<K> index = getTypedIndex(indexName);
        return index.remove(key);
    }

    /**
     * Belirtilen anahtara bağlı tek bir pointer'ı siler.
     */
    public <K extends Comparable<K>> boolean deleteEntry(
            String indexName,
            K key,
            RecordPointer pointer
    ) {
        Index<K> index = getTypedIndex(indexName);
        return index.remove(key, pointer);
    }

    /**
     * Bir pointer bilgisini günceller.
     */
    public <K extends Comparable<K>> boolean updateEntry(
            String indexName,
            K key,
            RecordPointer oldPointer,
            RecordPointer newPointer
    ) {
        Index<K> index = getTypedIndex(indexName);

        return index.update(
                key,
                oldPointer,
                newPointer
        );
    }

    /**
     * Tüm indeksleri değiştirilemez bir harita olarak döndürür.
     */
    public Map<String, Index<?>> getAllIndexes() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(indexes)
        );
    }

    /**
     * Sistemde kayıtlı indeks sayısını döndürür.
     */
    public int size() {
        return indexes.size();
    }

    /**
     * Hiç indeks bulunup bulunmadığını kontrol eder.
     */
    public boolean isEmpty() {
        return indexes.isEmpty();
    }

    /**
     * Bütün indeksleri temizler.
     */
    public void clear() {
        indexes.clear();
    }

    /**
     * Belirtilen tabloya ait bütün indeksleri siler.
     *
     * @return silinen indeks sayısı
     */
    public int dropIndexesForTable(
            String databaseName,
            String tableName
    ) {
        validateName(databaseName, "Veritabanı adı");
        validateName(tableName, "Tablo adı");

        List<String> indexNamesToRemove = new ArrayList<>();

        for (Map.Entry<String, Index<?>> entry : indexes.entrySet()) {
            IndexMetadata metadata = entry.getValue().getMetadata();

            if (metadata.belongsToTable(databaseName, tableName)) {
                indexNamesToRemove.add(entry.getKey());
            }
        }

        for (String indexName : indexNamesToRemove) {
            indexes.remove(indexName);
        }

        return indexNamesToRemove.size();
    }

    /**
     * Metin parametrelerini doğrular.
     */
    private void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidIndexException(
                    fieldName + " null veya boş olamaz."
            );
        }
    }

    @Override
    public String toString() {
        return "IndexManager{" +
                "indexCount=" + indexes.size() +
                ", indexNames=" + indexes.keySet() +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof IndexManager that)) {
            return false;
        }

        return Objects.equals(indexes, that.indexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes);
    }
}