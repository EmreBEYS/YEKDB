package com.yekdb.query.executor;

import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.TableReference;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SELECT sorgularındaki JOIN projection ve JOIN kolon çözümleme
 * sorumluluklarını SelectExecutor'dan ayırır.
 *
 * <p>Bu sınıf query akışını yönetmez. Yalnızca:</p>
 * <ul>
 *     <li>JOIN / multiple JOIN SELECT projection üretir.</li>
 *     <li>Qualified ve unqualified JOIN kolonlarını çözer.</li>
 *     <li>SELECT * için sonuç şemasını oluşturur.</li>
 * </ul>
 *
 * <p>Package-private tutulur; SelectExecutor'ın dahili yardımcı
 * bileşenidir.</p>
 */
final class SelectJoinProjectionExecutor {

    MultiJoinedProjection projectMultiJoinedRows(
            Table baseTable,
            List<Table> rightTables,
            SelectStatement statement,
            List<Map<String, Object>> joinedRows
    ) {

        List<Table> allTables = new ArrayList<>();
        allTables.add(baseTable);
        allTables.addAll(rightTables);

        List<TableReference> references = createMultiJoinReferences(statement);

        if (allTables.size() != references.size()) {
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

        List<Column> resultColumns = new ArrayList<>();
        List<SelectedJoinedColumn> selectedColumns = new ArrayList<>();

        for (SelectItem item : statement.getSelectItems()) {
            MultiJoinedSourceColumn sourceColumn =
                    resolveMultiJoinedSourceColumn(
                            item.getExpression(),
                            allTables,
                            references
                    );

            String outputName = getOutputColumnName(item);
            ensureUniqueResultColumn(resultColumns, outputName);

            resultColumns.add(
                    new Column(
                            outputName,
                            sourceColumn.column().getDataType()
                    )
            );

            selectedColumns.add(
                    new SelectedJoinedColumn(
                            sourceColumn.mapKey(),
                            outputName
                    )
            );
        }

        List<Row> resultRows = new ArrayList<>();

        for (Map<String, Object> joinedRow : joinedRows) {
            List<Object> values = new ArrayList<>();

            for (SelectedJoinedColumn selectedColumn : selectedColumns) {
                if (!SelectColumnResolver.containsKeyIgnoreCase(
                        joinedRow,
                        selectedColumn.mapKey()
                )) {
                    throw new QueryExecutionException(
                            "Column not found in multiple JOIN result: "
                                    + selectedColumn.mapKey()
                    );
                }

                Object value = SelectColumnResolver.getValueIgnoreCase(
                        joinedRow,
                        selectedColumn.mapKey()
                );

                if (value == null) {
                    throw new QueryExecutionException(
                            "Multiple JOIN projection produced a null value for column: "
                                    + selectedColumn.mapKey()
                    );
                }

                values.add(value);
            }

            resultRows.add(new Row(values));
        }

        return new MultiJoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    JoinedProjection projectJoinedRows(
            Table leftTable,
            Table rightTable,
            SelectStatement statement,
            JoinClause joinClause,
            List<Map<String, Object>> joinedRows
    ) {

        TableReference leftReference = statement.getTable();
        TableReference rightReference = new TableReference(
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

        List<Column> resultColumns = new ArrayList<>();
        List<SelectedJoinedColumn> selectedColumns = new ArrayList<>();

        for (SelectItem item : statement.getSelectItems()) {
            JoinedSourceColumn sourceColumn = resolveJoinedSourceColumn(
                    item.getExpression().trim(),
                    leftTable,
                    rightTable,
                    leftReference,
                    rightReference
            );

            String outputName = getOutputColumnName(item);
            ensureUniqueResultColumn(resultColumns, outputName);

            resultColumns.add(
                    new Column(
                            outputName,
                            sourceColumn.column().getDataType()
                    )
            );

            selectedColumns.add(
                    new SelectedJoinedColumn(
                            sourceColumn.mapKey(),
                            outputName
                    )
            );
        }

        List<Row> resultRows = new ArrayList<>();

        for (Map<String, Object> joinedRow : joinedRows) {
            List<Object> values = new ArrayList<>();

            for (SelectedJoinedColumn selectedColumn : selectedColumns) {
                if (!SelectColumnResolver.containsKeyIgnoreCase(
                        joinedRow,
                        selectedColumn.mapKey()
                )) {
                    throw new QueryExecutionException(
                            "Column not found in JOIN result: "
                                    + selectedColumn.mapKey()
                    );
                }

                values.add(
                        SelectColumnResolver.getValueIgnoreCase(
                                joinedRow,
                                selectedColumn.mapKey()
                        )
                );
            }

            resultRows.add(new Row(values));
        }

        return new JoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    MultiJoinedSourceColumn resolveMultiJoinedSourceColumn(
            String expression,
            List<Table> tables,
            List<TableReference> references
    ) {

        String trimmed = Objects.requireNonNull(
                expression,
                "SELECT expression cannot be null."
        ).trim();

        if (trimmed.isEmpty()) {
            throw new QueryExecutionException(
                    "SELECT expression cannot be blank."
            );
        }

        int dotIndex = trimmed.lastIndexOf('.');

        if (dotIndex >= 0) {
            String qualifier = trimmed.substring(0, dotIndex);
            String columnName = trimmed.substring(dotIndex + 1);

            for (int index = 0; index < references.size(); index++) {
                TableReference reference = references.get(index);

                if (!reference.matches(qualifier)) {
                    continue;
                }

                Column column = SelectColumnResolver.findColumnOrNull(
                        tables.get(index).getColumns(),
                        columnName
                );

                if (column == null) {
                    throw new QueryExecutionException(
                            "Column not found: " + expression
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

        MultiJoinedSourceColumn resolved = null;

        for (int index = 0; index < tables.size(); index++) {
            Column column = SelectColumnResolver.findColumnOrNull(
                    tables.get(index).getColumns(),
                    trimmed
            );

            if (column == null) {
                continue;
            }

            if (resolved != null) {
                throw new QueryExecutionException(
                        "Ambiguous column reference: " + expression
                );
            }

            TableReference reference = references.get(index);
            resolved = new MultiJoinedSourceColumn(
                    column,
                    reference.getEffectiveName()
                            + "."
                            + column.getName()
            );
        }

        if (resolved == null) {
            throw new QueryExecutionException(
                    "Column not found: " + expression
            );
        }

        return resolved;
    }

    JoinedSourceColumn resolveJoinedSourceColumn(
            String expression,
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference
    ) {

        String trimmed = Objects.requireNonNull(
                expression,
                "SELECT expression cannot be null."
        ).trim();

        int dotIndex = trimmed.lastIndexOf('.');

        if (dotIndex >= 0) {
            String qualifier = trimmed.substring(0, dotIndex);
            String columnName = trimmed.substring(dotIndex + 1);

            if (leftReference.matches(qualifier)) {
                Column column = SelectColumnResolver.findColumn(
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

            if (rightReference.matches(qualifier)) {
                Column column = SelectColumnResolver.findColumn(
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

        Column leftColumn = SelectColumnResolver.findColumnOrNull(
                leftTable.getColumns(),
                trimmed
        );

        Column rightColumn = SelectColumnResolver.findColumnOrNull(
                rightTable.getColumns(),
                trimmed
        );

        if (leftColumn != null && rightColumn != null) {
            throw new QueryExecutionException(
                    "Ambiguous column reference: " + expression
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
                "Column not found: " + expression
        );
    }

    private MultiJoinedProjection projectAllMultiJoinedColumns(
            List<Table> tables,
            List<TableReference> references,
            List<Map<String, Object>> joinedRows
    ) {
        List<Column> resultColumns = new ArrayList<>();
        List<String> mapKeys = new ArrayList<>();

        for (int index = 0; index < tables.size(); index++) {
            addAllProjectionColumns(
                    resultColumns,
                    mapKeys,
                    tables.get(index),
                    references.get(index)
            );
        }

        List<Row> resultRows = new ArrayList<>();

        for (Map<String, Object> joinedRow : joinedRows) {
            List<Object> values = new ArrayList<>();

            for (String mapKey : mapKeys) {
                if (!SelectColumnResolver.containsKeyIgnoreCase(
                        joinedRow,
                        mapKey
                )) {
                    throw new QueryExecutionException(
                            "Column not found in multiple JOIN result: "
                                    + mapKey
                    );
                }

                Object value = SelectColumnResolver.getValueIgnoreCase(
                        joinedRow,
                        mapKey
                );

                if (value == null) {
                    throw new QueryExecutionException(
                            "Multiple JOIN projection produced a null value for column: "
                                    + mapKey
                    );
                }

                values.add(value);
            }

            resultRows.add(new Row(values));
        }

        return new MultiJoinedProjection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    private JoinedProjection projectAllJoinedColumns(
            Table leftTable,
            Table rightTable,
            TableReference leftReference,
            TableReference rightReference,
            List<Map<String, Object>> joinedRows
    ) {
        List<Column> resultColumns = new ArrayList<>();
        List<String> mapKeys = new ArrayList<>();

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

        List<Row> resultRows = new ArrayList<>();

        for (Map<String, Object> joinedRow : joinedRows) {
            List<Object> values = new ArrayList<>();

            for (String mapKey : mapKeys) {
                values.add(
                        SelectColumnResolver.getValueIgnoreCase(
                                joinedRow,
                                mapKey
                        )
                );
            }

            resultRows.add(new Row(values));
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
        String qualifier = reference.getEffectiveName();

        for (Column column : table.getColumns()) {
            String qualifiedName = qualifier + "." + column.getName();

            resultColumns.add(
                    new Column(
                            qualifiedName,
                            column.getDataType()
                    )
            );

            mapKeys.add(qualifiedName);
        }
    }

    private List<TableReference> createMultiJoinReferences(
            SelectStatement statement
    ) {
        List<TableReference> references = new ArrayList<>();
        references.add(statement.getTable());

        for (JoinClause joinClause : statement.getJoins()) {
            references.add(
                    new TableReference(
                            joinClause.getTableName(),
                            joinClause.getAlias()
                    )
            );
        }

        return List.copyOf(references);
    }

    private String getOutputColumnName(SelectItem item) {
        String alias = item.getAlias();

        if (alias != null && !alias.isBlank()) {
            return alias.trim();
        }

        return item.getExpression().trim();
    }

    private void ensureUniqueResultColumn(
            List<Column> resultColumns,
            String outputName
    ) {
        boolean duplicate = resultColumns.stream()
                .anyMatch(
                        column -> column.getName()
                                .equalsIgnoreCase(outputName)
                );

        if (duplicate) {
            throw new QueryExecutionException(
                    "Duplicate SELECT result column: " + outputName
            );
        }
    }

    record MultiJoinedProjection(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    record MultiJoinedSourceColumn(
            Column column,
            String mapKey
    ) {
    }

    record JoinedProjection(
            List<Column> columns,
            List<Row> rows
    ) {
    }

    record JoinedSourceColumn(
            Column column,
            String mapKey
    ) {
    }

    private record SelectedJoinedColumn(
            String mapKey,
            String outputName
    ) {
    }
}
