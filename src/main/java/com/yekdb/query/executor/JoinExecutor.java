package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JOIN işlemlerinin temel executor sınıfı.
 *
 * Sprint 00-15:
 *
 * - INNER JOIN
 * - Nested Loop Join
 * - Table alias desteği
 * - Qualified column üretimi
 * - ON condition evaluation
 *
 * Örnek:
 *
 * employee e
 * INNER JOIN department d
 * ON e.department_id = d.id
 */

public final class JoinExecutor {
    private final ExpressionEvaluator expressionEvaluator;

    public JoinExecutor() {

        this(new ExpressionEvaluator());
    }

    public JoinExecutor(ExpressionEvaluator expressionEvaluator){
        this.expressionEvaluator=Objects.requireNonNull(expressionEvaluator,"ExpressionEvaluator cannot be null.");
    }
    // --------------------------------------------------
    // PUBLIC API
    // --------------------------------------------------

    /**
     * Tek bir JOIN clause çalıştırır.
     *
     * @param leftTable       sol tablo referansı
     * @param leftRows        sol tablo satırları
     * @param joinClause      JOIN bilgisi
     * @param rightRows       sağ tablo satırları
     * @return JOIN sonucu
     */
    public List<Map<String, Object>> execute(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        Objects.requireNonNull(
                leftTable,
                "Left table cannot be null."
        );

        Objects.requireNonNull(
                leftRows,
                "Left rows cannot be null."
        );

        Objects.requireNonNull(
                joinClause,
                "Join clause cannot be null."
        );

        Objects.requireNonNull(
                rightRows,
                "Right rows cannot be null."
        );

        return switch (joinClause.getJoinType()) {

            case INNER ->
                    executeInnerJoin(
                            leftTable,
                            leftRows,
                            joinClause,
                            rightRows
                    );
        };
    }

    // --------------------------------------------------
    // INNER JOIN
    // --------------------------------------------------

    /**
     * Nested Loop INNER JOIN uygular.
     */
    private List<Map<String, Object>> executeInnerJoin(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        TableReference rightTable =
                createRightTableReference(
                        joinClause
                );

        for (Map<String, Object> leftRow
                : leftRows) {

            for (Map<String, Object> rightRow
                    : rightRows) {

                Map<String, Object> joinedRow =
                        mergeRows(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRow
                        );

                boolean matches =
                        expressionEvaluator.evaluate(
                                joinClause.getCondition(),
                                joinedRow
                        );

                if (matches) {

                    result.add(
                            joinedRow
                    );
                }
            }
        }

        return result;
    }

    // --------------------------------------------------
    // ROW MERGE
    // --------------------------------------------------

    /**
     * Sol ve sağ satırı JOIN evaluator'ın anlayacağı
     * qualified kolon isimleriyle birleştirir.
     *
     * Örnek:
     *
     * employee e:
     *
     * id = 1
     * name = Yunus
     * department_id = 10
     *
     * department d:
     *
     * id = 10
     * name = IT
     *
     *
     * Sonuç:
     *
     * e.id = 1
     * employee.id = 1
     *
     * e.name = Yunus
     * employee.name = Yunus
     *
     * e.department_id = 10
     * employee.department_id = 10
     *
     * d.id = 10
     * department.id = 10
     *
     * d.name = IT
     * department.name = IT
     */
    private Map<String, Object> mergeRows(
            TableReference leftTable,
            Map<String, Object> leftRow,
            TableReference rightTable,
            Map<String, Object> rightRow
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        addQualifiedColumns(
                joinedRow,
                leftTable,
                leftRow
        );

        addQualifiedColumns(
                joinedRow,
                rightTable,
                rightRow
        );

        return joinedRow;
    }

    // --------------------------------------------------
    // QUALIFIED COLUMNS
    // --------------------------------------------------

    /**
     * Bir satırdaki kolonları:
     *
     * table.column
     *
     * ve alias varsa:
     *
     * alias.column
     *
     * şeklinde result map içerisine ekler.
     */
    private void addQualifiedColumns(
            Map<String, Object> target,
            TableReference table,
            Map<String, Object> row
    ) {

        Objects.requireNonNull(
                target,
                "Target row cannot be null."
        );

        Objects.requireNonNull(
                table,
                "Table reference cannot be null."
        );

        Objects.requireNonNull(
                row,
                "Source row cannot be null."
        );

        String tableName =
                table.getTableName();

        String alias =
                table.getAlias();

        for (Map.Entry<String, Object> entry
                : row.entrySet()) {

            String columnName =
                    normalizeSourceColumnName(
                            entry.getKey()
                    );

            Object value =
                    entry.getValue();

            /*
             * Gerçek tablo adı.
             *
             * employee.id
             */
            target.put(
                    tableName
                            + "."
                            + columnName,
                    value
            );

            /*
             * Alias varsa:
             *
             * e.id
             */
            if (table.hasAlias()) {

                target.put(
                        alias
                                + "."
                                + columnName,
                        value
                );
            }
        }
    }

    /**
     * Kaynak satır zaten qualified key içeriyorsa
     * sadece gerçek kolon adını ayırır.
     *
     * Örnek:
     *
     * e.id
     *
     * ->
     *
     * id
     */
    private String normalizeSourceColumnName(
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "Column name cannot be null."
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "Column name cannot be blank."
            );
        }

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0
                && dotIndex < normalized.length() - 1) {

            return normalized.substring(
                    dotIndex + 1
            );
        }

        return normalized;
    }

    // --------------------------------------------------
    // RIGHT TABLE
    // --------------------------------------------------

    /**
     * JoinClause bilgisinden sağ tablo referansı üretir.
     */
    private TableReference createRightTableReference(
            JoinClause joinClause
    ) {

        return new TableReference(
                joinClause.getTableName(),
                joinClause.getAlias()
        );
    }
}
