package com.yekdb.index;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Bir indeksin metadata bilgilerini temsil eder.
 *
 * Bu sınıf indeksin:
 * - adını,
 * - bağlı olduğu veritabanını,
 * - tabloyu,
 * - kolonu,
 * - indeks türünü,
 * - B+ Tree root page bilgisini
 * tutar.
 */
public class IndexMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Henüz fiziksel root page oluşturulmadığını belirtir.
     */
    public static final int UNASSIGNED_ROOT_PAGE_ID = -1;

    private long indexId;

    private String indexName;

    private String databaseName;

    private String tableName;

    private String columnName;

    private IndexType indexType;

    /**
     * İleride B+ Tree root node'unun tutulacağı sayfa numarası.
     * İlk aşamada -1 değerini alır.
     */
    private int rootPageId;

    private LocalDateTime createdAt;

    /**
     * Serialization ve framework kullanımları için boş constructor.
     */
    public IndexMetadata() {
        this.rootPageId = UNASSIGNED_ROOT_PAGE_ID;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Yeni indeks metadata nesnesi oluşturur.
     *
     * @param indexId indeks kimliği
     * @param indexName indeks adı
     * @param databaseName veritabanı adı
     * @param tableName tablo adı
     * @param columnName indekslenen kolon adı
     * @param indexType indeks türü
     */
    public IndexMetadata(
            long indexId,
            String indexName,
            String databaseName,
            String tableName,
            String columnName,
            IndexType indexType
    ) {
        this(
                indexId,
                indexName,
                databaseName,
                tableName,
                columnName,
                indexType,
                UNASSIGNED_ROOT_PAGE_ID,
                LocalDateTime.now()
        );
    }

    /**
     * Tüm alanları alan constructor.
     */
    public IndexMetadata(
            long indexId,
            String indexName,
            String databaseName,
            String tableName,
            String columnName,
            IndexType indexType,
            int rootPageId,
            LocalDateTime createdAt
    ) {
        this.indexId = indexId;
        this.indexName = indexName;
        this.databaseName = databaseName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.indexType = indexType;
        this.rootPageId = rootPageId;
        this.createdAt = createdAt;
    }

    /**
     * Metadata alanlarının geçerli olup olmadığını kontrol eder.
     *
     * @return metadata geçerliyse true
     */
    public boolean isValid() {
        return indexId >= 0
                && !isBlank(indexName)
                && !isBlank(databaseName)
                && !isBlank(tableName)
                && !isBlank(columnName)
                && indexType != null
                && rootPageId >= UNASSIGNED_ROOT_PAGE_ID
                && createdAt != null;
    }

    /**
     * Root page'in atanıp atanmadığını kontrol eder.
     *
     * @return root page atanmışsa true
     */
    public boolean hasRootPage() {
        return rootPageId >= 0;
    }

    /**
     * İndeksin belirtilen tabloya ait olup olmadığını kontrol eder.
     */
    public boolean belongsToTable(String databaseName, String tableName) {
        return Objects.equals(this.databaseName, databaseName)
                && Objects.equals(this.tableName, tableName);
    }

    /**
     * İndeksin belirtilen kolona ait olup olmadığını kontrol eder.
     */
    public boolean belongsToColumn(String columnName) {
        return Objects.equals(this.columnName, columnName);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public long getIndexId() {
        return indexId;
    }

    public void setIndexId(long indexId) {
        this.indexId = indexId;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(IndexType indexType) {
        this.indexType = indexType;
    }

    public int getRootPageId() {
        return rootPageId;
    }

    public void setRootPageId(int rootPageId) {
        this.rootPageId = rootPageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "IndexMetadata{" +
                "indexId=" + indexId +
                ", indexName='" + indexName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", columnName='" + columnName + '\'' +
                ", indexType=" + indexType +
                ", rootPageId=" + rootPageId +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof IndexMetadata that)) {
            return false;
        }

        return indexId == that.indexId
                && rootPageId == that.rootPageId
                && Objects.equals(indexName, that.indexName)
                && Objects.equals(databaseName, that.databaseName)
                && Objects.equals(tableName, that.tableName)
                && Objects.equals(columnName, that.columnName)
                && indexType == that.indexType
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                indexId,
                indexName,
                databaseName,
                tableName,
                columnName,
                indexType,
                rootPageId,
                createdAt
        );
    }
}