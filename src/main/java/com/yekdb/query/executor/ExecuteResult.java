package com.yekdb.query.executor;

import com.yekdb.storage.record.Row;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * QueryExecutor tarafından gerçekleştirilen sorguların
 * ortak sonuç modelidir.
 *
 * Bir sorgunun başarılı olup olmadığını, sonuç mesajını,
 * etkilenen kayıt sayısını ve SELECT sorgularında dönen
 * satırları taşır.
 */
public final class ExecuteResult {

    private final boolean success;
    private final String message;
    private final List<Row> rows;
    private final int affectedRows;

    private ExecuteResult(
            boolean success,
            String message,
            List<Row> rows,
            int affectedRows
    ) {
        if (affectedRows < 0) {
            throw new IllegalArgumentException(
                    "Affected row count cannot be negative."
            );
        }

        this.success = success;
        this.message = Objects.requireNonNullElse(
                message,
                ""
        );

        this.rows = rows == null
                ? Collections.emptyList()
                : List.copyOf(rows);

        this.affectedRows = affectedRows;
    }

    /**
     * Satır döndürmeyen başarılı işlemler için sonuç oluşturur.
     */
    public static ExecuteResult success(String message) {
        return new ExecuteResult(
                true,
                message,
                Collections.emptyList(),
                0
        );
    }

    /**
     * Etkilenen kayıt sayısı bulunan başarılı işlemler için
     * sonuç oluşturur.
     */
    public static ExecuteResult success(
            String message,
            int affectedRows
    ) {
        return new ExecuteResult(
                true,
                message,
                Collections.emptyList(),
                affectedRows
        );
    }

    /**
     * SELECT gibi satır döndüren işlemler için sonuç oluşturur.
     */
    public static ExecuteResult success(
            String message,
            List<Row> rows
    ) {
        List<Row> safeRows = rows == null
                ? Collections.emptyList()
                : rows;

        return new ExecuteResult(
                true,
                message,
                safeRows,
                safeRows.size()
        );
    }

    /**
     * Başarısız işlem sonucu oluşturur.
     */
    public static ExecuteResult failure(String message) {
        return new ExecuteResult(
                false,
                message,
                Collections.emptyList(),
                0
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Row> getRows() {
        return rows;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public boolean hasRows() {
        return !rows.isEmpty();
    }

    public int getRowCount() {
        return rows.size();
    }

    @Override
    public String toString() {
        return "ExecuteResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", rowCount=" + getRowCount() +
                ", affectedRows=" + affectedRows +
                '}';
    }
}