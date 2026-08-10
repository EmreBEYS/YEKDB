package com.yekdb.query.executor;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.optimizer.QueryPlan;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.List;
import java.util.Objects;

/**
 * SELECT sorgularını yürütür.
 *
 * Sprint 00-13:
 *
 * - ComparisonExpression
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 *
 * desteğine sahip Expression ağacını query execution
 * katmanına taşır.
 *
 * Şu anda desteklenen execution plan:
 *
 * - FULL_TABLE_SCAN
 *
 * INDEX_SCAN desteği sonraki sprintlerde eklenecektir.
 */
public final class SelectExecutor {

    private final QueryOptimizer queryOptimizer;

    /**
     * Varsayılan QueryOptimizer ile executor oluşturur.
     */
    public SelectExecutor() {

        this(
                new QueryOptimizer()
        );
    }

    /**
     * Testlerde veya özel kullanımlarda
     * QueryOptimizer dışarıdan verilebilir.
     */
    public SelectExecutor(
            QueryOptimizer queryOptimizer
    ) {

        this.queryOptimizer =
                Objects.requireNonNull(
                        queryOptimizer,
                        "QueryOptimizer cannot be null."
                );
    }

    /**
     * SELECT sorgusunu yürütür.
     *
     * WHERE yoksa whereExpression null olabilir.
     */
    public QueryResult execute(
            Table table,
            List<Row> rows,
            Expression whereExpression
    ) {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                rows,
                "Row list cannot be null."
        );

        QueryPlan queryPlan =
                queryOptimizer.optimize(
                        table,
                        whereExpression
                );

        Objects.requireNonNull(
                queryPlan,
                "QueryOptimizer cannot return null QueryPlan."
        );

        return executePlan(
                queryPlan,
                table,
                rows
        );
    }

    /**
     * QueryOptimizer tarafından üretilen
     * execution planını çalıştırır.
     */
    private QueryResult executePlan(
            QueryPlan queryPlan,
            Table table,
            List<Row> rows
    ) {

        return switch (
                queryPlan.getPlanType()
                ) {

            case FULL_TABLE_SCAN ->

                    TableScanExecutor.execute(
                            table,
                            rows,
                            queryPlan.getWhereExpression()
                    );

            case INDEX_SCAN ->

                    throw new UnsupportedOperationException(
                            "INDEX_SCAN execution support "
                                    + "has not yet been implemented."
                    );
        };
    }
}