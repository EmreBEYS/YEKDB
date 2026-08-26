package com.yekdb.constraint;

import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Table;

import java.util.Objects;

/**
 * Constraint validation sırasında ihtiyaç duyulan
 * ortak bilgileri taşır.
 *
 * Sprint 00-24 kapsamında:
 *
 * - Table
 * - Row
 * - RecordManager
 * - ignoredRecordId
 *
 * bilgilerini taşımaktadır.
 *
 * ignoredRecordId özellikle UPDATE sırasında
 * UNIQUE kontrolünün mevcut kaydı kendi kendisiyle
 * karşılaştırmasını engellemek için kullanılır.
 */
public final class ValidationContext {

    private final Table table;

    private final Row row;

    private final RecordManager recordManager;

    private final Long ignoredRecordId;

    /**
     * Sadece Row-level validation gereken durumlar için.
     *
     * Örneğin NOT NULL validation RecordManager
     * gerektirmez.
     */
    public ValidationContext(
            Table table,
            Row row
    ) {

        this(
                table,
                row,
                null,
                null
        );
    }

    /**
     * INSERT validation için.
     *
     * UNIQUE gibi mevcut kayıtların taranmasını
     * gerektiren constraint'lerde RecordManager taşınır.
     */
    public ValidationContext(
            Table table,
            Row row,
            RecordManager recordManager
    ) {

        this(
                table,
                row,
                recordManager,
                null
        );
    }

    /**
     * UPDATE validation için.
     *
     * ignoredRecordId:
     *
     * Güncellenmekte olan mevcut record'un UNIQUE
     * karşılaştırmasından çıkarılmasını sağlar.
     */
    public ValidationContext(
            Table table,
            Row row,
            RecordManager recordManager,
            Long ignoredRecordId
    ) {

        this.table =
                Objects.requireNonNull(
                        table,
                        "table cannot be null"
                );

        this.row =
                Objects.requireNonNull(
                        row,
                        "row cannot be null"
                );

        this.recordManager =
                recordManager;

        this.ignoredRecordId =
                ignoredRecordId;
    }

    public Table table() {

        return table;
    }

    public Row row() {

        return row;
    }

    public RecordManager recordManager() {

        return recordManager;
    }

    public Long ignoredRecordId() {

        return ignoredRecordId;
    }

    public boolean hasRecordManager() {

        return recordManager != null;
    }

    /**
     * UPDATE sırasında UNIQUE validation yapılırken
     * mevcut record'un kendisini duplicate olarak
     * görmesini engeller.
     *
     * Boxed Long karşılaştırması yerine primitive
     * long karşılaştırması kullanılır.
     */
    public boolean shouldIgnoreRecord(
            long recordId
    ) {

        return ignoredRecordId != null
                && ignoredRecordId.longValue() == recordId;
    }

    @Override
    public String toString() {

        return "ValidationContext{" +
                "table=" + table.getTableName() +
                ", row=" + row +
                ", hasRecordManager=" +
                (recordManager != null) +
                ", ignoredRecordId=" +
                ignoredRecordId +
                '}';
    }
}