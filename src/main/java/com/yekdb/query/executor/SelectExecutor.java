package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.index.RecordPointer;
import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.optimizer.OptimizedQuery;
import com.yekdb.query.optimizer.OptimizationContext;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.optimizer.QueryPlan;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.TableReference;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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


    private final QueryOptimizer queryOptimizer;

    private final OrderByExecutor orderByExecutor;

    private final LimitExecutor limitExecutor;

    private final ExpressionEvaluator expressionEvaluator;

    /**
     * Sprint 00-29 Phase 13 B+ Tree INDEX_SCAN executor.
     */
    private final IndexScanExecutor indexScanExecutor =
            new IndexScanExecutor();

    private final JoinExecutor joinExecutor;

    private final SelectJoinProjectionExecutor selectJoinProjectionExecutor =
            new SelectJoinProjectionExecutor();

    private final SelectAggregateExecutor selectAggregateExecutor;

    /**
     * Sprint 00-28 Phase 7 scalar function SELECT projection support.
     */
    private final SelectFunctionProjectionExecutor selectFunctionProjectionExecutor =
            new SelectFunctionProjectionExecutor();

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

        GroupByExecutor validatedGroupByExecutor =
                Objects.requireNonNull(
                        groupByExecutor,
                        "GroupByExecutor cannot be null."
                );

        AggregateExecutor validatedAggregateExecutor =
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

        this.selectAggregateExecutor =
                new SelectAggregateExecutor(
                        validatedGroupByExecutor,
                        validatedAggregateExecutor,
                        this.expressionEvaluator,
                        this.selectJoinProjectionExecutor
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

    /**
     * B+ Tree index listesi ve fiziksel row resolver ile index-aware SELECT çalıştırır.
     *
     * Uygun index yoksa otomatik olarak Full Table Scan fallback uygulanır.
     */
    public QueryResult execute(
            Table table,
            List<Row> rows,
            Expression whereExpression,
            List<Index<?>> availableIndexes,
            Function<RecordPointer, Row> rowResolver
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
                availableIndexes,
                "Available index list cannot be null."
        );

        Objects.requireNonNull(
                rowResolver,
                "Row resolver cannot be null."
        );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                table,
                                whereExpression,
                                availableIndexes
                        )
                );

        QueryPlan queryPlan =
                optimizedQuery.getQueryPlan();

        Objects.requireNonNull(
                queryPlan,
                "QueryOptimizer cannot return null QueryPlan."
        );

        return executeIndexAwarePlan(
                queryPlan,
                table,
                rows,
                availableIndexes,
                rowResolver,
                optimizedQuery.getOptimizedWhereExpression()
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
     * 5 - SELECT projection
     * 6 - ORDER BY
     * 7 - LIMIT / FETCH
     * 8 - QueryResult
     */
    public QueryResult executeStatement(
            Table table,
            List<Row> rows,
            SelectStatement statement
    ) {
        return executeStatementInternal(
                table,
                rows,
                statement,
                null,
                null
        );
    }

    /**
     * B+ Tree index context'i bulunan tek-table SELECT pipeline'ı.
     * Projection / aggregate / order / limit davranışları normal
     * executeStatement yolu ile aynıdır; yalnızca WHERE access path
     * index-aware çalışır.
     */
    public QueryResult executeStatement(
            Table table,
            List<Row> rows,
            SelectStatement statement,
            List<Index<?>> availableIndexes,
            Function<RecordPointer, Row> rowResolver
    ) {
        Objects.requireNonNull(
                availableIndexes,
                "Available index list cannot be null."
        );
        Objects.requireNonNull(
                rowResolver,
                "Row resolver cannot be null."
        );

        return executeStatementInternal(
                table,
                rows,
                statement,
                availableIndexes,
                rowResolver
        );
    }

    private QueryResult executeStatementInternal(
            Table table,
            List<Row> rows,
            SelectStatement statement,
            List<Index<?>> availableIndexes,
            Function<RecordPointer, Row> rowResolver
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

        QueryResult scanResult;

        if (availableIndexes != null
                && rowResolver != null
                && statement.getWhereExpression() != null) {

            scanResult = execute(
                    table,
                    rows,
                    statement.getWhereExpression(),
                    availableIndexes,
                    rowResolver
            );

        } else {

            scanResult = execute(
                    table,
                    rows,
                    statement.getWhereExpression()
            );
        }

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
                selectAggregateExecutor.containsAggregateExpression(
                        statement.getSelectItems()
                );

        /*
         * GROUP BY veya aggregate varsa
         * yeni result schema oluşturulur.
         */
        if (statement.hasGroupBy()
                || containsAggregate) {

            selectAggregateExecutor.validateAggregateQuery(
                    statement,
                    table,
                    containsAggregate
            );

            SelectAggregateExecutor.AggregateResult aggregateResult =
                    selectAggregateExecutor.executeAggregatePipeline(
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
                        selectAggregateExecutor.applyHaving(
                                currentRows,
                                currentColumns,
                                statement
                                        .getHavingClause()
                                        .getExpression()
                        );
            }

        } else {

            // ------------------------------------------
            // 4 - NORMAL SELECT PROJECTION
            // ------------------------------------------

            SingleTableProjection projection =
                    projectSingleTableRows(
                            table,
                            currentRows,
                            statement
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

        Map<String, Set<String>> requiredColumns =
                collectJoinProjectionPushdownColumns(
                        statement
                );

        List<Map<String, Object>> leftRowMaps =
                convertRowsToMaps(
                        leftTable,
                        leftRows,
                        requiredColumnsFor(
                                leftReference,
                                requiredColumns
                        )
                );

        List<Map<String, Object>> rightRowMaps =
                convertRowsToMaps(
                        rightTable,
                        rightRows,
                        requiredColumnsFor(
                                rightReference,
                                requiredColumns
                        )
                );

        rightRowMaps =
                applyPushdownPredicates(
                        rightReference,
                        rightRowMaps,
                        statement.getWhereExpression()
                );

        leftRowMaps =
                applyPushdownPredicates(
                        leftReference,
                        leftRowMaps,
                        statement.getWhereExpression()
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
                selectAggregateExecutor.containsAggregateExpression(
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

            selectAggregateExecutor.validateJoinedAggregateQuery(
                    leftTable,
                    rightTable,
                    leftReference,
                    rightReference,
                    statement,
                    containsAggregate
            );

            SelectAggregateExecutor.JoinedAggregateResult aggregateResult =
                    selectAggregateExecutor.executeJoinedAggregatePipeline(
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
                        selectAggregateExecutor.applyHaving(
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

            SelectJoinProjectionExecutor.JoinedProjection projection =
                    selectJoinProjectionExecutor.projectJoinedRows(
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
                selectAggregateExecutor.containsAggregateExpression(
                        statement.getSelectItems()
                );

        long startTime =
                System.nanoTime();

        // ----------------------------------------------
        // 1 - ROW CONVERSION
        // ----------------------------------------------

        Map<String, Set<String>> requiredColumns =
                collectJoinProjectionPushdownColumns(
                        statement
                );

        List<Map<String, Object>> baseRowMaps =
                convertRowsToMaps(
                        baseTable,
                        baseRows,
                        requiredColumnsFor(
                                statement.getTable(),
                                requiredColumns
                        )
                );

        List<List<Map<String, Object>>> convertedRightRows =
                new ArrayList<>();

        for (int index = 0;
             index < rightTables.size();
             index++) {

            convertedRightRows.add(
                    convertRowsToMaps(
                            rightTables.get(index),
                            rightTableRows.get(index),
                            requiredColumnsFor(
                                    new TableReference(
                                            statement.getJoins()
                                                    .get(index)
                                                    .getTableName(),
                                            statement.getJoins()
                                                    .get(index)
                                                    .getAlias()
                                    ),
                                    requiredColumns
                            )
                    )
            );
        }

        baseRowMaps =
                applyPushdownPredicates(
                        statement.getTable(),
                        baseRowMaps,
                        statement.getWhereExpression()
                );

        for (int index = 0;
             index < convertedRightRows.size();
             index++) {

            JoinClause joinClause =
                    statement.getJoins()
                            .get(index);

            TableReference rightReference =
                    new TableReference(
                            joinClause.getTableName(),
                            joinClause.getAlias()
                    );

            convertedRightRows.set(
                    index,
                    applyPushdownPredicates(
                            rightReference,
                            convertedRightRows.get(index),
                            statement.getWhereExpression()
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

            selectAggregateExecutor.validateMultiJoinedAggregateQuery(
                    allTables,
                    references,
                    statement,
                    containsAggregate
            );

            SelectAggregateExecutor.JoinedAggregateResult aggregateResult =
                    selectAggregateExecutor.executeMultiJoinedAggregatePipeline(
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
                        selectAggregateExecutor.applyHaving(
                                currentRows,
                                currentColumns,
                                statement.getHavingClause()
                                        .getExpression()
                        );
            }

        } else {

            SelectJoinProjectionExecutor.MultiJoinedProjection projection =
                    selectJoinProjectionExecutor.projectMultiJoinedRows(
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
     * Storage Row listesini JoinExecutor'ın kullandığı
     * column -> value map biçimine dönüştürür.
     */
    private List<Map<String, Object>> convertRowsToMaps(
            Table table,
            List<Row> rows
    ) {

        return convertRowsToMaps(
                table,
                rows,
                null
        );
    }

    /**
     * Storage Row listesini JoinExecutor'ın kullandığı
     * column -> value map biçimine dönüştürür.
     *
     * requiredColumns null ise bütün kolonlar taşınır.
     */
    private List<Map<String, Object>> convertRowsToMaps(
            Table table,
            List<Row> rows,
            Set<String> requiredColumns
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

                Column column =
                        columns.get(i);

                if (requiredColumns != null
                        && !requiredColumns.contains(
                        normalizeIdentifier(
                                column.getName()
                        )
                )) {

                    continue;
                }

                values.put(
                        column.getName(),
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
     * JOIN map conversion için gerekli kolonları hesaplar.
     *
     * Bu pushdown sadece taşınan kolon sayısını azaltır; SELECT * ve aggregate
     * sorgularda tüm kolonlar korunur.
     */
    private Map<String, Set<String>> collectJoinProjectionPushdownColumns(
            SelectStatement statement
    ) {

        if (statement.selectsAllColumns()
                || statement.hasGroupBy()
                || statement.hasHaving()
                || containsAggregateProjection(
                statement
        )) {

            return Map.of();
        }

        Map<String, Set<String>> requiredColumns =
                new LinkedHashMap<>();

        for (SelectItem item : statement.getSelectItems()) {

            if (item.isFunctionExpression()) {
                collectFunctionColumns(
                        item.getFunctionExpression(),
                        requiredColumns
                );
            } else {
                collectColumnReference(
                        item.getExpression(),
                        requiredColumns
                );
            }
        }

        collectExpressionColumns(
                statement.getWhereExpression(),
                requiredColumns
        );

        for (JoinClause joinClause : statement.getJoins()) {
            collectExpressionColumns(
                    joinClause.getCondition(),
                    requiredColumns
            );
        }

        statement.getOrderByItems()
                .forEach(orderByItem ->
                        collectColumnReference(
                                orderByItem.getColumnName(),
                                requiredColumns
                        )
                );

        return requiredColumns;
    }

    private boolean containsAggregateProjection(
            SelectStatement statement
    ) {

        return statement.getSelectItems()
                .stream()
                .map(SelectItem::getExpression)
                .filter(Objects::nonNull)
                .map(value ->
                        value.trim()
                                .toUpperCase(
                                        Locale.ROOT
                                )
                )
                .anyMatch(value ->
                        value.startsWith("COUNT(")
                                || value.startsWith("SUM(")
                                || value.startsWith("AVG(")
                                || value.startsWith("MIN(")
                                || value.startsWith("MAX(")
                );
    }

    private void collectExpressionColumns(
            Expression expression,
            Map<String, Set<String>> target
    ) {

        if (expression == null) {
            return;
        }

        if (expression instanceof ComparisonExpression comparisonExpression) {

            collectColumnExpression(
                    comparisonExpression.getLeftColumnExpression(),
                    target
            );

            if (comparisonExpression.isColumnToColumnComparison()) {
                collectColumnExpression(
                        comparisonExpression.getRightColumnExpression(),
                        target
                );
            }

            return;
        }

        if (expression instanceof com.yekdb.query.expression.BetweenExpression betweenExpression) {
            collectColumnReference(
                    betweenExpression.getColumnName(),
                    target
            );
            return;
        }

        if (expression instanceof com.yekdb.query.expression.InExpression inExpression) {
            collectColumnReference(
                    inExpression.getColumnName(),
                    target
            );
            return;
        }

        if (expression instanceof com.yekdb.query.expression.LikeExpression likeExpression) {
            collectColumnReference(
                    likeExpression.getColumnName(),
                    target
            );
            return;
        }

        if (expression instanceof com.yekdb.query.expression.FunctionComparisonExpression functionExpression) {
            collectFunctionColumns(
                    functionExpression.getLeftFunction(),
                    target
            );

            collectFunctionOperandColumns(
                    functionExpression.getExpectedValue(),
                    target
            );

            return;
        }

        if (expression instanceof LogicalExpression logicalExpression) {
            collectExpressionColumns(
                    logicalExpression.leftExpression(),
                    target
            );
            collectExpressionColumns(
                    logicalExpression.rightExpression(),
                    target
            );
            return;
        }

        if (expression instanceof com.yekdb.query.expression.NotExpression notExpression) {
            collectExpressionColumns(
                    notExpression.expression(),
                    target
            );
        }
    }

    private void collectFunctionColumns(
            com.yekdb.query.expression.FunctionCallExpression functionExpression,
            Map<String, Set<String>> target
    ) {

        for (Object argument : functionExpression.getArguments()) {
            collectFunctionOperandColumns(
                    argument,
                    target
            );
        }
    }

    private void collectFunctionOperandColumns(
            Object operand,
            Map<String, Set<String>> target
    ) {

        if (operand instanceof ColumnExpression columnExpression) {
            collectColumnExpression(
                    columnExpression,
                    target
            );
            return;
        }

        if (operand instanceof com.yekdb.query.expression.FunctionCallExpression functionExpression) {
            collectFunctionColumns(
                    functionExpression,
                    target
            );
        }
    }

    private void collectColumnReference(
            String columnReference,
            Map<String, Set<String>> target
    ) {

        if (columnReference == null
                || columnReference.isBlank()) {
            return;
        }

        String trimmed =
                columnReference.trim();

        if ("*".equals(
                trimmed
        )
                || trimmed.contains(
                "("
        )) {
            return;
        }

        collectColumnExpression(
                ColumnExpression.parse(
                        trimmed
                ),
                target
        );
    }

    private void collectColumnExpression(
            ColumnExpression columnExpression,
            Map<String, Set<String>> target
    ) {

        if (columnExpression == null) {
            return;
        }

        String qualifier =
                columnExpression.isQualified()
                        ? normalizeIdentifier(
                        columnExpression.getQualifier()
                )
                        : "";

        target.computeIfAbsent(
                qualifier,
                ignored -> new LinkedHashSet<>()
        ).add(
                normalizeIdentifier(
                        columnExpression.getColumnName()
                )
        );
    }

    private Set<String> requiredColumnsFor(
            TableReference tableReference,
            Map<String, Set<String>> requiredColumns
    ) {

        if (requiredColumns.isEmpty()) {
            return null;
        }

        Set<String> result =
                new LinkedHashSet<>();

        Set<String> unqualified =
                requiredColumns.get(
                        ""
                );

        if (unqualified != null) {
            result.addAll(
                    unqualified
            );
        }

        Set<String> tableColumns =
                requiredColumns.get(
                        normalizeIdentifier(
                                tableReference.getTableName()
                        )
                );

        if (tableColumns != null) {
            result.addAll(
                    tableColumns
            );
        }

        if (tableReference.hasAlias()) {
            Set<String> aliasColumns =
                    requiredColumns.get(
                            normalizeIdentifier(
                                    tableReference.getAlias()
                            )
                    );

            if (aliasColumns != null) {
                result.addAll(
                        aliasColumns
                );
            }
        }

        return result.isEmpty()
                ? null
                : result;
    }

    private String normalizeIdentifier(
            String identifier
    ) {

        return Objects.requireNonNull(
                        identifier,
                        "Identifier cannot be null."
                )
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
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
     * JOIN öncesinde yalnızca tek tabloya bağlı qualified WHERE
     * predicate'lerini ilgili tablo satırlarına uygular.
     *
     * Final WHERE filtresi korunur; bu nedenle pushdown yalnızca join'e
     * girecek aday satırları azaltır.
     */
    private List<Map<String, Object>> applyPushdownPredicates(
            TableReference tableReference,
            List<Map<String, Object>> rows,
            Expression whereExpression
    ) {

        Objects.requireNonNull(
                tableReference,
                "Table reference cannot be null."
        );

        Objects.requireNonNull(
                rows,
                "Row map list cannot be null."
        );

        List<Expression> predicates =
                collectPushdownPredicates(
                        tableReference,
                        whereExpression
                );

        if (predicates.isEmpty()) {
            return rows;
        }

        List<Map<String, Object>> matchedRows =
                new ArrayList<>();

        for (Map<String, Object> row : rows) {

            boolean matched =
                    true;

            for (Expression predicate : predicates) {

                if (!expressionEvaluator.evaluate(
                        predicate,
                        row
                )) {

                    matched =
                            false;

                    break;
                }
            }

            if (matched) {
                matchedRows.add(
                        row
                );
            }
        }

        return matchedRows;
    }

    private List<Expression> collectPushdownPredicates(
            TableReference tableReference,
            Expression expression
    ) {

        List<Expression> predicates =
                new ArrayList<>();

        collectPushdownPredicates(
                tableReference,
                expression,
                predicates
        );

        return predicates;
    }

    private void collectPushdownPredicates(
            TableReference tableReference,
            Expression expression,
            List<Expression> target
    ) {

        if (expression == null) {
            return;
        }

        if (expression instanceof ComparisonExpression comparisonExpression) {

            Expression unqualifiedPredicate =
                    createPushdownPredicate(
                            tableReference,
                            comparisonExpression
                    );

            if (unqualifiedPredicate != null) {
                target.add(
                        unqualifiedPredicate
                );
            }

            return;
        }

        if (expression instanceof LogicalExpression logicalExpression) {

            if (logicalExpression.operator()
                    != LogicalOperator.AND) {
                return;
            }

            collectPushdownPredicates(
                    tableReference,
                    logicalExpression.leftExpression(),
                    target
            );

            collectPushdownPredicates(
                    tableReference,
                    logicalExpression.rightExpression(),
                    target
            );
        }
    }

    private Expression createPushdownPredicate(
            TableReference tableReference,
            ComparisonExpression comparisonExpression
    ) {

        if (!comparisonExpression.isColumnToValueComparison()) {
            return null;
        }

        ColumnExpression column =
                comparisonExpression.getLeftColumnExpression();

        if (!column.isQualified()
                || !tableReference.matches(
                column.getQualifier()
        )) {

            return null;
        }

        return new ComparisonExpression(
                column.getColumnName(),
                comparisonExpression.operator(),
                comparisonExpression.expectedValue()
        );
    }


    // ==================================================
    // SINGLE-TABLE SELECT PROJECTION
    // ==================================================

    /**
     * Normal single-table SELECT projection işlemini gerçekleştirir.
     *
     * SELECT * kullanılmışsa mevcut schema ve satırlar korunur.
     * Belirli kolonlar seçilmişse yalnızca ilgili kolonlar ve değerler
     * sonuç setine taşınır.
     */
    private SingleTableProjection projectSingleTableRows(
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

        boolean hasFunctionProjection =
                statement.getSelectItems()
                        .stream()
                        .anyMatch(
                                SelectItem::isFunctionExpression
                        );

        if (hasFunctionProjection) {

            SelectFunctionProjectionExecutor.Projection projection =
                    selectFunctionProjectionExecutor.project(
                            table,
                            rows,
                            statement
                    );

            return new SingleTableProjection(
                    projection.columns(),
                    projection.rows()
            );
        }

        if (statement.selectsAllColumns()) {

            return new SingleTableProjection(
                    List.copyOf(
                            table.getColumns()
                    ),
                    new ArrayList<>(
                            rows
                    )
            );
        }

        List<Column> tableColumns =
                table.getColumns();

        List<Column> resultColumns =
                new ArrayList<>();

        List<Integer> selectedIndexes =
                new ArrayList<>();

        for (SelectItem item
                : statement.getSelectItems()) {

            String expression =
                    item.getExpression()
                            .trim();

            int columnIndex =
                    findSingleTableColumnIndex(
                            tableColumns,
                            expression
                    );

            Column sourceColumn =
                    tableColumns.get(
                            columnIndex
                    );

            String outputName =
                    getSingleTableOutputColumnName(
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
                            sourceColumn.getDataType(),
                            sourceColumn.getLength(),
                            sourceColumn.getPrecision(),
                            sourceColumn.getScale(),
                            sourceColumn.getArrayElementType(),
                            sourceColumn.getUserDefinedTypeName()
                    )
            );

            selectedIndexes.add(
                    columnIndex
            );
        }

        List<Row> resultRows =
                new ArrayList<>();

        for (Row row : rows) {

            List<Object> projectedValues =
                    new ArrayList<>();

            for (Integer columnIndex
                    : selectedIndexes) {

                projectedValues.add(
                        row.getValue(
                                columnIndex
                        )
                );
            }

            resultRows.add(
                    new Row(
                            projectedValues
                    )
            );
        }

        return new SingleTableProjection(
                List.copyOf(
                        resultColumns
                ),
                resultRows
        );
    }

    /**
     * Tek tablo SELECT ifadesindeki kolonun fiziksel kolon indexini bulur.
     */
    private int findSingleTableColumnIndex(
            List<Column> columns,
            String columnName
    ) {

        String normalizedName =
                normalizeSingleTableColumnName(
                        columnName
                );

        for (int index = 0;
             index < columns.size();
             index++) {

            Column column =
                    columns.get(
                            index
                    );

            if (column.getName()
                    .equalsIgnoreCase(
                            normalizedName
                    )) {

                return index;
            }
        }

        throw new QueryExecutionException(
                "Column not found: "
                        + columnName
        );
    }

    /**
     * Qualified kolon adını normalize eder.
     *
     * users.name -> name
     * u.name     -> name
     * name       -> name
     */
    private String normalizeSingleTableColumnName(
            String columnName
    ) {

        String normalized =
                Objects.requireNonNull(
                        columnName,
                        "Column name cannot be null."
                ).trim();

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0) {

            normalized =
                    normalized.substring(
                            dotIndex + 1
                    );
        }

        return normalized;
    }

    /**
     * SELECT sonucunda gösterilecek kolon adını belirler.
     */
    private String getSingleTableOutputColumnName(
            SelectItem item
    ) {

        String alias =
                item.getAlias();

        if (alias != null
                && !alias.isBlank()) {

            return alias.trim();
        }

        return normalizeSingleTableColumnName(
                item.getExpression()
        );
    }

    /**
     * Normal single-table SELECT projection sonucu.
     */
    private record SingleTableProjection(
            List<Column> columns,
            List<Row> rows
    ) {
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

            case EMPTY_RESULT ->
                    QueryResult.selectSuccess(
                            table.getColumns(),
                            List.of(),
                            0L
                    );

            case INDEX_SCAN ->
                    throw new UnsupportedOperationException(
                            "INDEX_SCAN requires index context and a row resolver."
                    );
        };
    }

    /**
     * Index context bulunan SELECT execution planını çalıştırır.
     */
    private QueryResult executeIndexAwarePlan(
            QueryPlan queryPlan,
            Table table,
            List<Row> rows,
            List<Index<?>> availableIndexes,
            Function<RecordPointer, Row> rowResolver,
            Expression finalWhereExpression
    ) {

        if (queryPlan.getPlanType()
                == com.yekdb.query.optimizer.QueryPlanType.FULL_TABLE_SCAN) {

            return TableScanExecutor.execute(
                    table,
                    rows,
                    queryPlan.getWhereExpression()
            );
        }

        if (queryPlan.getPlanType()
                == com.yekdb.query.optimizer.QueryPlanType.EMPTY_RESULT) {

            return QueryResult.selectSuccess(
                    table.getColumns(),
                    List.of(),
                    0L
            );
        }

        String indexName =
                queryPlan.getIndexName()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "INDEX_SCAN plan does not contain an index name."
                                )
                        );

        Index<?> selectedIndex =
                availableIndexes
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(index ->
                                index.getMetadata()
                                        .getIndexName()
                                        .equalsIgnoreCase(indexName)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "INDEX_SCAN selected index is not available: "
                                                + indexName
                                )
                        );

        return indexScanExecutor.execute(
                table,
                selectedIndex,
                queryPlan.getWhereExpression(),
                finalWhereExpression,
                rowResolver
        );
    }

}
