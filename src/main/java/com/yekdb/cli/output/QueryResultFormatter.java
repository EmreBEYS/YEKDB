package com.yekdb.cli.output;

import com.yekdb.query.executor.ExecuteResult;

import java.util.Objects;

/**
 * QueryExecutor tarafından döndürülen ExecuteResult
 * nesnesini terminal çıktısına dönüştürür.
 */
public final class QueryResultFormatter {

    private final TableFormatter tableFormatter;

    public QueryResultFormatter() {
        this(
                new TableFormatter()
        );
    }

    public QueryResultFormatter(
            TableFormatter tableFormatter
    ) {

        this.tableFormatter =
                Objects.requireNonNull(
                        tableFormatter,
                        "TableFormatter cannot be null."
                );
    }

    /**
     * ExecuteResult nesnesini terminalde
     * gösterilecek metne dönüştürür.
     */
    public String format(
            ExecuteResult result
    ) {

        Objects.requireNonNull(
                result,
                "ExecuteResult cannot be null."
        );

        /*
         * SELECT sonucu.
         */
        if (result.hasColumns()) {

            return formatSelectResult(
                    result
            );
        }

        /*
         * DML sonucu.
         */
        if (result.getAffectedRows() > 0) {

            return formatMutationResult(
                    result
            );
        }

        /*
         * DDL / management sonucu.
         */
        if (!result.getMessage().isBlank()) {

            return result.getMessage();
        }

        /*
         * Genel fallback.
         */
        if (result.isSuccess()) {

            return "Command executed successfully.";
        }

        return "Command execution failed.";
    }

    /**
     * SELECT sonucunu tablo olarak formatlar.
     */
    private String formatSelectResult(
            ExecuteResult result
    ) {

        StringBuilder output =
                new StringBuilder();

        if (!result.getMessage().isBlank()) {

            output.append(
                    result.getMessage()
            );

            output.append(
                    System.lineSeparator()
            );
        }

        String table =
                tableFormatter.format(
                        result.getColumns(),
                        result.getRows()
                );

        if (!table.isBlank()) {

            output.append(
                    table
            );
        }

        output.append(
                System.lineSeparator()
        );

        output.append(
                result.getRowCount()
        );

        output.append(
                " row(s)"
        );

        return output.toString();
    }

    /**
     * INSERT / UPDATE / DELETE sonucunu
     * standart terminal çıktısına dönüştürür.
     */
    private String formatMutationResult(
            ExecuteResult result
    ) {

        String message =
                result.getMessage();

        /*
         * Mutation tipi mevcut mesajdan
         * güvenli biçimde belirlenir.
         *
         * Phase 8 içerisinde ExecuteResult'a
         * operation type eklemeden mevcut
         * modeli koruyoruz.
         */
        String upperMessage =
                message.toUpperCase();

        if (upperMessage.startsWith("ROW INSERTED")) {

            return "INSERT "
                    + result.getAffectedRows()
                    + System.lineSeparator()
                    + message;
        }

        if (upperMessage.startsWith("UPDATE")) {

            return "UPDATE "
                    + result.getAffectedRows();
        }

        if (upperMessage.startsWith("DELETE")) {

            return "DELETE "
                    + result.getAffectedRows();
        }

        /*
         * Tanınmayan mutation durumunda
         * mevcut mesaj kaybolmaz.
         */
        if (!message.isBlank()) {

            return message;
        }

        return "Affected rows: "
                + result.getAffectedRows();
    }

    public TableFormatter getTableFormatter() {
        return tableFormatter;
    }
}