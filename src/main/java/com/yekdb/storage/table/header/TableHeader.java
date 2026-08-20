package com.yekdb.storage.table.header;

import java.util.Objects;


/**
 * Bir tablonun disk üzerinde saklanan temel metadata bilgisini temsil eder.
 *
 * <p>Bu sınıf Binary Table Header formatının mantıksal Java modelidir.
 * Binary serileştirme işlemleri {@link TableHeaderSerializer}, fiziksel
 * dosya erişimi ise {@link TableHeaderIO} tarafından yönetilir.</p>
 */
public final class TableHeader {

    private final long tableId;
    private final String tableName;
    private final int columnCount;
    private final long rowCount;
    private final long firstDataPageId;
    private final long lastDataPageId;
    private final long schemaOffset;
    private final int flags;

    public TableHeader(
            long tableId,
            String tableName,
            int columnCount,
            long rowCount,
            long firstDataPageId,
            long lastDataPageId,
            long schemaOffset,
            int flags
    ) {
        this.tableId = tableId;
        this.tableName = tableName;
        this.columnCount = columnCount;
        this.rowCount = rowCount;
        this.firstDataPageId = firstDataPageId;
        this.lastDataPageId = lastDataPageId;
        this.schemaOffset = schemaOffset;
        this.flags = flags;
    }

    public long getTableId() { return tableId; }
    public String getTableName() { return tableName; }
    public int getColumnCount() { return columnCount; }
    public long getRowCount() { return rowCount; }
    public long getFirstDataPageId() { return firstDataPageId; }
    public long getLastDataPageId() { return lastDataPageId; }
    public long getSchemaOffset() { return schemaOffset; }
    public int getFlags() { return flags; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TableHeader other)) return false;
        return tableId == other.tableId
                && columnCount == other.columnCount
                && rowCount == other.rowCount
                && firstDataPageId == other.firstDataPageId
                && lastDataPageId == other.lastDataPageId
                && schemaOffset == other.schemaOffset
                && flags == other.flags
                && Objects.equals(tableName, other.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableId, tableName, columnCount, rowCount,
                firstDataPageId, lastDataPageId, schemaOffset, flags);
    }

    @Override
    public String toString() {
        return "TableHeader{" +
                "tableId=" + tableId +
                ", tableName='" + tableName + '\'' +
                ", columnCount=" + columnCount +
                ", rowCount=" + rowCount +
                ", firstDataPageId=" + firstDataPageId +
                ", lastDataPageId=" + lastDataPageId +
                ", schemaOffset=" + schemaOffset +
                ", flags=" + flags +
                '}';
    }
}
