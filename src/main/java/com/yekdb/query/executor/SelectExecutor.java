package com.yekdb.query.executor;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.optimizer.QueryPlan;
import com.yekdb.query.optimizer.QueryPlanType;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.List;
import java.util.Objects;

/**
 * SELECT sorgularını yürütür.
 *
 * QueryOptimizer tarafından oluşturulan yürütme planını alır
 * ve uygun executor bileşenine yönlendirir.
 *
 * Sprint 00-11 kapsamında desteklenen plan:
 *
 * - FULL_TABLE_SCAN
 *
 * INDEX_SCAN desteği ilerleyen sprintlerde eklenecektir.
 */
public final class SelectExecutor {

    private final QueryOptimizer queryOptimizer;

    /**
     * Varsayılan QueryOptimizer ile SelectExecutor oluşturur.
     */
    public SelectExecutor() {
        this(new QueryOptimizer());
    }

    /**
     * Dışarıdan verilen QueryOptimizer ile SelectExecutor oluşturur.
     *
     * Bu constructor özellikle testlerde faydalıdır.
     *
     * @param queryOptimizer kullanılacak sorgu optimizer'ı
     */
    public SelectExecutor(QueryOptimizer queryOptimizer) {
        this.queryOptimizer = Objects.requireNonNull(
                queryOptimizer,
                "QueryOptimizer cannot be  null ."
        );
    }

    /**
     * SELECT sorgusunu yürütür.
     *
     * @param table sorgulanacak tablo
     * @param rows tabloya ait satırlar
     * @param whereExpression WHERE koşulu; null ise tüm satırlar döner
     * @return sorgu sonucu
     */
    public QueryResult execute(
            Table table,
            List<Row> rows,
            Expression whereExpression
    ) {
        Objects.requireNonNull(
                table,
                "Table cannot be  null."
        );

        Objects.requireNonNull(
                rows,
                "Row list cannot be null."
        );

        QueryPlan queryPlan = queryOptimizer.optimize(
                table,
                whereExpression
        );

        return executePlan(
                queryPlan,
                table,
                rows
        );
    }

    /**
     * QueryOptimizer tarafından oluşturulan planı çalıştırır.
     */
    private QueryResult executePlan(
            QueryPlan queryPlan,
            Table table,
            List<Row> rows
    ) {
        return switch (queryPlan.getPlanType()) {
            case FULL_TABLE_SCAN ->
                    TableScanExecutor.execute(
                            table,
                            rows,
                            queryPlan.getWhereExpression()
                    );

            case INDEX_SCAN ->
                    throw new UnsupportedOperationException(
                            "INDEX_SCAN execution support has not yet been implemented."
                    );
        };
    }
}