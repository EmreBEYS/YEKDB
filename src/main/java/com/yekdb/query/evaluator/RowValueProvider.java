package com.yekdb.query.evaluator;

import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.Table;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bir Row nesnesindeki değerlere sütun adı üzerinden erişim sağlar.
 *
 * Row değerleri indeks tabanlıdır:
 *
 * row.getValue(0)
 * row.getValue(1)
 *
 * WHERE motoru ise sütun adıyla çalışır:
 *
 * age > 18
 * city = "Malatya"
 *
 * Bu sınıf, sütun adı ile Row indeksi arasındaki bağlantıyı kurar.
 */
public final class RowValueProvider
        implements Function<String, Object> {

    private final Row row;
    private final Map<String, Integer> columnIndexes;

    /**
     * Yeni bir RowValueProvider oluşturur.
     *
     * @param row değerlendirilecek satır
     * @param table satırın ait olduğu tablo şeması
     */
    public RowValueProvider(
            Row row,
            Table table
    ) {
        this.row = Objects.requireNonNull(
                row,
                "Row cannot be null."
        );

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        validateRowSize(row, table);

        this.columnIndexes = createColumnIndexMap(
                table.getColumns()
        );
    }

    /**
     * Sütun adına karşılık gelen Row değerini döndürür.
     *
     * @param columnName sütun adı
     * @return satırdaki sütun değeri
     */
    @Override
    public Object apply(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be null or blank."
            );
        }

        String normalizedColumnName = normalize(columnName);

        Integer columnIndex = columnIndexes.get(
                normalizedColumnName
        );

        if (columnIndex == null) {
            throw new IllegalArgumentException(
                    "Column not found in table: " + columnName
            );
        }

        return row.getValue(columnIndex);
    }

    /**
     * Tablo sütunlarını isim-indeks haritasına dönüştürür.
     */
    private static Map<String, Integer> createColumnIndexMap(
            List<Column> columns
    ) {
        return IntStream.range(0, columns.size())
                .boxed()
                .collect(Collectors.toUnmodifiableMap(
                        index -> normalize(
                                columns.get(index).getName()
                        ),
                        index -> index
                ));
    }

    /**
     * Row değer sayısı ile tablo sütun sayısını karşılaştırır.
     */
    private static void validateRowSize(
            Row row,
            Table table
    ) {
        if (row.size() != table.getColumnCount()) {
            throw new IllegalArgumentException(
                    "The number of row values does not match the number of columns in the table. "
                            + "Row value number: " + row.size()
                            + ", number of table columns: "
                            + table.getColumnCount()
            );
        }
    }

    /**
     * Sütun adını standart biçime getirir.
     */
    private static String normalize(String columnName) {
        return columnName
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}