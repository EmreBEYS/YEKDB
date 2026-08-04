package com.yekdb.query.command;

import com.yekdb.storage.record.Row;

import java.util.Objects;

/**
 * INSERT INTO SQL komutunu temsil eder.
 *
 * <p>Komut, hedef tablo adını ve tabloya eklenecek
 * satır verisini taşır.</p>
 */
public final class InsertCommand implements Command {

    /**
     * Kaydın ekleneceği tablo adı.
     */
    private final String tableName;

    /**
     * Eklenecek satır verisi.
     */
    private final Row row;

    /**
     * Yeni INSERT komutu oluşturur.
     *
     * @param tableName hedef tablo adı
     * @param row       eklenecek satır
     */
    public InsertCommand(
            String tableName,
            Row row
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

        this.row = Objects.requireNonNull(
                row,
                "Row cannot be null."
        );

        if (row.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inserted row cannot be empty."
            );
        }
    }

    /**
     * Hedef tablo adını döndürür.
     *
     * @return tablo adı
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Eklenecek satırı döndürür.
     *
     * @return satır verisi
     */
    public Row getRow() {
        return row;
    }

    /**
     * Satırdaki değer sayısını döndürür.
     *
     * @return değer sayısı
     */
    public int getValueCount() {
        return row.size();
    }

    @Override
    public String toString() {
        return "InsertCommand{" +
                "tableName='" + tableName + '\'' +
                ", row=" + row +
                '}';
    }
}