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
    // SPRINT 00-15 - JOIN-AWARE SELECT PIPELINE
    // ==================================================

    /**
     * Sprint 00-15 JOIN-aware SELECT execution.
     *
     * Bu overload tek bir INNER JOIN'i destekler.
     * Çoklu JOIN ve JOIN + aggregate/group desteği
     * sonraki JOIN sprintlerinde genişletilecektir.
     *
     * Execution sırası:
     *
     * 1 - INNER JOIN
     * 2 - WHERE
     * 3 - SELECT projection
     * 4 - ORDER BY
     * 5 - LIMIT / FETCH
     * 6 - QueryResult
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

        if (!statement.hasJoins()) {

            return executeStatement(
                    leftTable,
                    leftRows,
                    statement
            );
        }

        if (statement.getJoinCount() != 1) {

            throw new QueryExecutionException(
                    "Sprint 00-15 supports exactly one JOIN per SELECT statement."
            );
        }

        boolean containsAggregate =
                containsAggregateExpression(
                        statement.getSelectItems()
                );

        if (statement.hasGroupBy()
                || statement.hasHaving()
                || containsAggregate) {

            throw new QueryExecutionException(
                    "Sprint 00-15 JOIN foundation does not yet support "
                            + "JOIN with GROUP BY, HAVING or aggregate expressions."
            );
        }

        long startTime =
                System.nanoTime();

        JoinClause joinClause =
                statement.getJoins()
                        .get(0);

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
                        statement.getTable(),
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

        // ----------------------------------------------
        // 3 - SELECT projection
        // ----------------------------------------------

        JoinedProjection projection =
                projectJoinedRows(
                        leftTable,
                        rightTable,
                        statement,
                        joinClause,
                        joinedMaps
                );

        List<Column> currentColumns =
                new ArrayList<>(
                        projection.columns()
                );

        List<Row> currentRows =
                new ArrayList<>(
                        projection.rows()
                );

        // ----------------------------------------------
        // 4 - ORDER BY
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
        // 5 - LIMIT
        // ----------------------------------------------

        if (statement.hasLimit()) {

            currentRows =
                    limitExecutor.execute(
                            currentRows,
                            statement.getLimitClause()
                    );
        }

        // ----------------------------------------------
        // 5 - FETCH
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