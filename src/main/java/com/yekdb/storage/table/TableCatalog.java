package com.yekdb.storage.table;

import com.yekdb.storage.exception.TableAlreadyExistsException;
import com.yekdb.storage.exception.TableNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YEKDB içerisindeki tablo kataloğunu yönetir.
 *
 * TableCatalog aktif veritabanındaki tabloların
 * şema ve metadata bilgilerini bellekte tutar.
 *
 * LinkedHashMap kullanıldığı için tablolar kayıt
 * sırasına göre saklanır.
 *
 * Sürüm: 1.0
 */
public class TableCatalog {

    private final Map<String, Table> tables;
    private final Map<String, TableMetadata> metadataEntries;

    /**
     * Boş bir tablo kataloğu oluşturur.
     */
    public TableCatalog() {
        this.tables = new LinkedHashMap<>();
        this.metadataEntries = new LinkedHashMap<>();
    }

    /**
     * Kataloğa tablo ve metadata bilgisini kaydeder.
     *
     * @param table    tablo
     * @param metadata tablo metadata bilgisi
     */
    public void registerTable(
            Table table,
            TableMetadata metadata
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Table metadata cannot be null."
            );
        }

        String tableName =
                TableNameValidator.validate(
                        table.getTableName()
                );

        String metadataTableName =
                TableNameValidator.validate(
                        metadata.getTableName()
                );

        if (!tableName.equals(metadataTableName)) {
            throw new IllegalArgumentException(
                    "Table name and metadata table name must match."
            );
        }

        if (table.getColumnCount()
                != metadata.getColumnCount()) {

            throw new IllegalArgumentException(
                    "Table column count and metadata "
                            + "column count must match."
            );
        }

        if (tables.containsKey(tableName)) {
            throw new TableAlreadyExistsException(
                    "Table already exists: " + tableName
            );
        }

        tables.put(tableName, table);
        metadataEntries.put(tableName, metadata);
    }

    /**
     * Verilen tabloyu katalogdan kaldırır.
     *
     * @param tableName tablo adı
     * @return kaldırılan tablo
     */
    public Table unregisterTable(String tableName) {

        String normalizedName =
                TableNameValidator.validate(tableName);

        Table removedTable =
                tables.remove(normalizedName);

        if (removedTable == null) {
            throw new TableNotFoundException(
                    "Table not found: " + normalizedName
            );
        }

        metadataEntries.remove(normalizedName);

        return removedTable;
    }

    /**
     * Verilen tabloyu döndürür.
     *
     * @param tableName tablo adı
     * @return tablo
     */
    public Table getTable(String tableName) {

        String normalizedName =
                TableNameValidator.validate(tableName);

        Table table =
                tables.get(normalizedName);

        if (table == null) {
            throw new TableNotFoundException(
                    "Table not found: " + normalizedName
            );
        }

        return table;
    }

    /**
     * Verilen tabloya ait metadata bilgisini döndürür.
     *
     * @param tableName tablo adı
     * @return metadata
     */
    public TableMetadata getMetadata(String tableName) {

        String normalizedName =
                TableNameValidator.validate(tableName);

        TableMetadata metadata =
                metadataEntries.get(normalizedName);

        if (metadata == null) {
            throw new TableNotFoundException(
                    "Table metadata not found: "
                            + normalizedName
            );
        }

        return metadata;
    }

    /**
     * Verilen isimde tablo bulunup bulunmadığını kontrol eder.
     *
     * @param tableName tablo adı
     * @return tablo varsa true
     */
    public boolean containsTable(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            return false;
        }

        try {
            return tables.containsKey(
                    TableNameValidator.validate(tableName)
            );

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Katalogdaki tabloları kayıt sırasına göre döndürür.
     *
     * @return değiştirilemez tablo listesi
     */
    public List<Table> listTables() {
        return List.copyOf(
                tables.values()
        );
    }

    /**
     * Katalogdaki tablo adlarını kayıt sırasına göre döndürür.
     *
     * @return değiştirilemez tablo adı listesi
     */
    public List<String> listTableNames() {
        return List.copyOf(
                tables.keySet()
        );
    }

    /**
     * Katalogdaki metadata bilgilerini kayıt sırasına göre döndürür.
     *
     * @return değiştirilemez metadata listesi
     */
    public List<TableMetadata> listMetadata() {
        return List.copyOf(
                metadataEntries.values()
        );
    }

    /**
     * Katalogdaki tablo sayısını döndürür.
     *
     * @return tablo sayısı
     */
    public int size() {
        return tables.size();
    }

    /**
     * Kataloğun boş olup olmadığını kontrol eder.
     *
     * @return katalog boşsa true
     */
    public boolean isEmpty() {
        return tables.isEmpty();
    }

    /**
     * Tüm tablo ve metadata kayıtlarını temizler.
     */
    public void clear() {
        tables.clear();
        metadataEntries.clear();
    }
}