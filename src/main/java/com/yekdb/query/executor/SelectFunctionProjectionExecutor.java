package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.FunctionCallExpression;
import com.yekdb.query.expression.FunctionValueResolver;
import com.yekdb.query.function.BuiltInFunctions;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Sprint 00-28 Phase 7.
 *
 * Single-table SELECT projection içerisinde scalar SQL function
 * ifadelerini değerlendirir.
 *
 * Örnekler:
 * LOWER(name)
 * UPPER(city) AS city_upper
 * ABS(balance)
 * LENGTH(TRIM(name))
 */
final class SelectFunctionProjectionExecutor {

    private final FunctionValueResolver functionValueResolver =
            new FunctionValueResolver(
                    BuiltInFunctions.createDefaultRegistry()
            );

    Projection project(
            Table table,
            List<Row> rows,
            SelectStatement statement
    ) {
        Objects.requireNonNull(table, "Table cannot be null.");
        Objects.requireNonNull(rows, "Row list cannot be null.");
        Objects.requireNonNull(statement, "SelectStatement cannot be null.");

        List<Column> resultColumns = new ArrayList<>();
        List<SelectValueResolver> valueResolvers = new ArrayList<>();

        for (SelectItem item : statement.getSelectItems()) {
            String outputName = outputName(item);

            boolean duplicate = resultColumns.stream()
                    .anyMatch(column -> column.getName().equalsIgnoreCase(outputName));

            if (duplicate) {
                throw new QueryExecutionException(
                        "Duplicate SELECT result column: " + outputName
                );
            }

            if (item.isFunctionExpression()) {
                FunctionCallExpression functionExpression =
                        item.getFunctionExpression();

                resultColumns.add(
                        new Column(
                                outputName,
                                inferFunctionType(functionExpression, table)
                        )
                );

                valueResolvers.add(
                        rowValues -> functionValueResolver.resolve(
                                functionExpression,
                                rowValues
                        )
                );

            } else {
                int columnIndex = findColumnIndex(
                        table.getColumns(),
                        item.getExpression()
                );

                Column sourceColumn = table.getColumns().get(columnIndex);

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

                valueResolvers.add(
                        rowValues -> rowValues.get(sourceColumn.getName())
                );
            }
        }

        List<Row> resultRows = new ArrayList<>();

        for (Row row : rows) {
            Map<String, Object> rowValues = buildRowValues(
                    table,
                    statement,
                    row
            );

            List<Object> projectedValues = new ArrayList<>();

            for (SelectValueResolver resolver : valueResolvers) {
                projectedValues.add(
                        resolver.resolve(rowValues)
                );
            }

            resultRows.add(
                    new Row(projectedValues)
            );
        }

        return new Projection(
                List.copyOf(resultColumns),
                resultRows
        );
    }

    private Map<String, Object> buildRowValues(
            Table table,
            SelectStatement statement,
            Row row
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        List<Column> columns = table.getColumns();

        for (int index = 0; index < columns.size(); index++) {
            String columnName = columns.get(index).getName();
            Object value = row.getValue(index);

            values.put(columnName, value);
            values.put(table.getTableName() + "." + columnName, value);

            if (statement.hasTableAlias()) {
                values.put(
                        statement.getTableAlias() + "." + columnName,
                        value
                );
            }
        }

        return values;
    }

    private DataType inferFunctionType(
            FunctionCallExpression expression,
            Table table
    ) {
        String name = expression.getFunctionName();

        return switch (name) {
            case "LOWER", "UPPER", "TRIM" -> DataType.STRING;
            case "LENGTH" -> DataType.INT;
            case "ABS" -> inferAbsType(expression, table);
            default -> throw new QueryExecutionException(
                    "Unknown scalar function in SELECT projection: " + name
            );
        };
    }

    private DataType inferAbsType(
            FunctionCallExpression expression,
            Table table
    ) {
        if (expression.getArgumentCount() != 1) {
            throw new QueryExecutionException(
                    "Function ABS expects exactly 1 argument in SELECT projection."
            );
        }

        Object argument = expression.getArguments().get(0);

        if (argument instanceof ColumnExpression columnExpression) {
            int index = findColumnIndex(
                    table.getColumns(),
                    columnExpression.getQualifiedName()
            );

            DataType type = table.getColumns().get(index).getDataType();

            if (type == DataType.INT
                    || type == DataType.LONG
                    || type == DataType.DOUBLE
                    || type == DataType.NUMERIC) {
                return type;
            }

            throw new QueryExecutionException(
                    "ABS requires a numeric column but got: " + type
            );
        }

        if (argument instanceof Integer
                || argument instanceof Short
                || argument instanceof Byte) {
            return DataType.INT;
        }

        if (argument instanceof Long) {
            return DataType.LONG;
        }

        if (argument instanceof Float
                || argument instanceof Double) {
            return DataType.DOUBLE;
        }

        throw new QueryExecutionException(
                "Cannot infer ABS result type from argument: " + argument
        );
    }

    private int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {
        String normalized = normalizeColumnName(columnName);

        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index).getName().equalsIgnoreCase(normalized)) {
                return index;
            }
        }

        throw new QueryExecutionException(
                "Column not found: " + columnName
        );
    }

    private String normalizeColumnName(String columnName) {
        String normalized = Objects.requireNonNull(
                columnName,
                "Column name cannot be null."
        ).trim();

        int dotIndex = normalized.lastIndexOf('.');

        if (dotIndex >= 0) {
            normalized = normalized.substring(dotIndex + 1);
        }

        return normalized;
    }

    private String outputName(SelectItem item) {
        if (item.hasAlias()) {
            return item.getAlias().trim();
        }

        if (!item.isFunctionExpression()) {
            return normalizeColumnName(item.getExpression());
        }

        String generated = item.getExpression()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (generated.isBlank()) {
            return "function_result";
        }

        if (Character.isDigit(generated.charAt(0))) {
            return "function_" + generated;
        }

        return generated;
    }

    @FunctionalInterface
    private interface SelectValueResolver {
        Object resolve(Map<String, Object> rowValues);
    }

    record Projection(
            List<Column> columns,
            List<Row> rows
    ) {
    }
}
