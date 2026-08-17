package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.TableReference;
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
 * SELECT sorgularındaki GROUP BY, aggregate ve HAVING işlemlerini
 * ana SelectExecutor akışından ayırır.
 *
 * Sprint 00-17 Query Phase 2C refactoru.
 */
final class SelectAggregateExecutor {

    private static final Pattern AGGREGATE_PATTERN =
            Pattern.compile(
                    "^(COUNT|SUM|AVG|MIN|MAX)\\s*\\(\\s*(\\*|[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)\\s*\\)$",
                    Pattern.CASE_INSENSITIVE
            );

    private final GroupByExecutor groupByExecutor;
    private final AggregateExecutor aggregateExecutor;
    private final ExpressionEvaluator expressionEvaluator;
    private final SelectJoinProjectionExecutor selectJoinProjectionExecutor;

    SelectAggregateExecutor(
            GroupByExecutor groupByExecutor,
            AggregateExecutor aggregateExecutor,
            ExpressionEvaluator expressionEvaluator,
            SelectJoinProjectionExecutor selectJoinProjectionExecutor
    ) {
        this.groupByExecutor = Objects.requireNonNull(groupByExecutor);
        this.aggregateExecutor = Objects.requireNonNull(aggregateExecutor);
        this.expressionEvaluator = Objects.requireNonNull(expressionEvaluator);
        this.selectJoinProjectionExecutor = Objects.requireNonNull(selectJoinProjectionExecutor);
    }

    // ==================================================
    // SPRINT 00-16 - MULTIPLE JOIN + GROUP BY + AGGREGATE
    // ==================================================

    /**
     * Multiple JOIN sonucu oluşan Map satırları üzerinde
     * GROUP BY ve aggregate pipeline'ını yürütür.
     */
    JoinedAggregateResult executeMultiJoinedAggregatePipeline(
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

                SelectJoinProjectionExecutor.MultiJoinedSourceColumn sourceColumn =
                        selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
                                expression,
                                tables,
                                references
                        );

                Object value =
                        SelectColumnResolver.getValueIgnoreCase(
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

                SelectJoinProjectionExecutor.MultiJoinedSourceColumn sourceColumn =
                        selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
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

                SelectJoinProjectionExecutor.MultiJoinedSourceColumn sourceColumn =
                        selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
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
    void validateMultiJoinedAggregateQuery(
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

                    selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
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

                selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
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

                        selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
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

                selectJoinProjectionExecutor.resolveMultiJoinedSourceColumn(
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
    JoinedAggregateResult executeJoinedAggregatePipeline(
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

                SelectJoinProjectionExecutor.JoinedSourceColumn sourceColumn =
                        selectJoinProjectionExecutor.resolveJoinedSourceColumn(
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

                SelectJoinProjectionExecutor.JoinedSourceColumn sourceColumn =
                        selectJoinProjectionExecutor.resolveJoinedSourceColumn(
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

            return SelectColumnResolver.getValueIgnoreCase(
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
    void validateJoinedAggregateQuery(
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
                selectJoinProjectionExecutor.resolveJoinedSourceColumn(
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

    AggregateResult executeAggregatePipeline(
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
                        SelectColumnResolver.findColumnIndex(
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
                        SelectColumnResolver.findColumn(
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
                        SelectColumnResolver.findColumn(
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
    List<Row> applyHaving(
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
    void validateAggregateQuery(
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
                        SelectColumnResolver.normalizeColumnName(
                                item.getExpression()
                        );

                boolean grouped =
                        groupByClause
                                .getColumnNames()
                                .stream()
                                .map(
                                        SelectColumnResolver::normalizeColumnName
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
                SelectColumnResolver.findColumn(
                        table.getColumns(),
                        item.getExpression()
                );
            }
        }
    }

    // ==================================================
    // AGGREGATE DETECTION
    // ==================================================

    boolean containsAggregateExpression(
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


    record JoinedAggregateResult(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    record AggregateResult(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    private record AggregateCall(
            AggregateExecutor.AggregateFunction function,
            String columnName
    ) {
    }
}
