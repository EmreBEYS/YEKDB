package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JOIN + GROUP BY + Aggregate işlemlerinden sonra
 * oluşan sonuç satırlarına HAVING filtresi uygular.
 *
 * HAVING, WHERE'den farklı olarak ham JOIN satırlarını
 * değil aggregate sonucu oluşmuş satırları filtreler.
 *
 * Örnek:
 *
 * SELECT
 *     d.name,
 *     COUNT(e.id) AS employee_count
 * FROM department d
 * LEFT JOIN employee e
 *     ON d.id = e.department_id
 * GROUP BY d.name
 * HAVING employee_count >= 2;
 */
public final class JoinHavingExecutor {

    private final ExpressionEvaluator expressionEvaluator;

    public JoinHavingExecutor() {
        this(new ExpressionEvaluator());
    }

    public JoinHavingExecutor(
            ExpressionEvaluator expressionEvaluator
    ) {

        this.expressionEvaluator =
                Objects.requireNonNull(
                        expressionEvaluator,
                        "ExpressionEvaluator cannot be null."
                );
    }

    /**
     * Aggregate sonucu oluşmuş JOIN satırlarını
     * HAVING expression üzerinden filtreler.
     *
     * @param rows             aggregate sonuç satırları
     * @param havingExpression HAVING koşulu
     * @return HAVING koşulunu sağlayan satırlar
     */
    public List<Map<String, Object>> execute(
            List<Map<String, Object>> rows,
            Expression havingExpression
    ) {

        Objects.requireNonNull(
                rows,
                "HAVING source lines cannot be null."
        );

        /*
         * HAVING bulunmuyorsa mevcut sonuç doğrudan
         * korunur.
         */
        if (havingExpression == null) {
            return new ArrayList<>(rows);
        }

        List<Map<String, Object>> matchedRows =
                new ArrayList<>();

        for (Map<String, Object> row : rows) {

            Objects.requireNonNull(
                    row,
                    "The row in the HAVING result cannot be null."
            );

            boolean matches =
                    expressionEvaluator.evaluate(
                            havingExpression,
                            row
                    );

            if (matches) {
                matchedRows.add(row);
            }
        }

        return matchedRows;
    }
}