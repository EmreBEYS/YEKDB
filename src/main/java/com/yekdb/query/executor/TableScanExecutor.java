package com.yekdb.query.executor;

import com.yekdb.query.evaluator.WhereEvaluator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bir tabloya ait satırları sırayla tarar
 * ve WHERE koşulunu sağlayan satırları döndürür.
 *
 * Bu sınıf şimdilik Full Table Scan gerçekleştirir.
 */
public final class TableScanExecutor {

    private TableScanExecutor() {
    }

    /**
     * WHERE koşulu bulunan tablo taraması gerçekleştirir.
     */
    public static QueryResult execute(
            Table table,
            List<Row> rows,
            Expression whereExpression
    ) {
        Objects.requireNonNull(
                table,
                "Table null olamaz."
        );

        Objects.requireNonNull(
                rows,
                "Row listesi null olamaz."
        );

        long startTime = System.nanoTime();

        List<Row> matchedRows = new ArrayList<>();

        for (Row row : rows) {
            if (row == null) {
                throw new IllegalArgumentException(
                        "Row listesi null satır içeremez."
                );
            }

            if (whereExpression == null
                    || WhereEvaluator.evaluate(
                    whereExpression,
                    row,
                    table
            )) {
                matchedRows.add(row);
            }
        }

        long executionTime =
                System.nanoTime() - startTime;

        return QueryResult.selectSuccess(
                table.getColumns(),
                matchedRows,
                executionTime
        );
    }
}