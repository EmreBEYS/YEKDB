package com.yekdb.query.optimizer;

import com.yekdb.query.expression.Expression;
import com.yekdb.table.Table;

import java.util.Objects;

/**
 * Sorgular için temel yürütme planı oluşturur.
 *
 * Sprint 00-11 kapsamında varsayılan plan Full Table Scan'dir.
 * Index seçimi sonraki sprintlerde genişletilecektir.
 */
public final class QueryOptimizer {

    /**
     * Varsayılan QueryOptimizer oluşturur.
     */
    public QueryOptimizer() {
    }

    /**
     * Verilen sorgu için yürütme planı oluşturur.
     *
     * @param table sorgulanan tablo
     * @param whereExpression WHERE koşulu
     * @return oluşturulan yürütme planı
     */
    public QueryPlan optimize(
            Table table,
            Expression whereExpression
    ) {
        Objects.requireNonNull(
                table,
                "Table null olamaz."
        );

        if (whereExpression == null) {
            return new QueryPlan(
                    QueryPlanType.FULL_TABLE_SCAN,
                    null,
                    null,
                    "WHERE koşulu bulunmadığı için tüm tablo taranacak."
            );
        }

        return new QueryPlan(
                QueryPlanType.FULL_TABLE_SCAN,
                whereExpression,
                null,
                "Uygun index seçimi henüz uygulanmadığı için "
                        + "WHERE koşulu Full Table Scan ile yürütülecek."
        );
    }
}