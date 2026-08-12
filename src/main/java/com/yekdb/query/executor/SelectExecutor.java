package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.optimizer.QueryPlan;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SELECT sorgularını yürütür.
 *
 * Sprint 00-14 final pipeline:
 *
 * WHERE
 *   ->
 * GROUP BY
 *   ->
 * Aggregate
 *   ->
 * HAVING
 *   ->
 * ORDER BY
 *   ->
 * LIMIT / FETCH
 *   ->
 * QueryResult
 *
 * INDEX_SCAN desteği ilerleyen sprintlerde eklenecektir.
 */
public final class SelectExecutor {

    /**
     * Aggregate expression:
     *
     * COUNT(*)
     * COUNT(id)
     * SUM(salary)
     * AVG(salary)
     * MIN(age)
     * MAX(age)
     */
    private static final Pattern AGGREGATE_PATTERN =
            Pattern.compile(
                    "^(COUNT|SUM|AVG|MIN|MAX)\\s*\\(\\s*(\\*|[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)\\s*\\)$",
                    Pattern.CASE_INSENSITIVE
            );

    private final QueryOptimizer queryOptimizer;

    private final OrderByExecutor orderByExecutor;

    private final GroupByExecutor groupByExecutor;

    private final AggregateExecutor aggregateExecutor;

    private final LimitExecutor limitExecutor;

    private final ExpressionEvaluator expressionEvaluator;

    // ==================================================
    // CONSTRUCTORS
    // ==================================================

    /**
     * Varsayılan executor bileşenleri.
     */
    public SelectExecutor() {

        this(
                new QueryOptimizer(),
                new OrderByExecutor(),
                new GroupByExecutor(),
                new AggregateExecutor(),
                new LimitExecutor(),
                new ExpressionEvaluator()
        );
    }

    /**
     * Eski constructor uyumluluğu.
     */
    public SelectExecutor(
            QueryOptimizer queryOptimizer
    ) {

        this(
                queryOptimizer,
                new OrderByExecutor(),
                new GroupByExecutor(),
                new AggregateExecutor(),
                new LimitExecutor(),
                new ExpressionEvaluator()
        );
    }

    /**
     * ORDER BY entegrasyonunda kullanılan
     * eski constructor uyumluluğu.
     */
    public SelectExecutor(
            QueryOptimizer queryOptimizer,
            OrderByExecutor orderByExecutor
    ) {

        this(
                queryOptimizer,
                orderByExecutor,
                new GroupByExecutor(),
                new AggregateExecutor(),
                new LimitExecutor(),
                new ExpressionEvaluator()
        );
    }

    /**
     * Bütün bağımlılıkların dışarıdan
     * verilebildiği constructor.
     */
    public SelectExecutor(
            QueryOptimizer queryOptimizer,
            OrderByExecutor orderByExecutor,
            GroupByExecutor groupByExecutor,
            AggregateExecutor aggregateExecutor,
            LimitExecutor limitExecutor,
            ExpressionEvaluator expressionEvaluator
    ) {

        this.queryOptimizer =
                Objects.requireNonNull(
                        queryOptimizer,
                        "QueryOptimizer cannot be null."
                );

        this.orderByExecutor =
                Objects.requireNonNull(
                        orderByExecutor,
                        "OrderByExecutor cannot be null."
                );

        this.groupByExecutor =
                Objects.requireNonNull(
                        groupByExecutor,
                        "GroupByExecutor cannot be null."
                );

        this.aggregateExecutor =
                Objects.requireNonNull(
                        aggregateExecutor,
                        "AggregateExecutor cannot be null."
                );

        this.limitExecutor =
                Objects.requireNonNull(
                        limitExecutor,
                        "LimitExecutor cannot be null."
                );

        this.expressionEvaluator =
                Objects.requireNonNull(
                        expressionEvaluator,
                        "ExpressionEvaluator cannot be null."
                );
    }

    // ==================================================
    // OLD SELECT API
    // ==================================================

