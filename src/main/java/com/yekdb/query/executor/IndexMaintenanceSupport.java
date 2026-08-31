package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.index.RecordPointer;
import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * INSERT / UPDATE / DELETE işlemleri ile B+ Tree index yapılarını
 * senkron tutan ortak yardımcı bileşendir.
 *
 * NULL index key değerleri mevcut B+ Tree null key kabul etmediği için
 * index dışında bırakılır. PRIMARY için NULL kontrolü constraint
 * katmanında gerçekleştirilir.
 */
final class IndexMaintenanceSupport {

    private IndexMaintenanceSupport() {
    }

    /**
     * INSERT fiziksel olarak gerçekleşmeden önce UNIQUE / PRIMARY
     * index ihlallerini doğrular.
     */
    static void validateInsert(
            Table table,
            Row row,
            List<Index<?>> indexes
    ) {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                row,
                "Row cannot be null."
        );

        for (Index<?> index :
                relevantIndexes(
                        table,
                        indexes
                )) {

            Object key =
                    keyFor(
                            table,
                            row,
                            index.getMetadata()
                    );

            /*
             * NULL değerler mevcut B+ Tree key-space içerisine
             * alınmaz.
             */
            if (key == null) {
                continue;
            }

            if (index.getMetadata()
                    .getIndexType()
                    .isUnique()
                    && !search(
                    index,
                    key
            ).isEmpty()) {

                throw duplicate(
                        index,
                        key
                );
            }
        }
    }

    /**
     * Başarılı INSERT sonrasında bütün ilgili index'lere
     * key -> RecordPointer ilişkisini ekler.
     */
    static void applyInsert(
            Table table,
            Row row,
            RecordPointer pointer,
            List<Index<?>> indexes
    ) {

        Objects.requireNonNull(
                pointer,
                "RecordPointer cannot be null."
        );

        for (Index<?> index :
                relevantIndexes(
                        table,
                        indexes
                )) {

            Object key =
                    keyFor(
                            table,
                            row,
                            index.getMetadata()
                    );

            if (key != null) {

                insert(
                        index,
                        key,
                        pointer
                );
            }
        }
    }

    /**
     * UPDATE statement içerisindeki bütün değişiklikleri fiziksel
     * mutation öncesinde UNIQUE / PRIMARY index kurallarına göre
     * doğrular.
     */
    static void validateUpdates(
            Table table,
            List<UpdateChange> changes,
            List<Index<?>> indexes
    ) {

        Objects.requireNonNull(
                changes,
                "Update changes cannot be null."
        );

        List<Index<?>> relevant =
                relevantIndexes(
                        table,
                        indexes
                );

        for (Index<?> index : relevant) {

            if (!index.getMetadata()
                    .getIndexType()
                    .isUnique()) {

                continue;
            }

            Map<RecordPointer, UpdateChange> byOldPointer =
                    new HashMap<>();

            for (UpdateChange change : changes) {

                byOldPointer.put(
                        change.oldPointer(),
                        change
                );
            }

            /*
             * Aynı UPDATE statement'i içerisinde iki farklı row'un
             * aynı UNIQUE key'e taşınmasını engeller.
             */
            Set<Object> statementKeys =
                    new HashSet<>();

            for (UpdateChange change : changes) {

                Object oldKey =
                        keyFor(
                                table,
                                change.oldRow(),
                                index.getMetadata()
                        );

                Object newKey =
                        keyFor(
                                table,
                                change.newRow(),
                                index.getMetadata()
                        );

                /*
                 * Index key değişmediyse yeni bir uniqueness
                 * kontrolüne gerek yoktur.
                 */
                if (newKey == null
                        || Objects.equals(
                        oldKey,
                        newKey
                )) {

                    continue;
                }

                if (!statementKeys.add(
                        newKey
                )) {

                    throw duplicate(
                            index,
                            newKey
                    );
                }

                for (RecordPointer existingPointer :
                        search(
                                index,
                                newKey
                        )) {

                    /*
                     * Aynı fiziksel row zaten bu key'e sahipse
                     * conflict değildir.
                     */
                    if (existingPointer.equals(
                            change.oldPointer()
                    )) {

                        continue;
                    }

                    /*
                     * Existing key başka bir UPDATE candidate tarafından
                     * aynı statement içerisinde boşaltılacaksa kullanımına
                     * izin verilebilir.
                     */
                    UpdateChange existingChange =
                            byOldPointer.get(
                                    existingPointer
                            );

                    if (existingChange != null) {

                        Object existingOldKey =
                                keyFor(
                                        table,
                                        existingChange.oldRow(),
                                        index.getMetadata()
                                );

                        Object existingNewKey =
                                keyFor(
                                        table,
                                        existingChange.newRow(),
                                        index.getMetadata()
                                );

                        if (Objects.equals(
                                existingOldKey,
                                newKey
                        )
                                && !Objects.equals(
                                existingNewKey,
                                newKey
                        )) {

                            continue;
                        }
                    }

                    throw duplicate(
                            index,
                            newKey
                    );
                }
            }
        }
    }

    /**
     * Başarılı fiziksel UPDATE sonrasında index key ve pointer
     * değişikliklerini uygular.
     */
    static void applyUpdate(
            Table table,
            Row oldRow,
            Row newRow,
            RecordPointer oldPointer,
            RecordPointer newPointer,
            List<Index<?>> indexes
    ) {

        Objects.requireNonNull(
                oldPointer,
                "Old RecordPointer cannot be null."
        );

        Objects.requireNonNull(
                newPointer,
                "New RecordPointer cannot be null."
        );

        for (Index<?> index :
                relevantIndexes(
                        table,
                        indexes
                )) {

            IndexMetadata metadata =
                    index.getMetadata();

            Object oldKey =
                    keyFor(
                            table,
                            oldRow,
                            metadata
                    );

            Object newKey =
                    keyFor(
                            table,
                            newRow,
                            metadata
                    );

            /*
             * Indexed değer değişmediyse yalnızca fiziksel
             * RecordPointer taşınmış olabilir.
             */
            if (Objects.equals(
                    oldKey,
                    newKey
            )) {

                if (oldKey != null
                        && !oldPointer.equals(
                        newPointer
                )) {

                    boolean updated =
                            update(
                                    index,
                                    oldKey,
                                    oldPointer,
                                    newPointer
                            );

                    if (!updated) {

                        throw new IllegalStateException(
                                "Index pointer update failed for index: "
                                        + metadata.getIndexName()
                        );
                    }
                }

                continue;
            }

            /*
             * Key değişmişse eski ilişki kaldırılır.
             */
            if (oldKey != null
                    && !remove(
                    index,
                    oldKey,
                    oldPointer
            )) {

                throw new IllegalStateException(
                        "Old index entry was not found for index: "
                                + metadata.getIndexName()
                );
            }

            /*
             * Yeni key NULL değilse yeni ilişki oluşturulur.
             */
            if (newKey != null) {

                insert(
                        index,
                        newKey,
                        newPointer
                );
            }
        }
    }

    /**
     * DELETE edilen row'a ait bütün index ilişkilerini kaldırır.
     */
    static void applyDelete(
            Table table,
            Row row,
            RecordPointer pointer,
            List<Index<?>> indexes
    ) {

        Objects.requireNonNull(
                pointer,
                "RecordPointer cannot be null."
        );

        for (Index<?> index :
                relevantIndexes(
                        table,
                        indexes
                )) {

            Object key =
                    keyFor(
                            table,
                            row,
                            index.getMetadata()
                    );

            if (key == null) {
                continue;
            }

            boolean removed =
                    remove(
                            index,
                            key,
                            pointer
                    );

            if (!removed) {

                throw new IllegalStateException(
                        "Index entry was not found during DELETE for index: "
                                + index.getMetadata()
                                .getIndexName()
                );
            }
        }
    }

    /**
     * Verilen tabloya gerçekten ait index listesini döndürür.
     */
    static List<Index<?>> relevantIndexes(
            Table table,
            List<Index<?>> indexes
    ) {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        if (indexes == null
                || indexes.isEmpty()) {

            return List.of();
        }

        return indexes.stream()
                .filter(Objects::nonNull)
                .filter(index ->
                        index.getMetadata()
                                != null
                )
                .filter(index ->
                        index.getMetadata()
                                .getTableName()
                                != null
                )
                .filter(index ->
                        index.getMetadata()
                                .getTableName()
                                .equalsIgnoreCase(
                                        table.getTableName()
                                )
                )
                .toList();
    }

    /**
     * Index metadata içerisindeki column adına karşılık gelen
     * row değerini bulur.
     */
    private static Object keyFor(
            Table table,
            Row row,
            IndexMetadata metadata
    ) {

        String columnName =
                metadata.getColumnName();

        List<Column> columns =
                table.getColumns();

        for (int i = 0;
             i < columns.size();
             i++) {

            if (columns.get(i)
                    .getName()
                    .equalsIgnoreCase(
                            columnName
                    )) {

                Object key =
                        row.getValue(i);

                if (key != null
                        && !(key
                        instanceof Comparable<?>)) {

                    throw new IllegalArgumentException(
                            "Indexed column value must implement Comparable. Column: "
                                    + columnName
                    );
                }

                return key;
            }
        }

        throw new IllegalArgumentException(
                "Indexed column was not found in table: "
                        + columnName
        );
    }

    private static DuplicateIndexKeyException duplicate(
            Index<?> index,
            Object key
    ) {

        return new DuplicateIndexKeyException(
                "Duplicate index key is not allowed. Index: "
                        + index.getMetadata()
                        .getIndexName()
                        + ", key: "
                        + key
        );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static List<RecordPointer> search(
            Index<?> index,
            Object key
    ) {

        return ((Index) index)
                .search(
                        (Comparable) key
                );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static void insert(
            Index<?> index,
            Object key,
            RecordPointer pointer
    ) {

        ((Index) index)
                .insert(
                        (Comparable) key,
                        pointer
                );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static boolean remove(
            Index<?> index,
            Object key,
            RecordPointer pointer
    ) {

        return ((Index) index)
                .remove(
                        (Comparable) key,
                        pointer
                );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static boolean update(
            Index<?> index,
            Object key,
            RecordPointer oldPointer,
            RecordPointer newPointer
    ) {

        return ((Index) index)
                .update(
                        (Comparable) key,
                        oldPointer,
                        newPointer
                );
    }

    /**
     * UPDATE başlamadan önce ihtiyaç duyulan immutable
     * index mutation snapshot'ıdır.
     */
    record UpdateChange(
            long recordId,
            Row oldRow,
            Row newRow,
            RecordPointer oldPointer
    ) {

        UpdateChange {

            Objects.requireNonNull(
                    oldRow,
                    "Old row cannot be null."
            );

            Objects.requireNonNull(
                    newRow,
                    "New row cannot be null."
            );

            Objects.requireNonNull(
                    oldPointer,
                    "Old RecordPointer cannot be null."
            );
        }
    }
}