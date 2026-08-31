package com.yekdb.query.optimizer;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.storage.table.Table;

import java.util.List;
import java.util.Objects;

/**
 * Sorgular için temel yürütme planı oluşturur.
 *
 * Phase 13 ile birlikte uygun B+ Tree index bulunduğunda
 * equality ve range predicate'leri INDEX_SCAN planına dönüştürülebilir.
 */
public final class QueryOptimizer {

    /**
     * Varsayılan QueryOptimizer oluşturur.
     */
    public QueryOptimizer() {
    }

    /**
     * Eski optimizer API.
     *
     * Index listesi sağlanmadığı için geriye dönük uyumluluk amacıyla
     * Full Table Scan üretmeye devam eder.
     */
    public QueryPlan optimize(
            Table table,
            Expression whereExpression
    ) {

        return optimize(
                table,
                whereExpression,
                List.of()
        );
    }

    /**
     * Verilen sorgu ve kullanılabilir index listesi için execution planı üretir.
     */
    public QueryPlan optimize(
            Table table,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                availableIndexes,
                "Available index list cannot be null."
        );

        if (whereExpression == null) {

            return new QueryPlan(
                    QueryPlanType.FULL_TABLE_SCAN,
                    null,
                    null,
                    "No WHERE condition exists, so the table will be scanned fully."
            );
        }

        String indexedColumn =
                findIndexableColumn(
                        whereExpression
                );

        if (indexedColumn == null) {

            return new QueryPlan(
                    QueryPlanType.FULL_TABLE_SCAN,
                    whereExpression,
                    null,
                    "WHERE expression is not eligible for a B+ Tree index access path."
            );
        }

        Index<?> index =
                findMatchingIndex(
                        table,
                        indexedColumn,
                        availableIndexes
                );

        if (index == null) {

            return new QueryPlan(
                    QueryPlanType.FULL_TABLE_SCAN,
                    whereExpression,
                    null,
                    "No compatible index exists for column '"
                            + indexedColumn
                            + "'."
            );
        }

        return new QueryPlan(
                QueryPlanType.INDEX_SCAN,
                whereExpression,
                index.getMetadata()
                        .getIndexName(),
                "B+ Tree index '"
                        + index.getMetadata()
                        .getIndexName()
                        + "' will be used for column '"
                        + indexedColumn
                        + "'."
        );
    }

    /**
     * Expression doğrudan B+ Tree access predicate ise kolon adını döndürür.
     */
    private String findIndexableColumn(
            Expression expression
    ) {

        if (expression
                instanceof ComparisonExpression comparison) {

            if (!comparison.isColumnToValueComparison()
                    || comparison.operator()
                    == ComparisonOperator.NOT_EQUALS
                    || !(comparison.expectedValue()
                    instanceof Comparable<?>)) {

                return null;
            }

            return comparison
                    .getLeftColumnExpression()
                    .getColumnName();
        }

        if (expression
                instanceof BetweenExpression between) {

            if (between.isNegated()
                    || !(between.getLowerBound()
                    instanceof Comparable<?>)
                    || !(between.getUpperBound()
                    instanceof Comparable<?>)) {

                return null;
            }

            return ColumnExpression
                    .parse(
                            between.getColumnName()
                    )
                    .getColumnName();
        }

        return null;
    }

    /**
     * Tablo ve kolon metadata'sı ile eşleşen ilk index'i seçer.
     */
    private Index<?> findMatchingIndex(
            Table table,
            String columnName,
            List<Index<?>> availableIndexes
    ) {

        for (Index<?> index : availableIndexes) {

            if (index == null) {
                continue;
            }

            IndexMetadata metadata =
                    index.getMetadata();

            if (metadata == null
                    || !metadata.isValid()) {
                continue;
            }

            if (metadata.getTableName()
                    .equalsIgnoreCase(
                            table.getTableName()
                    )
                    && metadata.belongsToColumn(
                    columnName
            )) {

                return index;
            }
        }

        return null;
    }
}