    /**
     * Eski SELECT API.
     *
     * WHERE expression üzerinden çalışır.
     *
     * Geriye dönük uyumluluk için korunur.
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

    // ==================================================
    // FINAL SELECT STATEMENT PIPELINE
    // ==================================================

    /**
     * Sprint 00-14 final SELECT execution.
     *
     * Execution sırası:
     *
     * 1 - WHERE
     * 2 - GROUP BY
     * 3 - Aggregate
     * 4 - HAVING
     * 5 - ORDER BY
     * 6 - LIMIT / FETCH
     * 7 - QueryResult
     */
    public QueryResult executeStatement(
            Table table,
            List<Row> rows,
            SelectStatement statement
    ) {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                rows,
                "Row list cannot be null."
        );

        Objects.requireNonNull(
                statement,
                "SelectStatement cannot be null."
        );

        long startTime =
                System.nanoTime();

        // ----------------------------------------------
        // 1 - WHERE
        // ----------------------------------------------

        QueryResult scanResult =
                execute(
                        table,
                        rows,
                        statement.getWhereExpression()
                );

        List<Row> currentRows =
                new ArrayList<>(
                        scanResult.getRows()
                );

        List<Column> currentColumns =
                new ArrayList<>(
                        scanResult.getColumns()
                );

        // ----------------------------------------------
        // Aggregate query detection
        // ----------------------------------------------

        boolean containsAggregate =
                containsAggregateExpression(
                        statement.getSelectItems()
                );

        /*
         * GROUP BY veya aggregate varsa
         * yeni result schema oluşturulur.
         */
        if (statement.hasGroupBy()
                || containsAggregate) {

            validateAggregateQuery(
                    statement,
                    table,
                    containsAggregate
            );

            AggregateResult aggregateResult =
                    executeAggregatePipeline(
                            table,
                            currentRows,
                            statement,
                            containsAggregate
                    );

            currentRows =
                    new ArrayList<>(
                            aggregateResult.rows()
                    );

            currentColumns =
                    new ArrayList<>(
                            aggregateResult.columns()
                    );

            // ------------------------------------------
            // 4 - HAVING
            // ------------------------------------------

            if (statement.hasHaving()) {

                currentRows =
                        applyHaving(
                                currentRows,
                                currentColumns,
                                statement
                                        .getHavingClause()
                                        .getExpression()
                        );
            }
        }

        // ----------------------------------------------
        // 5 - ORDER BY
        // ----------------------------------------------

        if (statement.hasOrderBy()) {

            currentRows =
                    orderByExecutor.execute(
                            currentRows,
                            currentColumns,
                            statement.getOrderByItems()
                    );
        }

        // ----------------------------------------------
        // 6 - LIMIT
        // ----------------------------------------------

        if (statement.hasLimit()) {

            currentRows =
                    limitExecutor.execute(
                            currentRows,
                            statement.getLimitClause()
                    );
        }

        // ----------------------------------------------
        // 6 - FETCH
        // ----------------------------------------------

        if (statement.hasFetch()) {

            currentRows =
                    limitExecutor.execute(
                            currentRows,
                            statement.getFetchClause()
                    );
        }

        long executionTime =
                System.nanoTime()
                        - startTime;

        // ----------------------------------------------
        // 7 - QueryResult
        // ----------------------------------------------

