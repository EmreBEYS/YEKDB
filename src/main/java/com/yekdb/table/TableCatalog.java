package com.yekdb.table;

import com.yekdb.table.exception.TableAlreadyExistsException;
import com.yekdb.table.exception.TableNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YEKDB içerisindeki tablo kataloğunu yönetir.
 *
 * TableCatalog, aktif bir veritabanındaki tabloların
 * şema ve metadata bilgilerini bellekte tutar.
 *
 * Tablolar eklenme sırasına göre saklanır.
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
     * Kataloğa yeni bir tablo ve metadata bilgisi ekler.
     *
     * @param table    eklenecek tablo
     * @param metadata tablo metadata bilgisi
     * @throws IllegalArgumentException      tablo veya metadata null ise
     * @throws TableAlreadyExistsException   aynı isimde tablo zaten varsa
     */
    public void registerTable(Table table, TableMetadata metadata) {

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

        String tableName = normalizeTableName(table.getTableName());

        if (!tableName.equals(
                normalizeTableName(metadata.getTableName())
        )) {
            throw new IllegalArgumentException(
                    "Table name and metadata table name must match."
            );
        }

        if (table.getColumnCount() != metadata.getColumnCount()) {
            throw new IllegalArgumentException(
                    "Table column count and metadata column count must match."
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
     * Verilen isimdeki tabloyu katalogdan kaldırır.
     *
     * @param tableName tablo adı
     * @return kaldırılan tablo
     * @throws TableNotFoundException tablo bulunamazsa
     */
    public Table unregisterTable(String tableName) {

        String normalizedName = normalizeTableName(tableName);

        Table removedTable = tables.remove(normalizedName);

        if (removedTable == null) {
            throw new TableNotFoundException(
                    "Table not found: " + normalizedName
            );
        }

        metadataEntries.remove(normalizedName);

        return removedTable;
    }

    /**
     * Verilen isimdeki tabloyu döndürür.
     *
     * @param tableName tablo adı
     * @return tablo
     * @throws TableNotFoundException tablo bulunamazsa
     */
    public Table getTable(String tableName) {

        String normalizedName = normalizeTableName(tableName);

        Table table = tables.get(normalizedName);

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
     * @return tablo metadata bilgisi
     * @throws TableNotFoundException tablo bulunamazsa
     */
    public TableMetadata getMetadata(String tableName) {

        String normalizedName = normalizeTableName(tableName);

        TableMetadata metadata = metadataEntries.get(normalizedName);

        if (metadata == null) {
            throw new TableNotFoundException(
                    "Table metadata not found: " + normalizedName
            );
        }

        return metadata;
    }

    /**
     * Verilen isimde bir tablo bulunup bulunmadığını kontrol eder.
     *
     * @param tableName tablo adı
     * @return tablo varsa true
     */
    public boolean containsTable(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            return false;
        }

        return tables.containsKey(normalizeTableName(tableName));
    }

    /**
     * Katalogdaki tabloların listesini döndürür.
     *
     * @return değiştirilemez tablo listesi
     */
    public List<Table> listTables() {
        return Collections.unmodifiableList(
                new ArrayList<>(tables.values())
        );
    }

    /**
     * Katalogdaki tablo adlarını döndürür.
     *
     * @return değiştirilemez tablo adı listesi
     */
    public List<String> listTableNames() {
        return Collections.unmodifiableList(
                new ArrayList<>(tables.keySet())
        );
    }

    /**
     * Katalogdaki metadata bilgilerinin listesini döndürür.
     *
     * @return değiştirilemez metadata listesi
     */
    public List<TableMetadata> listMetadata() {
        return Collections.unmodifiableList(
                new ArrayList<>(metadataEntries.values())
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
     * Katalogdaki bütün tablo ve metadata bilgilerini temizler.
     */
    public void clear() {
        tables.clear();
        metadataEntries.clear();
    }

    /**
     * Tablo adını standart formata dönüştürür.
     *
     * @param tableName tablo adı
     * @return normalize edilmiş tablo adı
     */
    private String normalizeTableName(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        return tableName.trim().toLowerCase();
    }
}