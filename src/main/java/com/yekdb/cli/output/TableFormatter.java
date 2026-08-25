package com.yekdb.cli.output;

import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SELECT sonuçlarını ASCII tablo biçiminde
 * terminal çıktısına dönüştürür.
 */
public final class TableFormatter {

    private static final String COLUMN_SEPARATOR =
            " | ";

    private static final String EDGE =
            "|";

    private static final char BORDER_CHARACTER =
            '-';

    private static final char BORDER_INTERSECTION =
            '+';

    public TableFormatter() {
    }

    /**
     * Sütun ve satır listesini terminal tablosuna
     * dönüştürür.
     */
    public String format(
            List<Column> columns,
            List<Row> rows
    ) {

        Objects.requireNonNull(
                columns,
                "Column list cannot be null."
        );

        Objects.requireNonNull(
                rows,
                "Row list cannot be null."
        );

        if (columns.isEmpty()) {
            return "";
        }

        int columnCount =
                columns.size();

        validateRows(
                rows,
                columnCount
        );

        List<String> headers =
                extractHeaders(
                        columns
                );

        int[] widths =
                calculateColumnWidths(
                        headers,
                        rows
                );

        String border =
                createBorder(
                        widths
                );

        StringBuilder builder =
                new StringBuilder();

        builder.append(border)
                .append(
                        System.lineSeparator()
                );

        builder.append(
                        createRow(
                                headers,
                                widths
                        )
                )
                .append(
                        System.lineSeparator()
                );

        builder.append(border);

        for (Row row : rows) {

            builder.append(
                    System.lineSeparator()
            );

            builder.append(
                    createRow(
                            stringifyRow(row),
                            widths
                    )
            );
        }

        builder.append(
                System.lineSeparator()
        );

        builder.append(
                border
        );

        return builder.toString();
    }

    /**
     * Column listesinden terminal header
     * isimlerini çıkarır.
     */
    private List<String> extractHeaders(
            List<Column> columns
    ) {

        List<String> headers =
                new ArrayList<>(
                        columns.size()
                );

        for (Column column : columns) {

            headers.add(
                    column.getName()
            );
        }

        return headers;
    }

    /**
     * Her sütun için gerekli minimum terminal
     * genişliğini hesaplar.
     */
    private int[] calculateColumnWidths(
            List<String> headers,
            List<Row> rows
    ) {

        int[] widths =
                new int[
                        headers.size()
                        ];

        /*
         * Önce header uzunlukları.
         */
        for (int i = 0;
             i < headers.size();
             i++) {

            widths[i] =
                    headers
                            .get(i)
                            .length();
        }

        /*
         * Daha sonra row değerleri.
         */
        for (Row row : rows) {

            for (int i = 0;
                 i < widths.length;
                 i++) {

                String value =
                        stringifyValue(
                                row.getValue(i)
                        );

                widths[i] =
                        Math.max(
                                widths[i],
                                value.length()
                        );
            }
        }

        return widths;
    }

    /**
     * ASCII border oluşturur.
     *
     * Örnek:
     *
     * +----+------+-----+
     */
    private String createBorder(
            int[] widths
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                BORDER_INTERSECTION
        );

        for (int width : widths) {

            builder.append(
                    String.valueOf(
                            BORDER_CHARACTER
                    ).repeat(
                            width + 2
                    )
            );

            builder.append(
                    BORDER_INTERSECTION
            );
        }

        return builder.toString();
    }

    /**
     * Header veya veri satırını oluşturur.
     */
    private String createRow(
            List<String> values,
            int[] widths
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                EDGE
        );

        for (int i = 0;
             i < values.size();
             i++) {

            builder.append(" ");

            builder.append(
                    padRight(
                            values.get(i),
                            widths[i]
                    )
            );

            builder.append(" ");

            builder.append(
                    EDGE
            );
        }

        return builder.toString();
    }

    /**
     * Row değerlerini String listesine dönüştürür.
     */
    private List<String> stringifyRow(
            Row row
    ) {

        List<String> values =
                new ArrayList<>(
                        row.size()
                );

        for (Object value
                : row.getValues()) {

            values.add(
                    stringifyValue(
                            value
                    )
            );
        }

        return values;
    }

    private String stringifyValue(
            Object value
    ) {

        if (value == null) {
            return "NULL";
        }

        return String.valueOf(
                value
        );
    }

    /**
     * Hücreyi sütun genişliğine göre
     * sağdan boşlukla tamamlar.
     */
    private String padRight(
            String value,
            int width
    ) {

        return value
                + " ".repeat(
                Math.max(
                        0,
                        width - value.length()
                )
        );
    }

    /**
     * Row / column sayısı uyuşmazlığını
     * erkenden tespit eder.
     */
    private void validateRows(
            List<Row> rows,
            int columnCount
    ) {

        for (Row row : rows) {

            Objects.requireNonNull(
                    row,
                    "Row list cannot contain null values."
            );

            if (row.size()
                    != columnCount) {

                throw new IllegalArgumentException(
                        "Row column count does not match "
                                + "result column count. "
                                + "Expected: "
                                + columnCount
                                + ", actual: "
                                + row.size()
                );
            }
        }
    }
}