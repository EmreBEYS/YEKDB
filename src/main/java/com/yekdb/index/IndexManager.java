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
 * IndexManager:
 * - index oluşturma,
 * - index silme,
 * - index arama,
 * - index listeleme,
 * - index girdileri üzerinde işlem yapma
 *
 * görevlerini üstlenir.
 */
public class IndexManager implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Yeni indeks kimliği üretir.
     */
    private final AtomicLong indexIdGenerator;

    /**
     * Normalize edilmiş index adı ile index nesnesini eşler.
     */
    private final Map<String, Index<?>> indexes;

    public IndexManager() {

        this.indexIdGenerator =
                new AtomicLong(1L);

        this.indexes =
                new LinkedHashMap<>();
    }

    /**
     * Yeni indeks oluşturur.
     */
    public <K extends Comparable<K>> Index<K> createIndex(
            String indexName,
            String databaseName,
            String tableName,
            String columnName,
            IndexType indexType
    ) {

        String normalizedIndexName =
                IndexIdentifierValidator
                        .validateIndexName(indexName);

        String normalizedDatabaseName =
                IndexIdentifierValidator
                        .validateDatabaseName(databaseName);

        String normalizedTableName =
                IndexIdentifierValidator
                        .validateTableName(tableName);

        String normalizedColumnName =
                IndexIdentifierValidator
                        .validateColumnName(columnName);

        if (indexType == null) {

            throw new InvalidIndexException(
                    "Index türü null olamaz."
            );
        }

        if (indexes.containsKey(
                normalizedIndexName
        )) {

            throw new DuplicateIndexException(
                    "Aynı isimde bir index zaten mevcut: "
                            + normalizedIndexName
            );
        }

        long indexId =
                indexIdGenerator
                        .getAndIncrement();

        IndexMetadata metadata =
                new IndexMetadata(
                        indexId,
                        normalizedIndexName,
                        normalizedDatabaseName,
                        normalizedTableName,
                        normalizedColumnName,
                        indexType
                );

        Index<K> index =
                new Index<>(metadata);

        indexes.put(
                normalizedIndexName,
                index
        );

        return index;
    }

    /**
     * Hazır metadata üzerinden indeks oluşturur.
     */
    public <K extends Comparable<K>> Index<K> createIndex(
            IndexMetadata metadata
    ) {

        if (metadata == null
                || !metadata.isValid()) {

            throw new InvalidIndexException(
                    "Geçerli bir IndexMetadata sağlanmalıdır."
            );
        }

        String normalizedIndexName =
                IndexIdentifierValidator
                        .validateIndexName(
                                metadata.getIndexName()
                        );

        if (indexes.containsKey(
                normalizedIndexName
        )) {

            throw new DuplicateIndexException(
                    "Aynı isimde bir index zaten mevcut: "
                            + normalizedIndexName
            );
        }

        Index<K> index =
                new Index<>(metadata);

        indexes.put(
                normalizedIndexName,
                index
        );

        indexIdGenerator.updateAndGet(
                current ->
                        Math.max(
                                current,
                                metadata.getIndexId() + 1
                        )
        );

        return index;
    }

    /**
     * Belirtilen indeksi siler.
     */
    public Index<?> dropIndex(
            String indexName
    ) {

        String normalizedIndexName =
                IndexIdentifierValidator
                        .validateIndexName(indexName);

        Index<?> removedIndex =
                indexes.remove(
                        normalizedIndexName
                );

        if (removedIndex == null) {

            throw new IndexNotFoundException(
                    "Silinecek index bulunamadı: "
                            + normalizedIndexName
            );
        }

        return removedIndex;
    }

    /**
     * Belirtilen indeksi döndürür.
     */
    public Index<?> getIndex(
            String indexName
    ) {

        String normalizedIndexName =
                IndexIdentifierValidator
                        .validateIndexName(indexName);

        Index<?> index =
                indexes.get(
                        normalizedIndexName
                );

        if (index == null) {

            throw new IndexNotFoundException(
                    "Index bulunamadı: "
                            + normalizedIndexName
            );
        }

        return index;
    }

    /**
     * Generic tip ile indeks döndürür.
     */
    @SuppressWarnings("unchecked")
    public <K extends Comparable<K>> Index<K> getTypedIndex(
            String indexName
    ) {

        return (Index<K>)
                getIndex(indexName);
    }

    /**
     * İndeksin mevcut olup olmadığını kontrol eder.
     */
    public boolean indexExists(
            String indexName
    ) {

        if (indexName == null
                || indexName.isBlank()) {

            return false;
        }

        try {

            String normalizedIndexName =
                    IndexIdentifierValidator
                            .validateIndexName(indexName);

            return indexes.containsKey(
                    normalizedIndexName
            );

        } catch (InvalidIndexException exception) {

            return false;
        }
    }

    /**
     * Belirtilen tabloya ait indeksleri listeler.
     */
    public List<Index<?>> getIndexesForTable(
            String databaseName,
            String tableName
    ) {

        String normalizedDatabaseName =
                IndexIdentifierValidator
                        .validateDatabaseName(databaseName);

        String normalizedTableName =
                IndexIdentifierValidator
                        .validateTableName(tableName);

        List<Index<?>> result =
                new ArrayList<>();

        for (Index<?> index
                : indexes.values()) {

            IndexMetadata metadata =
                    index.getMetadata();

            if (metadata.belongsToTable(
                    normalizedDatabaseName,
                    normalizedTableName
            )) {

                result.add(index);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Belirtilen tablo ve kolona ait indeksleri listeler.
     */
    public List<Index<?>> getIndexesForColumn(
            String databaseName,
            String tableName,
            String columnName
    ) {

        String normalizedDatabaseName =
                IndexIdentifierValidator
                        .validateDatabaseName(databaseName);

        String normalizedTableName =
                IndexIdentifierValidator
                        .validateTableName(tableName);

        String normalizedColumnName =
                IndexIdentifierValidator
                        .validateColumnName(columnName);

        List<Index<?>> result =
                new ArrayList<>();

        for (Index<?> index
                : indexes.values()) {

            IndexMetadata metadata =
                    index.getMetadata();

            if (metadata.belongsToTable(
                    normalizedDatabaseName,
                    normalizedTableName
            )
                    && metadata.belongsToColumn(
                    normalizedColumnName
            )) {

                result.add(index);
            }
        }

        return List.copyOf(result);
    }

    /**
     * İndekse yeni kayıt ekler.
     */
    public <K extends Comparable<K>> void insertEntry(
            String indexName,
            K key,
            RecordPointer pointer
    ) {

        Index<K> index =
                getTypedIndex(indexName);

        index.insert(
                key,
                pointer
        );
    }

    /**
     * İndeks içerisinde anahtar arar.
     */
    public <K extends Comparable<K>>
    List<RecordPointer> search(
            String indexName,
            K key
    ) {

        Index<K> index =
                getTypedIndex(indexName);

        return index.search(key);
    }

    /**
     * Anahtarı ve tüm pointer ilişkilerini siler.
     */
    public <K extends Comparable<K>>
    boolean deleteEntry(
            String indexName,
            K key
    ) {

        Index<K> index =
                getTypedIndex(indexName);

        return index.remove(key);
    }

    /**
     * Belirli bir pointer ilişkisini siler.
     */
    public <K extends Comparable<K>>
    boolean deleteEntry(
            String indexName,
            K key,
            RecordPointer pointer
    ) {

        Index<K> index =
                getTypedIndex(indexName);

        return index.remove(
                key,
                pointer
        );
    }

    /**
     * Pointer bilgisini günceller.
     */
    public <K extends Comparable<K>>
    boolean updateEntry(
            String indexName,
            K key,
            RecordPointer oldPointer,
            RecordPointer newPointer
    ) {

        Index<K> index =
                getTypedIndex(indexName);

        return index.update(
                key,
                oldPointer,
                newPointer
        );
    }

    /**
     * Bütün indekslerin değiştirilemez kopyasını döndürür.
     */
    public Map<String, Index<?>> getAllIndexes() {

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        indexes
                )
        );
    }

    public int size() {
        return indexes.size();
    }

    public boolean isEmpty() {
        return indexes.isEmpty();
    }

    /**
     * Tüm indeksleri temizler.
     */
    public void clear() {
        indexes.clear();
    }

    /**
     * Belirtilen tabloya ait bütün indeksleri siler.
     *
     * @return silinen index sayısı
     */
    public int dropIndexesForTable(
            String databaseName,
            String tableName
    ) {

        String normalizedDatabaseName =
                IndexIdentifierValidator
                        .validateDatabaseName(databaseName);

        String normalizedTableName =
                IndexIdentifierValidator
                        .validateTableName(tableName);

        List<String> indexNamesToRemove =
                new ArrayList<>();

        for (Map.Entry<String, Index<?>> entry
                : indexes.entrySet()) {

            IndexMetadata metadata =
                    entry
                            .getValue()
                            .getMetadata();

            if (metadata.belongsToTable(
                    normalizedDatabaseName,
                    normalizedTableName
            )) {

                indexNamesToRemove.add(
                        entry.getKey()
                );
            }
        }

        for (String indexName
                : indexNamesToRemove) {

            indexes.remove(indexName);
        }

        return indexNamesToRemove.size();
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

        return Objects.equals(
                indexes,
                that.indexes
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes);
    }
}