        return QueryResult.selectSuccess(
                currentColumns,
                currentRows,
                executionTime
        );
    }

    // ==================================================
    // GROUP BY + AGGREGATE
    // ==================================================

    private AggregateResult executeAggregatePipeline(
            Table table,
            List<Row> rows,
            SelectStatement statement,
            boolean containsAggregate
    ) {

        Map<List<Object>, List<Row>> groups;

        // ----------------------------------------------
        // GROUP BY
        // ----------------------------------------------

        if (statement.hasGroupBy()) {

            groups =
                    groupByExecutor.execute(
                            rows,
                            table.getColumns(),
                            statement.getGroupByClause()
                    );

        } else {

            /*
             * Aggregate var fakat GROUP BY yok:
             *
             * SELECT COUNT(*)
             * FROM users;
             *
             * Bütün satırlar tek implicit group.
             */
            groups =
                    new LinkedHashMap<>();

            groups.put(
                    List.of(),
                    new ArrayList<>(rows)
            );
        }

        // ----------------------------------------------
        // Result schema
        // ----------------------------------------------

        List<Column> resultColumns =
                createAggregateResultColumns(
                        table,
                        statement.getSelectItems()
                );

        List<Row> resultRows =
                new ArrayList<>();

        /*
         * GROUP BY sonucu hiç grup üretmediyse
         * fakat global aggregate sorgusuysa yine
         * tek aggregate sonucu üretmeliyiz.
         */
        if (groups.isEmpty()
                && containsAggregate
                && !statement.hasGroupBy()) {

            groups.put(
                    List.of(),
                    List.of()
            );
        }

        // ----------------------------------------------
        // Her group için SELECT sonucu oluştur
        // ----------------------------------------------

        for (List<Row> groupRows
                : groups.values()) {

            List<Object> resultValues =
                    new ArrayList<>();

            for (SelectItem selectItem
                    : statement.getSelectItems()) {

                String expression =
                        selectItem
                                .getExpression()
                                .trim();

                AggregateCall aggregateCall =
                        parseAggregateCall(
                                expression
                        );

                /*
                 * Aggregate item
                 */
                if (aggregateCall != null) {

                    Object value =
                            aggregateExecutor.execute(
                                    groupRows,
                                    table.getColumns(),
                                    aggregateCall.function(),
                                    aggregateCall.columnName()
                            );

                    /*
                     * Mevcut Row modeli NULL kabul etmiyor.
                     *
                     * MIN/MAX empty input -> null üretebilir.
                     */
                    if (value == null) {

                        throw new QueryExecutionException(
                                "Aggregate result cannot currently be null. "
                                        + "Expression: "
                                        + expression
                                        + ". NULL Row support is not implemented yet."
                        );
                    }

                    resultValues.add(
                            value
                    );

                    continue;
                }

                /*
                 * Normal kolon:
                 *
                 * SELECT department, COUNT(*)
                 */
                if (groupRows.isEmpty()) {

                    throw new QueryExecutionException(
                            "Non-aggregate column cannot be produced "
                                    + "from an empty aggregate group: "
                                    + expression
                    );
                }

                int columnIndex =
                        findColumnIndex(
                                table.getColumns(),
                                expression
                        );

                resultValues.add(
                        groupRows
                                .get(0)
                                .getValue(
                                        columnIndex
                                )
                );
            }

            resultRows.add(
                    new Row(
                            resultValues
                    )
            );
        }

        return new AggregateResult(
                resultColumns,
                resultRows
        );
    }

    // ==================================================
    // RESULT COLUMNS
    // ==================================================

    /**
     * Aggregate/group result schema oluşturur.
     *
     * Örnek:
     *
     * SELECT
     *     department,
     *     COUNT(*) AS employee_count
     *
     * ->
     *
     * department STRING
     * employee_count LONG
     */
    private List<Column> createAggregateResultColumns(
            Table table,
            List<SelectItem> selectItems
    ) {

        List<Column> resultColumns =
                new ArrayList<>();

        for (SelectItem item : selectItems) {

            String expression =
                    item
                            .getExpression()
                            .trim();

            AggregateCall aggregateCall =
                    parseAggregateCall(
                            expression
                    );

            String outputName =
                    getOutputColumnName(
                            item
                    );

            DataType outputType;

            if (aggregateCall == null) {

                Column sourceColumn =
                        findColumn(
                                table.getColumns(),
                                expression
                        );

                outputType =
                        sourceColumn.getDataType();

            } else {

                outputType =
                        determineAggregateDataType(
                                table,
                                aggregateCall
                        );
            }

            /*
             * Aynı isimli output kolonlarını engelle.
             */
            boolean duplicate =
                    resultColumns.stream()
                            .anyMatch(
                                    column ->
                                            column.getName()
                                                    .equalsIgnoreCase(
                                                            outputName
                                                    )
                            );

            if (duplicate) {

                throw new QueryExecutionException(
                        "Duplicate SELECT result column: "
                                + outputName
                );
            }

            resultColumns.add(
                    new Column(
                            outputName,
                            outputType
                    )
            );
        }

        return List.copyOf(
                resultColumns
        );
    }

    private DataType determineAggregateDataType(
            Table table,
            AggregateCall aggregateCall
    ) {

        return switch (
                aggregateCall.function()
                ) {

            /*
             * COUNT her zaman Long.
             */
            case COUNT ->
                    DataType.LONG;

            /*
             * AggregateExecutor SUM / AVG için
             * Double döndürüyor.
             */
            case SUM, AVG ->
                    DataType.DOUBLE;

            /*
             * MIN / MAX kaynak kolonun
             * veri tipini korur.
             */
            case MIN, MAX -> {

                Column column =
                        findColumn(
                                table.getColumns(),
                                aggregateCall.columnName()
                        );

                yield column.getDataType();
            }
        };
    }

    // ==================================================
    // HAVING
    // ==================================================

    /**
     * Aggregate result row'larını HAVING expression
     * üzerinden filtreler.
     *
     * Örnek:
     *
     * employee_count > 2
     */
    private List<Row> applyHaving(
            List<Row> rows,
            List<Column> columns,
            Expression havingExpression
    ) {

        List<Row> matchedRows =
                new ArrayList<>();

        for (Row row : rows) {

            /*
             * Case-insensitive map.
             *
             * employee_count
             * EMPLOYEE_COUNT
             *
             * aynı kolonu çözebilir.
             */
            Map<String, Object> values =
                    new TreeMap<>(
                            String.CASE_INSENSITIVE_ORDER
                    );

            for (int i = 0;
                 i < columns.size();
                 i++) {

                values.put(
                        columns.get(i)
                                .getName(),
                        row.getValue(i)
                );
            }

            if (expressionEvaluator.evaluate(
                    havingExpression,
                    values
            )) {

                matchedRows.add(
                        row
                );
            }
        }

        return matchedRows;
    }

    // ==================================================
    // AGGREGATE VALIDATION
    // ==================================================

    /**
     * Aggregate SQL kurallarının temel kontrolü.
     */
    private void validateAggregateQuery(
            SelectStatement statement,
            Table table,
            boolean containsAggregate
    ) {

        List<SelectItem> selectItems =
                statement.getSelectItems();

        /*
         * SELECT *
         * GROUP BY ...
         *
         * Sprint 00-14 içerisinde desteklenmiyor.
         */
        for (SelectItem item : selectItems) {

            if ("*".equals(
                    item.getExpression()
                            .trim()
            )) {

                throw new QueryExecutionException(
                        "SELECT * cannot be used with GROUP BY "
                                + "or aggregate result generation."
                );
            }
        }

        /*
         * Aggregate var ancak GROUP BY yok.
         *
         * Geçerli:
         *
         * SELECT COUNT(*) FROM users
         *
         * Geçersiz:
         *
         * SELECT department, COUNT(*)
         * FROM users
         */
        if (containsAggregate
                && !statement.hasGroupBy()) {

            for (SelectItem item : selectItems) {

                if (parseAggregateCall(
                        item.getExpression()
                ) == null) {

                    throw new QueryExecutionException(
                            "Non-aggregate SELECT column requires GROUP BY: "
                                    + item.getExpression()
                    );
                }
            }

            return;
        }

        /*
         * GROUP BY varsa aggregate olmayan SELECT
         * kolonlarının GROUP BY içinde bulunması gerekir.
         */
        if (statement.hasGroupBy()) {

            GroupByClause groupByClause =
                    statement.getGroupByClause();

            for (SelectItem item : selectItems) {

                if (parseAggregateCall(
                        item.getExpression()
                ) != null) {

                    continue;
                }

                String selectedColumn =
                        normalizeColumnName(
                                item.getExpression()
                        );

                boolean grouped =
                        groupByClause
                                .getColumnNames()
                                .stream()
                                .map(
                                        this::normalizeColumnName
                                )
                                .anyMatch(
                                        selectedColumn::equalsIgnoreCase
                                );

                if (!grouped) {

                    throw new QueryExecutionException(
                            "SELECT column must appear in GROUP BY "
                                    + "or be used in an aggregate function: "
                                    + item.getExpression()
                    );
                }

                /*
                 * Kolonun gerçekten var olduğunu da doğrula.
                 */
                findColumn(
                        table.getColumns(),
                        item.getExpression()
                );
            }
        }
    }

    // ==================================================
    // AGGREGATE DETECTION
    // ==================================================

    private boolean containsAggregateExpression(
            List<SelectItem> selectItems
    ) {

        for (SelectItem item : selectItems) {

            if (parseAggregateCall(
                    item.getExpression()
            ) != null) {

                return true;
            }
        }

        return false;
    }

    /**
     * COUNT(*) gibi String expression'ı
     * AggregateExecutor parametrelerine dönüştürür.
     */
    private AggregateCall parseAggregateCall(
            String expression
    ) {

        if (expression == null
                || expression.isBlank()) {

            return null;
        }

        Matcher matcher =
                AGGREGATE_PATTERN.matcher(
                        expression.trim()
                );

        if (!matcher.matches()) {

            return null;
        }

        String functionName =
                matcher.group(1)
                        .toUpperCase(
                                Locale.ROOT
                        );

        String columnName =
                matcher.group(2);

        AggregateExecutor.AggregateFunction function =
                AggregateExecutor.AggregateFunction.valueOf(
                        functionName
                );

        return new AggregateCall(
                function,
                columnName
        );
    }

    // ==================================================
    // COLUMN HELPERS
    // ==================================================

    private int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {

        String normalizedColumnName =
                normalizeColumnName(
                        columnName
                );

        for (int i = 0;
             i < columns.size();
             i++) {

            if (columns.get(i)
                    .getName()
                    .equalsIgnoreCase(
                            normalizedColumnName
                    )) {

                return i;
            }
        }

        throw new QueryExecutionException(
                "Column not found: "
                        + columnName
        );
    }

    private Column findColumn(
            List<Column> columns,
            String columnName
    ) {

        int index =
                findColumnIndex(
                        columns,
                        columnName
                );

        return columns.get(
                index
        );
    }

    /**
     * Qualified kolon:
     *
     * e.salary
     *
     * ->
     *
     * salary
     */
    private String normalizeColumnName(
            String columnName
    ) {

        String normalized =
                Objects.requireNonNull(
                                columnName,
                                "Column name cannot be null."
                        )
                        .trim();

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0) {

            normalized =
                    normalized.substring(
                            dotIndex + 1
                    );
        }

        return normalized.toLowerCase(
                Locale.ROOT
        );
    }

    /**
     * SELECT output kolon adı.
     *
     * COUNT(*) AS employee_count
     *
     * ->
     *
     * employee_count
     *
     * Alias yoksa expression kullanılır.
     */
    private String getOutputColumnName(
            SelectItem item
    ) {

        String alias =
                item.getAlias();

        if (alias != null
                && !alias.isBlank()) {

            return alias.trim();
        }

        return item
                .getExpression()
                .trim();
    }

    // ==================================================
    // QUERY PLAN
    // ==================================================

    /**
     * Optimizer tarafından oluşturulan execution planını
     * çalıştırır.
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

    // ==================================================
    // INTERNAL RESULT TYPES
    // ==================================================

    /**
     * GROUP BY + Aggregate execution sonucu.
     */
    private record AggregateResult(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    /**
     * Parse edilmiş aggregate expression.
     */
    private record AggregateCall(
            AggregateExecutor.AggregateFunction function,
            String columnName
    ) {
    }
}