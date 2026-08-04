package com.yekdb.query.command;

import java.util.Objects;

/**
 * DROP TABLE SQL komutunu temsil eder.
 */
public final class DropTableCommand implements Command {

    /**
     * Silinecek tablonun adı.
     */
    private final String tableName;

    /**
     * Yeni DROP TABLE komutu oluşturur.
     *
     * @param tableName silinecek tablo adı
     */
    public DropTableCommand(String tableName) {

        this.tableName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (this.tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }
    }

    /**
     * Tablo adını döndürür.
     *
     * @return tablo adı
     */
    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "DropTableCommand{" +
                "tableName='" + tableName + '\'' +
                '}';
    }
}