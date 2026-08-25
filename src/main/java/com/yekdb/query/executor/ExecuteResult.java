package com.yekdb.query.executor;

import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * QueryExecutor tarafından gerçekleştirilen sorguların
 * ortak sonuç modelidir.
 *
 * SELECT sorgularında:
 * - sütun bilgileri,
 * - satırlar
 *
 * DDL / DML işlemlerinde:
 * - sonuç mesajı,
 * - etkilenen satır sayısı
 *
 * taşınır.
 */
public final class ExecuteResult {

    private final boolean success;
    private final String message;
    private final List<Column> columns;
    private final List<Row> rows;
    private final int affectedRows;

    private ExecuteResult(
            boolean success,
            String message,
            List<Column> columns,
            List<Row> rows,
            int affectedRows
    ) {

        if (affectedRows < 0) {
            throw new IllegalArgumentException(
                    "Affected row count cannot be negative."
            );
        }

        this.success = success;

        this.message =
                Objects.requireNonNullElse(
                        message,
                        ""
                );

        this.columns =
                columns == null
                        ? Collections.emptyList()
                        : List.copyOf(columns);

        this.rows =
                rows == null
                        ? Collections.emptyList()
                        : List.copyOf(rows);

        this.affectedRows =
                affectedRows;
    }

    /**
     * Satır döndürmeyen başarılı işlemler.
     */
    public static ExecuteResult success(
            String message
    ) {

        return new ExecuteResult(
                true,
                message,
                Collections.emptyList(),
                Collections.emptyList(),
                0
        );
    }

    /**
     * Etkilenen satır sayısı bulunan işlemler.
     */
    public static ExecuteResult success(
            String message,
            int affectedRows
    ) {

        return new ExecuteResult(
                true,
                message,
                Collections.emptyList(),
                Collections.emptyList(),
                affectedRows
        );
    }

    /**
     * Eski SELECT API.
     *
     * Geriye dönük uyumluluk için korunur.
     */
    public static ExecuteResult success(
            String message,
            List<Row> rows
    ) {

        List<Row> safeRows =
                rows == null
                        ? Collections.emptyList()
                        : rows;

        return new ExecuteResult(
                true,
                message,
                Collections.emptyList(),
                safeRows,
                safeRows.size()
        );
    }

    /**
     * SELECT sonucu için sütun + satır bilgisi
     * taşıyan yeni factory.
     */
    public static ExecuteResult selectSuccess(
            String message,
            List<Column> columns,
            List<Row> rows
    ) {

        List<Column> safeColumns =
                columns == null
                        ? Collections.emptyList()
                        : columns;

        List<Row> safeRows =
                rows == null
                        ? Collections.emptyList()
                        : rows;

        return new ExecuteResult(
                true,
                message,
                safeColumns,
                safeRows,
                safeRows.size()
        );
    }

    /**
     * Başarısız işlem sonucu.
     */
    public static ExecuteResult failure(
            String message
    ) {

        return new ExecuteResult(
                false,
                message,
                Collections.emptyList(),
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

    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public boolean hasColumns() {
        return !columns.isEmpty();
    }

    public boolean hasRows() {
        return !rows.isEmpty();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public int getRowCount() {
        return rows.size();
    }

    @Override
    public String toString() {
        return "ExecuteResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", columnCount=" + getColumnCount() +
                ", rowCount=" + getRowCount() +
                ", affectedRows=" + affectedRows +
                '}';
    }
}