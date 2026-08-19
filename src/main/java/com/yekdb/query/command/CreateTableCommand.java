package com.yekdb.query.command;

import com.yekdb.storage.table.Column;

import java.util.List;
import java.util.Objects;

/**
 * CREATE TABLE SQL komutunu temsil eder.
 */
public final class CreateTableCommand implements Command {

    /**
     * Oluşturulacak tablonun adı.
     */
    private final String tableName;

    /**
     * Tablo sütunları.
     */
    private final List<Column> columns;

    /**
     * Yeni CREATE TABLE komutu oluşturur.
     *
     * @param tableName tablo adı
     * @param columns tablo sütunları
     */
    public CreateTableCommand(
            String tableName,
            List<Column> columns
    ) {

        this.tableName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (this.tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        Objects.requireNonNull(
                columns,
                "Column list cannot be null."
        );

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Table must contain at least one column."
            );
        }

        this.columns = List.copyOf(columns);
    }

    /**
     * Tablo adını döndürür.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Tablo sütunlarını döndürür.
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Sütun sayısını döndürür.
     */
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String toString() {
        return "CreateTableCommand{" +
                "tableName='" + tableName + '\'' +
                ", columnCount=" + columns.size() +
                '}';
    }
}