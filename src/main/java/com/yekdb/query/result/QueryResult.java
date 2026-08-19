package com.yekdb.query.result;

import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;

import java.util.List;
import java.util.Objects;

/**
 * Bir sorgu yürütme işleminin sonucunu temsil eder.
 *
 * SELECT sorgularında:
 * - sütunlar
 * - dönen satırlar
 * - etkilenen satır sayısı
 *
 * DELETE veya UPDATE sorgularında:
 * - etkilenen satır sayısı
 * - işlem mesajı
 */

public final class QueryResult {
    private final boolean success;
    private final String message;
    private final List<Column> columns;
    private final List<Row> rows;
    private final int affectedRowCount;
    private final long executionTimeNanos;

    private QueryResult(boolean success,String message,List<Column> columns,List<Row> rows,int affectedRowCount,long executionTimeNanos ){
        this.success=success;
        this.message=message;
        this.columns= columns ==null ? List.of() :List.copyOf(columns);
        this.rows=rows == null ? List.of():List.copyOf(rows);
        if(affectedRowCount<0){
            throw new IllegalArgumentException("The number of affected rows cannot be negative.");
        }
        if (executionTimeNanos <0){
            throw new IllegalArgumentException("Working time cannot be negative.");
        }
        this.affectedRowCount=affectedRowCount;
        this.executionTimeNanos=executionTimeNanos;
    }
    /**
     * SELECT sorguları için başarılı sonuç oluşturur.
     */
    public static QueryResult selectSuccess(
            List<Column> columns,
            List<Row> rows,
            long executionTimeNanos
    ) {
        Objects.requireNonNull(
                columns,
                "The column list cannot be null."
        );

        Objects.requireNonNull(
                rows,
                "The row list cannot be null."
        );

        return new QueryResult(
                true,
                "The interrogation was conducted successfully.",
                columns,
                rows,
                rows.size(),
                executionTimeNanos
        );
    }

    /**
     * INSERT, UPDATE veya DELETE gibi işlemler için sonuç oluşturur.
     */
    public static QueryResult modificationSuccess(
            String message,
            int affectedRowCount,
            long executionTimeNanos
    ) {
        return new QueryResult(
                true,
                normalizeMessage(message),
                List.of(),
                List.of(),
                affectedRowCount,
                executionTimeNanos
        );
    }

    /**
     * Başarısız sorgu sonucu oluşturur.
     */
    public static QueryResult failure(
            String message,
            long executionTimeNanos
    ) {
        return new QueryResult(
                false,
                normalizeMessage(message),
                List.of(),
                List.of(),
                0,
                executionTimeNanos
        );
    }

    private static String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "No query result message found.";
        }

        return message.trim();
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

    public int getAffectedRowCount() {
        return affectedRowCount;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public double getExecutionTimeMillis() {
        return executionTimeNanos / 1_000_000.0;
    }

    public boolean hasRows() {
        return !rows.isEmpty();
    }

    public boolean hasColumns() {
        return !columns.isEmpty();
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", columnCount=" + columns.size() +
                ", rowCount=" + rows.size() +
                ", affectedRowCount=" + affectedRowCount +
                ", executionTimeMillis=" + getExecutionTimeMillis() +
                '}';
    }
}
