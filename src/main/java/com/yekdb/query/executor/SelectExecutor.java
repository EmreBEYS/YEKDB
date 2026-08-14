package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.optimizer.QueryPlan;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.TableReference;
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

    private final JoinExecutor joinExecutor;

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
                new ExpressionEvaluator(),
                new JoinExecutor()
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
                new ExpressionEvaluator(),
                new JoinExecutor()
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
                new ExpressionEvaluator(),
                new JoinExecutor()
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

        this(
                queryOptimizer,
                orderByExecutor,
                groupByExecutor,
                aggregateExecutor,
                limitExecutor,
                expressionEvaluator,
                new JoinExecutor()
        );
    }

    /**
     * Sprint 00-15 tam dependency-injection constructor.
     */
    public SelectExecutor(
            QueryOptimizer queryOptimizer,
            OrderByExecutor orderByExecutor,
            GroupByExecutor groupByExecutor,
            AggregateExecutor aggregateExecutor,
            LimitExecutor limitExecutor,
            ExpressionEvaluator expressionEvaluator,
            JoinExecutor joinExecutor
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

        this.joinExecutor =
                Objects.requireNonNull(
                        joinExecutor,
                        "JoinExecutor cannot be null."
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

        if (statement.hasJoins()) {

            throw new QueryExecutionException(
                    "JOIN statement requires the JOIN-aware executeStatement "
                            + "overload with right table rows."
            );
        }

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
    // SPRINT 00-16 - JOIN-AWARE SELECT PIPELINE
    // ==================================================

    /**
     * Sprint 00-16 JOIN-aware SELECT execution.
     *
     * Execution sırası:
     *
     * 1 - JOIN
     * 2 - WHERE
     * 3 - GROUP BY
     * 4 - Aggregate
     * 5 - HAVING
     * 6 - SELECT projection
     * 7 - ORDER BY
     * 8 - LIMIT / FETCH
     * 9 - QueryResult
     */
    public QueryResult executeStatement(
            Table leftTable,
            List<Row> leftRows,
            Table rightTable,
            List<Row> rightRows,
            SelectStatement statement
    ) {

        Objects.requireNonNull(
                leftTable,
                "Left table cannot be null."
        );

        Objects.requireNonNull(
                leftRows,
                "Left row list cannot be null."
        );

        Objects.requireNonNull(
                rightTable,
                "Right table cannot be null."
        );

        Objects.requireNonNull(
                rightRows,
                "Right row list cannot be null."
        );

        Objects.requireNonNull(
                statement,
                "SelectStatement cannot be null."
        );

        /*
         * JOIN yoksa eski SELECT pipeline aynen korunur.
         */
        if (!statement.hasJoins()) {

            return executeStatement(
                    leftTable,
                    leftRows,
                    statement
            );
        }

        /*
         * Bu overload fiziksel olarak tek sağ tablo aldığı
         * için burada yalnızca tek JOIN çalıştırılır.
         *
         * Multiple JOIN için MultiJoinExecutor kullanan
         * ayrı execution yolu eklenecektir.
         */
        if (statement.getJoinCount() != 1) {

            throw new QueryExecutionException(
                    "This SELECT overload supports exactly one JOIN. "
                            + "Use the multi-JOIN execution overload instead."
            );
        }

        long startTime =
                System.nanoTime();

        JoinClause joinClause =
                statement.getJoins()
                        .get(0);

        TableReference leftReference =
                statement.getTable();

        TableReference rightReference =
                new TableReference(
                        joinClause.getTableName(),
                        joinClause.getAlias()
                );

        // ----------------------------------------------
        // 1 - JOIN
        // ----------------------------------------------

        List<Map<String, Object>> leftRowMaps =
                convertRowsToMaps(
                        leftTable,
                        leftRows
                );

        List<Map<String, Object>> rightRowMaps =
                convertRowsToMaps(
                        rightTable,
                        rightRows
                );

        List<Map<String, Object>> joinedMaps =
                joinExecutor.execute(
                        leftReference,
                        leftRowMaps,
                        joinClause,
                        rightRowMaps
                );

        // ----------------------------------------------
        // 2 - WHERE
        // ----------------------------------------------

        if (statement.hasWhereClause()) {

            joinedMaps =
                    applyWhereToJoinedRows(
                            joinedMaps,
                            statement.getWhereExpression()
                    );
        }

        boolean containsAggregate =
                containsAggregateExpression(
                        statement.getSelectItems()
                );

        List<Column> currentColumns;
        List<Row> currentRows;

        /*
         * GROUP BY, aggregate veya HAVING varsa
         * JOIN-aware aggregate pipeline kullanılır.
         */
        if (statement.hasGroupBy()
                || containsAggregate
                || statement.hasHaving()) {

            validateJoinedAggregateQuery(
                    leftTable,
                    rightTable,
                    leftReference,
                    rightReference,
                    statement,
                    containsAggregate
            );

            JoinedAggregateResult aggregateResult =
                    executeJoinedAggregatePipeline(
                            leftTable,
                            rightTable,
                            leftReference,
                            rightReference,
                            joinedMaps,
                            statement,
                            containsAggregate
                    );

            currentColumns =
                    new ArrayList<>(
                            aggregateResult.columns()
                    );

            currentRows =
                    new ArrayList<>(
                            aggregateResult.rows()
                    );

            // ------------------------------------------
            // 5 - HAVING
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

        } else {

            // ------------------------------------------
            // 6 - Normal SELECT projection
            // ------------------------------------------

            JoinedProjection projection =
                    projectJoinedRows(
                            leftTable,
                            rightTable,
                            statement,
                            joinClause,
                            joinedMaps
                    );

            currentColumns =
                    new ArrayList<>(
                            projection.columns()
                    );

            currentRows =
                    new ArrayList<>(
                            projection.rows()
                    );
        }

        // ----------------------------------------------
        // 7 - ORDER BY
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
        // 8 - LIMIT
        // ----------------------------------------------

        if (statement.hasLimit()) {

            currentRows =
                    limitExecutor.execute(
                            currentRows,
                            statement.getLimitClause()
                    );
        }

        // ----------------------------------------------
        // 8 - FETCH
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
        // 9 - QueryResult
        // ----------------------------------------------

        return QueryResult.selectSuccess(
                currentColumns,
                currentRows,
                executionTime
        );
    }

    // ==================================================
    // SPRINT 00-16 - MULTIPLE JOIN SELECT PIPELINE
    // ==================================================

    /**
     * Sprint 00-16 multiple JOIN-aware SELECT execution.
     *
     * Bu overload birden fazla JOIN tablosunu sıralı olarak
     * MultiJoinExecutor üzerinden yürütür.
     *
     * Execution sırası:
     *
     * 1 - Multiple JOIN
     * 2 - WHERE
     * 3 - SELECT projection
     * 4 - ORDER BY
     * 5 - LIMIT / FETCH
     * 6 - QueryResult
     *
     * GROUP BY / aggregate / HAVING aynı pipeline içinde
     * multiple JOIN sonucu üzerinde çalıştırılır.
     */
    public QueryResult executeStatement(
            Table baseTable,
            List<Row> baseRows,
            List<Table> rightTables,
            List<List<Row>> rightTableRows,
            SelectStatement statement
    ) {

        Objects.requireNonNull(
                baseTable,
                "Base table cannot be null."
        );

        Objects.requireNonNull(
                baseRows,
                "Base row list cannot be null."
        );

        Objects.requireNonNull(
                rightTables,
                "Right table list cannot be null."
        );

        Objects.requireNonNull(
                rightTableRows,
                "Right table row-list cannot be null."
        );

        Objects.requireNonNull(
                statement,
                "SelectStatement cannot be null."
        );

        if (!statement.hasJoins()) {

            return executeStatement(
                    baseTable,
                    baseRows,
                    statement
            );
        }

        if (statement.getJoinCount() < 2) {

            throw new QueryExecutionException(
                    "Multiple JOIN execution requires at least two JOIN clauses."
            );
        }

        if (rightTables.size()
                != statement.getJoinCount()) {

            throw new QueryExecutionException(
                    "Right table count must match JOIN clause count."
            );
        }

        if (rightTableRows.size()
                != statement.getJoinCount()) {

            throw new QueryExecutionException(
                    "Right table row-list count must match JOIN clause count."
            );
        }

        for (int index = 0;
             index < rightTables.size();
             index++) {

            Objects.requireNonNull(
                    rightTables.get(index),
                    "Right table cannot be null."
            );

            Objects.requireNonNull(
                    rightTableRows.get(index),
                    "Right table row list cannot be null."
            );
        }

        boolean containsAggregate =
                containsAggregateExpression(
                        statement.getSelectItems()
                );

        long startTime =
                System.nanoTime();

        // ----------------------------------------------
        // 1 - ROW CONVERSION
        // ----------------------------------------------

        List<Map<String, Object>> baseRowMaps =
                convertRowsToMaps(
                        baseTable,
                        baseRows
                );

        List<List<Map<String, Object>>> convertedRightRows =
                new ArrayList<>();

        for (int index = 0;
             index < rightTables.size();
             index++) {

            convertedRightRows.add(
                    convertRowsToMaps(
                            rightTables.get(index),
                            rightTableRows.get(index)
                    )
            );
        }

        // ----------------------------------------------
        // 2 - MULTIPLE JOIN
        // ----------------------------------------------

        MultiJoinExecutor multiJoinExecutor =
                new MultiJoinExecutor();

        List<Map<String, Object>> joinedMaps =
                multiJoinExecutor.execute(
                        statement.getTable(),
                        baseRowMaps,
                        statement.getJoins(),
                        convertedRightRows
                );

        // ----------------------------------------------
        // 3 - WHERE
        // ----------------------------------------------

        if (statement.hasWhereClause()) {

            joinedMaps =
                    applyWhereToJoinedRows(
                            joinedMaps,
                            statement.getWhereExpression()
                    );
        }

        List<Column> currentColumns;
        List<Row> currentRows;

        List<Table> allTables =
                createMultiJoinTables(
                        baseTable,
                        rightTables
                );

        List<TableReference> references =
                createMultiJoinReferences(
                        statement
                );

        if (statement.hasGroupBy()
                || containsAggregate
                || statement.hasHaving()) {

            validateMultiJoinedAggregateQuery(
                    allTables,
                    references,
                    statement,
                    containsAggregate
            );

            JoinedAggregateResult aggregateResult =
                    executeMultiJoinedAggregatePipeline(
                            allTables,
                            references,
                            joinedMaps,
                            statement,
                            containsAggregate
                    );

            currentColumns =
                    new ArrayList<>(
                            aggregateResult.columns()
                    );

            currentRows =
                    new ArrayList<>(
                            aggregateResult.rows()
                    );

            if (statement.hasHaving()) {

                currentRows =
                        applyHaving(
                                currentRows,
                                currentColumns,
                                statement.getHavingClause()
                                        .getExpression()
                        );
            }

        } else {

            MultiJoinedProjection projection =
                    projectMultiJoinedRows(
                            baseTable,
                            rightTables,
                            statement,
                            joinedMaps
                    );

            currentColumns =
                    new ArrayList<>(
                            projection.columns()
                    );

            currentRows =
                    new ArrayList<>(
                            projection.rows()
                    );
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

        return QueryResult.selectSuccess(
                currentColumns,
                currentRows,
                executionTime
        );
    }

    /**
     * Multiple JOIN sonucunu final SELECT projection biçimine dönüştürür.
     */
    private MultiJoinedProjection projectMultiJoinedRows(
            Table baseTable,
            List<Table> rightTables,
            SelectStatement statement,
            List<Map<String, Object>> joinedRows
    ) {

        List<Table> allTables =
                new ArrayList<>();

        allTables.add(
                baseTable
        );

        allTables.addAll(
                rightTables
        );

        List<TableReference> references =
                createMultiJoinReferences(
                        statement
                );

        if (allTables.size()
                != references.size()) {

            throw new QueryExecutionException(
                    "Table metadata count must match multiple JOIN reference count."
            );
        }

        if (statement.selectsAllColumns()) {

            return projectAllMultiJoinedColumns(
                    allTables,
                    references,
                    joinedRows
            );
        }

        List<Column> resultColumns =
                new ArrayList<>();

        List<SelectedJoinedColumn> selectedColumns =
                new ArrayList<>();

        for (SelectItem item
                : statement.getSelectItems()) {

            MultiJoinedSourceColumn sourceColumn =
                    resolveMultiJoinedSourceColumn(
                            item.getExpression(),
                            allTables,
                            references
                    );

            String outputName =
                    getOutputColumnName(
                            item
                    );

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
                            sourceColumn.column()
                                    .getDataType()
                    )
            );

            selectedColumns.add(
                    new SelectedJoinedColumn(
                            sourceColumn.mapKey(),
                            outputName
                    )
            );
        }

        List<Row> resultRows =
                new ArrayList<>();

        for (Map<String, Object> joinedRow
                : joinedRows) {

            List<Object> values =
                    new ArrayList<>();

            for (SelectedJoinedColumn selectedColumn
                    : selectedColumns) {

                if (!containsKeyIgnoreCase(
                        joinedRow,
                        selectedColumn.mapKey()
                )) {

                    throw new QueryExecutionException(
                            "Column not found in multiple JOIN result: "
                                    + selectedColumn.mapKey()
                    );
                }

                Object value =
                        getValueIgnoreCase(
                                joinedRow,
                                selectedColumn.mapKey()
                        );

                if (value == null) {

                    throw new QueryExecutionException(
                            "Multiple JOIN projection produced a null value for column: "
                                    + selectedColumn.mapKey()
                    );
                }

                values.add(
                        value
                );
            }

            resultRows.add(
                    new Row(
                            values
                    )
            );
        }

        return new MultiJoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    /**
     * Multiple JOIN için SELECT * projection üretir.
     */
    private MultiJoinedProjection projectAllMultiJoinedColumns(
            List<Table> tables,
            List<TableReference> references,
            List<Map<String, Object>> joinedRows
    ) {

        List<Column> resultColumns =
                new ArrayList<>();

        List<String> mapKeys =
                new ArrayList<>();

        for (int index = 0;
             index < tables.size();
             index++) {

            addAllProjectionColumns(
                    resultColumns,
                    mapKeys,
                    tables.get(index),
                    references.get(index)
            );
        }

        List<Row> resultRows =
                new ArrayList<>();

        for (Map<String, Object> joinedRow
                : joinedRows) {

            List<Object> values =
                    new ArrayList<>();

            for (String mapKey : mapKeys) {

                if (!containsKeyIgnoreCase(
                        joinedRow,
                        mapKey
                )) {

                    throw new QueryExecutionException(
                            "Column not found in multiple JOIN result: "
                                    + mapKey
                    );
                }

                Object value =
                        getValueIgnoreCase(
                                joinedRow,
                                mapKey
                        );

                if (value == null) {

                    throw new QueryExecutionException(
                            "Multiple JOIN projection produced a null value for column: "
                                    + mapKey
                    );
                }

                values.add(
                        value
                );
            }

            resultRows.add(
                    new Row(
                            values
                    )
            );
        }

        return new MultiJoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    /**
     * Multiple JOIN için fiziksel tablo listesini oluşturur.
     */
    private List<Table> createMultiJoinTables(
            Table baseTable,
            List<Table> rightTables
    ) {

        List<Table> tables =
                new ArrayList<>();

        tables.add(
                baseTable
        );

        tables.addAll(
                rightTables
        );

        return List.copyOf(
                tables
        );
    }

    /**
     * SELECT statement içindeki base ve JOIN table reference listesini oluşturur.
     */
    private List<TableReference> createMultiJoinReferences(
            SelectStatement statement
    ) {

        List<TableReference> references =
                new ArrayList<>();

        references.add(
                statement.getTable()
        );

        for (JoinClause joinClause
                : statement.getJoins()) {

            references.add(
                    new TableReference(
                            joinClause.getTableName(),
                            joinClause.getAlias()
                    )
            );
        }

        return List.copyOf(
                references
        );
    }

    /**
     * Multiple JOIN projection için qualified veya güvenli
     * unqualified kolon çözümlemesi yapar.
     */
    private MultiJoinedSourceColumn resolveMultiJoinedSourceColumn(
            String expression,
            List<Table> tables,
            List<TableReference> references
    ) {

        String trimmed =
                Objects.requireNonNull(
                                expression,
                                "SELECT expression cannot be null."
                        )
                        .trim();

        if (trimmed.isEmpty()) {

            throw new QueryExecutionException(
                    "SELECT expression cannot be blank."
            );
        }

        int dotIndex =
                trimmed.lastIndexOf('.');

        if (dotIndex >= 0) {

            String qualifier =
                    trimmed.substring(
                            0,
                            dotIndex
                    );

            String columnName =
                    trimmed.substring(
                            dotIndex + 1
                    );

            for (int index = 0;
                 index < references.size();
                 index++) {

                TableReference reference =
                        references.get(index);

                if (!reference.matches(
                        qualifier
                )) {

                    continue;
                }

                Column column =
                        findColumnOrNull(
                                tables.get(index)
                                        .getColumns(),
                                columnName
                        );

                if (column == null) {

                    throw new QueryExecutionException(
                            "Column not found: "
                                    + expression
                    );
                }

                return new MultiJoinedSourceColumn(
                        column,
                        reference.getEffectiveName()
                                + "."
                                + column.getName()
                );
            }

            throw new QueryExecutionException(
                    "Unknown table or alias in SELECT column: "
                            + expression
            );
        }

        MultiJoinedSourceColumn resolved =
                null;

        for (int index = 0;
             index < tables.size();
             index++) {

            Column column =
                    findColumnOrNull(
                            tables.get(index)
                                    .getColumns(),
                            trimmed
                    );

            if (column == null) {

                continue;
            }

            if (resolved != null) {

                throw new QueryExecutionException(
                        "Ambiguous column reference: "
                                + expression
                );
            }

            TableReference reference =
                    references.get(index);

            resolved =
                    new MultiJoinedSourceColumn(
                            column,
                            reference.getEffectiveName()
                                    + "."
                                    + column.getName()
                    );
        }

        if (resolved == null) {

            throw new QueryExecutionException(
                    "Column not found: "
                            + expression
            );
        }

        return resolved;
    }

    /**
     * Storage Row listesini JoinExecutor'ın kullandığı
     * column -> value map biçimine dönüştürür.
     */
    private List<Map<String, Object>> convertRowsToMaps(
            Table table,
            List<Row> rows
    ) {

        List<Column> columns =
                table.getColumns();

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Row row : rows) {

            Map<String, Object> values =
                    new LinkedHashMap<>();

            for (int i = 0;
                 i < columns.size();
                 i++) {

                values.put(
                        columns.get(i)
                                .getName(),
                        row.getValue(i)
                );
            }

            result.add(
                    values
            );
        }

        return result;
    }

    /**
     * JOIN sonrasında WHERE filtresi uygular.
     *
     * Qualified kolonlar ExpressionEvaluator tarafından
     * doğrudan çözülebilir:
     *
     * e.name
     * d.name
     */
    private List<Map<String, Object>> applyWhereToJoinedRows(
            List<Map<String, Object>> rows,
            Expression whereExpression
    ) {

        List<Map<String, Object>> matched =
                new ArrayList<>();

        for (Map<String, Object> row : rows) {

            if (expressionEvaluator.evaluate(
                    whereExpression,
                    row
            )) {

                matched.add(
                        row
                );
            }
        }

        return matched;
    }

    /**
     * JOIN edilmiş map satırlarını SELECT projection
     * sonucuna dönüştürür.
     */
    private JoinedProjection projectJoinedRows(
            Table leftTable,
            Table rightTable,
            SelectStatement statement,
            JoinClause joinClause,
            List<Map<String, Object>> joinedRows
    ) {

        TableReference leftReference =
                statement.getTable();

        TableReference rightReference =
                new TableReference(
                        joinClause.getTableName(),
                        joinClause.getAlias()
                );

        if (statement.selectsAllColumns()) {

            return projectAllJoinedColumns(
                    leftTable,
                    rightTable,
                    leftReference,
                    rightReference,
                    joinedRows
            );
        }

        List<Column> resultColumns =
                new ArrayList<>();

        List<SelectedJoinedColumn> selectedColumns =
                new ArrayList<>();

        for (SelectItem item
                : statement.getSelectItems()) {

            String expression =
                    item.getExpression()
                            .trim();

            JoinedSourceColumn sourceColumn =
                    resolveJoinedSourceColumn(
                            expression,
                            leftTable,
                            rightTable,
                            leftReference,
                            rightReference
                    );

            String outputName =
                    getOutputColumnName(
                            item
                    );

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
                            sourceColumn.column()
                                    .getDataType()
                    )
            );

            selectedColumns.add(
                    new SelectedJoinedColumn(
                            sourceColumn.mapKey(),
                            outputName
                    )
            );
        }

        List<Row> resultRows =
                new ArrayList<>();

        for (Map<String, Object> joinedRow
                : joinedRows) {

            List<Object> values =
                    new ArrayList<>();

            for (SelectedJoinedColumn selectedColumn
                    : selectedColumns) {

                if (!containsKeyIgnoreCase(
                        joinedRow,
                        selectedColumn.mapKey()
                )) {

                    throw new QueryExecutionException(
                            "Column not found in JOIN result: "
                                    + selectedColumn.mapKey()
                    );
                }

                values.add(
                        getValueIgnoreCase(
                                joinedRow,
                                selectedColumn.mapKey()
                        )
                );
            }

            resultRows.add(
                    new Row(
                            values
                    )
            );
        }

        return new JoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    /**
     * SELECT * JOIN projection.
     *
     * Çakışmayı önlemek için result kolonları
     * effective table name ile qualified üretilir.
     *
     * Örnek:
     *
     * e.id
     * e.name
     * d.id
     * d.name
     */
    private JoinedProjection projectAllJoinedColumns(
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference,
            List<Map<String, Object>> joinedRows
    ) {

        List<Column> resultColumns =
                new ArrayList<>();

        List<String> mapKeys =
                new ArrayList<>();

        addAllProjectionColumns(
                resultColumns,
                mapKeys,
                leftTable,
                leftReference
        );

        addAllProjectionColumns(
                resultColumns,
                mapKeys,
                rightTable,
                rightReference
        );

        List<Row> resultRows =
                new ArrayList<>();

        for (Map<String, Object> joinedRow
                : joinedRows) {

            List<Object> values =
                    new ArrayList<>();

            for (String mapKey : mapKeys) {

                values.add(
                        getValueIgnoreCase(
                                joinedRow,
                                mapKey
                        )
                );
            }

            resultRows.add(
                    new Row(
                            values
                    )
            );
        }

        return new JoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    private void addAllProjectionColumns(
            List<Column> resultColumns,
            List<String> mapKeys,
            Table table,
            TableReference reference
    ) {

        String qualifier =
                reference.getEffectiveName();

        for (Column column
                : table.getColumns()) {

            String qualifiedName =
                    qualifier
                            + "."
                            + column.getName();

            resultColumns.add(
                    new Column(
                            qualifiedName,
                            column.getDataType()
                    )
            );

            mapKeys.add(
                    qualifiedName
            );
        }
    }

    /**
     * SELECT item'ın hangi JOIN tarafına ait olduğunu çözer.
     *
     * Qualified kolonlar doğrudan qualifier üzerinden,
     * unqualified kolonlar ise ambiguity kontrolü ile çözülür.
     */
    private JoinedSourceColumn resolveJoinedSourceColumn(
            String expression,
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference
    ) {

        String trimmed =
                Objects.requireNonNull(
                                expression,
                                "SELECT expression cannot be null."
                        )
                        .trim();

        int dotIndex =
                trimmed.lastIndexOf('.');

        if (dotIndex >= 0) {

            String qualifier =
                    trimmed.substring(
                            0,
                            dotIndex
                    );

            String columnName =
                    trimmed.substring(
                            dotIndex + 1
                    );

            if (leftReference.matches(
                    qualifier
            )) {

                Column column =
                        findColumn(
                                leftTable.getColumns(),
                                columnName
                        );

                return new JoinedSourceColumn(
                        column,
                        leftReference.getEffectiveName()
                                + "."
                                + column.getName()
                );
            }

            if (rightReference.matches(
                    qualifier
            )) {

                Column column =
                        findColumn(
                                rightTable.getColumns(),
                                columnName
                        );

                return new JoinedSourceColumn(
                        column,
                        rightReference.getEffectiveName()
                                + "."
                                + column.getName()
                );
            }

            throw new QueryExecutionException(
                    "Unknown table or alias in SELECT column: "
                            + expression
            );
        }

        Column leftColumn =
                findColumnOrNull(
                        leftTable.getColumns(),
                        trimmed
                );

        Column rightColumn =
                findColumnOrNull(
                        rightTable.getColumns(),
                        trimmed
                );

        if (leftColumn != null
                && rightColumn != null) {

            throw new QueryExecutionException(
                    "Ambiguous column reference: "
                            + expression
            );
        }

        if (leftColumn != null) {

            return new JoinedSourceColumn(
                    leftColumn,
                    leftReference.getEffectiveName()
                            + "."
                            + leftColumn.getName()
            );
        }

        if (rightColumn != null) {

            return new JoinedSourceColumn(
                    rightColumn,
                    rightReference.getEffectiveName()
                            + "."
                            + rightColumn.getName()
            );
        }

        throw new QueryExecutionException(
                "Column not found: "
                        + expression
        );
    }

    private Column findColumnOrNull(
            List<Column> columns,
            String columnName
    ) {

        String normalized =
                normalizeColumnName(
                        columnName
                );

        for (Column column : columns) {

            if (column.getName()
                    .equalsIgnoreCase(
                            normalized
                    )) {

                return column;
            }
        }

        return null;
    }

    private boolean containsKeyIgnoreCase(
            Map<String, Object> values,
            String key
    ) {

        for (String existingKey
                : values.keySet()) {

            if (existingKey.equalsIgnoreCase(
                    key
            )) {

                return true;
            }
        }

        return false;
    }

    private Object getValueIgnoreCase(
            Map<String, Object> values,
            String key
    ) {

        for (Map.Entry<String, Object> entry
                : values.entrySet()) {

            if (entry.getKey()
                    .equalsIgnoreCase(
                            key
                    )) {

                return entry.getValue();
            }
        }

        throw new QueryExecutionException(
                "Column not found in JOIN result: "
                        + key
        );
    }

    // ==================================================
    // SPRINT 00-16 - MULTIPLE JOIN + GROUP BY + AGGREGATE
    // ==================================================

    /**
     * Multiple JOIN sonucu oluşan Map satırları üzerinde
     * GROUP BY ve aggregate pipeline'ını yürütür.
     */
    private JoinedAggregateResult executeMultiJoinedAggregatePipeline(
            List<Table> tables,
            List<TableReference> references,
            List<Map<String, Object>> joinedRows,
            SelectStatement statement,
            boolean containsAggregate
    ) {

        Map<List<Object>, List<Map<String, Object>>> groups;

        if (statement.hasGroupBy()) {

            groups =
                    groupByExecutor.executeJoinedRows(
                            joinedRows,
                            statement.getGroupByClause()
                    );

        } else {

            groups =
                    new LinkedHashMap<>();

            groups.put(
                    List.of(),
                    new ArrayList<>(joinedRows)
            );
        }

        if (groups.isEmpty()
                && containsAggregate
                && !statement.hasGroupBy()) {

            groups.put(
                    List.of(),
                    List.of()
            );
        }

        List<Column> resultColumns =
                createMultiJoinedAggregateResultColumns(
                        tables,
                        references,
                        statement.getSelectItems()
                );

        List<Row> resultRows =
                new ArrayList<>();

        for (List<Map<String, Object>> groupRows
                : groups.values()) {

            List<Object> resultValues =
                    new ArrayList<>();

            for (SelectItem selectItem
                    : statement.getSelectItems()) {

                String expression =
                        selectItem.getExpression()
                                .trim();

                AggregateCall aggregateCall =
                        parseAggregateCall(
                                expression
                        );

                if (aggregateCall != null) {

                    Object value =
                            aggregateExecutor.executeJoinedRows(
                                    groupRows,
                                    aggregateCall.function(),
                                    aggregateCall.columnName()
                            );

                    if (value == null) {

                        throw new QueryExecutionException(
                                "Aggregate result cannot currently be null. "
                                        + "Expression: "
                                        + expression
                        );
                    }

                    resultValues.add(
                            value
                    );

                    continue;
                }

                if (groupRows.isEmpty()) {

                    throw new QueryExecutionException(
                            "Non-aggregate column cannot be produced "
                                    + "from an empty multiple JOIN aggregate group: "
                                    + expression
                    );
                }

                MultiJoinedSourceColumn sourceColumn =
                        resolveMultiJoinedSourceColumn(
                                expression,
                                tables,
                                references
                        );

                Object value =
                        getValueIgnoreCase(
                                groupRows.get(0),
                                sourceColumn.mapKey()
                        );

                if (value == null) {

                    throw new QueryExecutionException(
                            "Multiple JOIN aggregate projection produced a null value "
                                    + "for column: "
                                    + expression
                    );
                }

                resultValues.add(
                        value
                );
            }

            resultRows.add(
                    new Row(
                            resultValues
                    )
            );
        }

        return new JoinedAggregateResult(
                resultColumns,
                resultRows
        );
    }

    /**
     * Multiple JOIN aggregate sonucunun output şemasını oluşturur.
     */
    private List<Column> createMultiJoinedAggregateResultColumns(
            List<Table> tables,
            List<TableReference> references,
            List<SelectItem> selectItems
    ) {

        List<Column> resultColumns =
                new ArrayList<>();

        for (SelectItem item : selectItems) {

            String expression =
                    item.getExpression()
                            .trim();

            String outputName =
                    getOutputColumnName(
                            item
                    );

            AggregateCall aggregateCall =
                    parseAggregateCall(
                            expression
                    );

            DataType outputType;

            if (aggregateCall != null) {

                outputType =
                        determineMultiJoinedAggregateDataType(
                                tables,
                                references,
                                aggregateCall
                        );

            } else {

                MultiJoinedSourceColumn sourceColumn =
                        resolveMultiJoinedSourceColumn(
                                expression,
                                tables,
                                references
                        );

                outputType =
                        sourceColumn.column()
                                .getDataType();
            }

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

    /**
     * Multiple JOIN aggregate sonucunun DataType bilgisini belirler.
     */
    private DataType determineMultiJoinedAggregateDataType(
            List<Table> tables,
            List<TableReference> references,
            AggregateCall aggregateCall
    ) {

        return switch (aggregateCall.function()) {

            case COUNT ->
                    DataType.LONG;

            case SUM, AVG ->
                    DataType.DOUBLE;

            case MIN, MAX -> {

                if ("*".equals(
                        aggregateCall.columnName()
                )) {

                    throw new QueryExecutionException(
                            aggregateCall.function()
                                    + " does not support '*'."
                    );
                }

                MultiJoinedSourceColumn sourceColumn =
                        resolveMultiJoinedSourceColumn(
                                aggregateCall.columnName(),
                                tables,
                                references
                        );

                yield sourceColumn.column()
                        .getDataType();
            }
        };
    }

    /**
     * Multiple JOIN + GROUP BY / aggregate sorgularının
     * temel SQL kurallarını doğrular.
     */
    private void validateMultiJoinedAggregateQuery(
            List<Table> tables,
            List<TableReference> references,
            SelectStatement statement,
            boolean containsAggregate
    ) {

        if (tables.size()
                != references.size()) {

            throw new QueryExecutionException(
                    "Table metadata count must match multiple JOIN reference count."
            );
        }

        List<SelectItem> selectItems =
                statement.getSelectItems();

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

        if (statement.hasHaving()
                && !statement.hasGroupBy()
                && !containsAggregate) {

            throw new QueryExecutionException(
                    "HAVING requires GROUP BY or an aggregate query."
            );
        }

        if (containsAggregate
                && !statement.hasGroupBy()) {

            for (SelectItem item : selectItems) {

                AggregateCall aggregateCall =
                        parseAggregateCall(
                                item.getExpression()
                        );

                if (aggregateCall == null) {

                    throw new QueryExecutionException(
                            "Non-aggregate SELECT column requires GROUP BY: "
                                    + item.getExpression()
                    );
                }

                if (!"*".equals(
                        aggregateCall.columnName()
                )) {

                    resolveMultiJoinedSourceColumn(
                            aggregateCall.columnName(),
                            tables,
                            references
                    );
                }
            }

            return;
        }

        if (statement.hasGroupBy()) {

            List<String> groupedColumns =
                    statement.getGroupByClause()
                            .getColumnNames();

            for (String groupedColumn
                    : groupedColumns) {

                resolveMultiJoinedSourceColumn(
                        groupedColumn,
                        tables,
                        references
                );
            }

            for (SelectItem item : selectItems) {

                AggregateCall aggregateCall =
                        parseAggregateCall(
                                item.getExpression()
                        );

                if (aggregateCall != null) {

                    if (!"*".equals(
                            aggregateCall.columnName()
                    )) {

                        resolveMultiJoinedSourceColumn(
                                aggregateCall.columnName(),
                                tables,
                                references
                        );
                    }

                    continue;
                }

                String selected =
                        item.getExpression()
                                .trim();

                boolean grouped =
                        groupedColumns.stream()
                                .anyMatch(
                                        groupedColumn ->
                                                groupedColumn.equalsIgnoreCase(
                                                        selected
                                                )
                                );

                if (!grouped) {

                    throw new QueryExecutionException(
                            "SELECT column must appear in GROUP BY "
                                    + "or be used in an aggregate function: "
                                    + item.getExpression()
                    );
                }

                resolveMultiJoinedSourceColumn(
                        selected,
                        tables,
                        references
                );
            }
        }
    }

    // ==================================================
    // SPRINT 00-16 - JOIN + GROUP BY + AGGREGATE
    // ==================================================

    /**
     * JOIN sonucu oluşmuş Map satırları üzerinde
     * GROUP BY ve aggregate işlemlerini yürütür.
     */
    private JoinedAggregateResult executeJoinedAggregatePipeline(
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference,
            List<Map<String, Object>> joinedRows,
            SelectStatement statement,
            boolean containsAggregate
    ) {

        Map<List<Object>, List<Map<String, Object>>> groups;

        // ----------------------------------------------
        // GROUP BY
        // ----------------------------------------------

        if (statement.hasGroupBy()) {

            groups =
                    groupByExecutor.executeJoinedRows(
                            joinedRows,
                            statement.getGroupByClause()
                    );

        } else {

            /*
             * GROUP BY bulunmayan aggregate sorgusunda
             * bütün JOIN sonucu tek implicit gruptur.
             */
            groups =
                    new LinkedHashMap<>();

            groups.put(
                    List.of(),
                    new ArrayList<>(
                            joinedRows
                    )
            );
        }

        /*
         * Global aggregate sorgusunda kaynak satır bulunmasa
         * bile tek aggregate sonucu üretilebilir.
         */
        if (groups.isEmpty()
                && containsAggregate
                && !statement.hasGroupBy()) {

            groups.put(
                    List.of(),
                    List.of()
            );
        }

        List<Column> resultColumns =
                createJoinedAggregateResultColumns(
                        leftTable,
                        rightTable,
                        leftReference,
                        rightReference,
                        statement.getSelectItems()
                );

        List<Row> resultRows =
                new ArrayList<>();

        // ----------------------------------------------
        // Her grup için SELECT sonucu
        // ----------------------------------------------

        for (List<Map<String, Object>> groupRows
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
                 * Aggregate SELECT item.
                 */
                if (aggregateCall != null) {

                    Object value =
                            aggregateExecutor.executeJoinedRows(
                                    groupRows,
                                    aggregateCall.function(),
                                    aggregateCall.columnName()
                            );

                    /*
                     * Mevcut Row modeli NULL kabul etmediği için
                     * NULL aggregate sonucu final Row'a taşınamaz.
                     */
                    if (value == null) {

                        throw new QueryExecutionException(
                                "Aggregate result cannot currently be null. "
                                        + "Expression: "
                                        + expression
                        );
                    }

                    resultValues.add(
                            value
                    );

                    continue;
                }

                /*
                 * Aggregate olmayan GROUP BY kolonu
                 * grubun ilk satırından okunur.
                 */
                if (groupRows.isEmpty()) {

                    throw new QueryExecutionException(
                            "Non-aggregate column cannot be produced "
                                    + "from an empty JOIN aggregate group: "
                                    + expression
                    );
                }

                Object value =
                        getJoinedColumnValue(
                                groupRows.get(0),
                                expression
                        );

                /*
                 * Outer JOIN sonucunda değer NULL olabilir.
                 * Row NULL-aware hale gelene kadar burada
                 * güvenli biçimde hata üretilir.
                 */
                if (value == null) {

                    throw new QueryExecutionException(
                            "JOIN aggregate projection produced a null value "
                                    + "for column: "
                                    + expression
                    );
                }

                resultValues.add(
                        value
                );
            }

            resultRows.add(
                    new Row(
                            resultValues
                    )
            );
        }

        return new JoinedAggregateResult(
                resultColumns,
                resultRows
        );
    }

    /**
     * JOIN + GROUP BY / aggregate sonucu için
     * output kolon şemasını oluşturur.
     */
    private List<Column> createJoinedAggregateResultColumns(
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference,
            List<SelectItem> selectItems
    ) {

        List<Column> resultColumns =
                new ArrayList<>();

        for (SelectItem item : selectItems) {

            String expression =
                    item.getExpression()
                            .trim();

            String outputName =
                    getOutputColumnName(
                            item
                    );

            AggregateCall aggregateCall =
                    parseAggregateCall(
                            expression
                    );

            DataType outputType;

            if (aggregateCall != null) {

                outputType =
                        determineJoinedAggregateDataType(
                                leftTable,
                                rightTable,
                                leftReference,
                                rightReference,
                                aggregateCall
                        );

            } else {

                JoinedSourceColumn sourceColumn =
                        resolveJoinedSourceColumn(
                                expression,
                                leftTable,
                                rightTable,
                                leftReference,
                                rightReference
                        );

                outputType =
                        sourceColumn
                                .column()
                                .getDataType();
            }

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

    /**
     * JOIN aggregate sonucunun DataType bilgisini belirler.
     */
    private DataType determineJoinedAggregateDataType(
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference,
            AggregateCall aggregateCall
    ) {

        return switch (
                aggregateCall.function()
                ) {

            /*
             * COUNT her zaman LONG.
             */
            case COUNT ->
                    DataType.LONG;

            /*
             * AggregateExecutor SUM / AVG için
             * Double döndürür.
             */
            case SUM, AVG ->
                    DataType.DOUBLE;

            /*
             * MIN / MAX kaynak kolon tipini korur.
             */
            case MIN, MAX -> {

                if ("*".equals(
                        aggregateCall.columnName()
                )) {

                    throw new QueryExecutionException(
                            aggregateCall.function()
                                    + " does not support '*'."
                    );
                }

                JoinedSourceColumn sourceColumn =
                        resolveJoinedSourceColumn(
                                aggregateCall.columnName(),
                                leftTable,
                                rightTable,
                                leftReference,
                                rightReference
                        );

                yield sourceColumn
                        .column()
                        .getDataType();
            }
        };
    }

    /**
     * JOIN sonucundan qualified veya güvenli
     * unqualified kolon değerini çözer.
     */
    private Object getJoinedColumnValue(
            Map<String, Object> row,
            String columnName
    ) {

        Objects.requireNonNull(
                row,
                "JOIN row cannot be null."
        );

        Objects.requireNonNull(
                columnName,
                "Column name cannot be null."
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new QueryExecutionException(
                    "Column name cannot be blank."
            );
        }

        /*
         * Qualified kolon doğrudan aranır.
         */
        if (normalized.contains(".")) {

            return getValueIgnoreCase(
                    row,
                    normalized
            );
        }

        /*
         * Önce normal tek tablo key'i varsa kullanılır.
         */
        for (Map.Entry<String, Object> entry
                : row.entrySet()) {

            if (entry.getKey()
                    .equalsIgnoreCase(
                            normalized
                    )) {

                return entry.getValue();
            }
        }

        /*
         * Unqualified JOIN kolonlarını qualified
         * key'ler üzerinden çöz.
         */
        String suffix =
                "."
                        + normalized;

        Object resolvedValue = null;
        boolean found = false;

        for (Map.Entry<String, Object> entry
                : row.entrySet()) {

            if (!entry.getKey()
                    .toLowerCase(Locale.ROOT)
                    .endsWith(
                            suffix.toLowerCase(
                                    Locale.ROOT
                            )
                    )) {

                continue;
            }

            if (!found) {

                resolvedValue =
                        entry.getValue();

                found = true;

                continue;
            }

            /*
             * JoinExecutor aynı fiziksel kolon için hem
             * table.column hem alias.column key'i tutabilir.
             *
             * Aynı değer duplicate alias olarak kabul edilir.
             */
            if (!Objects.equals(
                    resolvedValue,
                    entry.getValue()
            )) {

                throw new QueryExecutionException(
                        "Ambiguous column reference: "
                                + columnName
                );
            }
        }

        if (!found) {

            throw new QueryExecutionException(
                    "Column not found in JOIN result: "
                            + columnName
            );
        }

        return resolvedValue;
    }

    /**
     * JOIN + GROUP BY / aggregate sorgularının
     * temel SQL kurallarını doğrular.
     */
    private void validateJoinedAggregateQuery(
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference,
            SelectStatement statement,
            boolean containsAggregate
    ) {

        List<SelectItem> selectItems =
                statement.getSelectItems();

        /*
         * SELECT * aggregate/group sorgularında
         * desteklenmez.
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
         * HAVING tek başına kullanılamaz.
         */
        if (statement.hasHaving()
                && !statement.hasGroupBy()
                && !containsAggregate) {

            throw new QueryExecutionException(
                    "HAVING requires GROUP BY or an aggregate query."
            );
        }

        /*
         * Aggregate var fakat GROUP BY yoksa
         * bütün SELECT item'ları aggregate olmalıdır.
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
         * GROUP BY mevcutsa aggregate olmayan
         * SELECT kolonları GROUP BY içinde bulunmalıdır.
         */
        if (statement.hasGroupBy()) {

            List<String> groupedColumns =
                    statement
                            .getGroupByClause()
                            .getColumnNames();

            for (SelectItem item : selectItems) {

                if (parseAggregateCall(
                        item.getExpression()
                ) != null) {

                    continue;
                }

                String selected =
                        item.getExpression()
                                .trim();

                boolean grouped =
                        groupedColumns.stream()
                                .anyMatch(
                                        groupedColumn ->
                                                groupedColumn
                                                        .equalsIgnoreCase(
                                                                selected
                                                        )
                                );

                if (!grouped) {

                    throw new QueryExecutionException(
                            "SELECT column must appear in GROUP BY "
                                    + "or be used in an aggregate function: "
                                    + item.getExpression()
                    );
                }

                /*
                 * Kolonun gerçekten JOIN tablolarından
                 * birinde bulunduğunu doğrula.
                 */
                resolveJoinedSourceColumn(
                        selected,
                        leftTable,
                        rightTable,
                        leftReference,
                        rightReference
                );
            }
        }
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
     * Multiple JOIN projection sonucu.
     */
    private record MultiJoinedProjection(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    /**
     * Multiple JOIN SELECT item çözümleme sonucu.
     */
    private record MultiJoinedSourceColumn(
            Column column,
            String mapKey
    ) {
    }

    /**
     * JOIN projection sonucu.
     */
    private record JoinedProjection(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    /**
     * JOIN SELECT item çözümleme sonucu.
     */
    private record JoinedSourceColumn(
            Column column,
            String mapKey
    ) {
    }

    /**
     * Projection sırasında kullanılan seçilmiş JOIN kolonu.
     */
    private record SelectedJoinedColumn(
            String mapKey,
            String outputName
    ) {
    }

    /**
     * JOIN + GROUP BY + Aggregate execution sonucu.
     */
    private record JoinedAggregateResult(
            List<Column> columns,
            List<Row> rows
    ) {
    }

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