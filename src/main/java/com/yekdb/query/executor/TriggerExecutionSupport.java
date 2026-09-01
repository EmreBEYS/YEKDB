package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;
import com.yekdb.trigger.TriggerDefinition;
import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerTiming;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Trigger body çalıştırma ve OLD / NEW bağlamı hazırlama işlemlerini kapsüller.
 */
final class TriggerExecutionSupport {

    private static final int MAX_TRIGGER_DEPTH = 16;

    private final Deque<String> activeTriggers =
            new ArrayDeque<>();

    void executeInsertTriggers(
            Database database,
            Table table,
            InsertCommand command,
            TriggerTiming timing,
            Function<String, ExecuteResult> sqlExecutor
    ) {
        Objects.requireNonNull(database, "Database cannot be null.");
        Objects.requireNonNull(table, "Table cannot be null.");
        Objects.requireNonNull(command, "InsertCommand cannot be null.");
        Objects.requireNonNull(timing, "Trigger timing cannot be null.");
        Objects.requireNonNull(sqlExecutor, "SQL executor cannot be null.");

        List<TriggerDefinition> triggers =
                database.getTriggerCatalog()
                        .findTriggers(
                                table.getTableName(),
                                timing,
                                TriggerEvent.INSERT
                        );

        if (triggers.isEmpty()) {
            return;
        }

        Map<String, Object> newValues =
                createNewValueMap(
                        table,
                        command
                );

        for (TriggerDefinition trigger : triggers) {
            executeTriggerBody(
                    trigger,
                    null,
                    newValues,
                    sqlExecutor
            );
        }
    }

    void executeUpdateTriggers(
            Database database,
            Table table,
            List<RowChange> rowChanges,
            TriggerTiming timing,
            Function<String, ExecuteResult> sqlExecutor
    ) {
        Objects.requireNonNull(database, "Database cannot be null.");
        Objects.requireNonNull(table, "Table cannot be null.");
        Objects.requireNonNull(rowChanges, "Row changes cannot be null.");
        Objects.requireNonNull(timing, "Trigger timing cannot be null.");
        Objects.requireNonNull(sqlExecutor, "SQL executor cannot be null.");

        List<TriggerDefinition> triggers =
                database.getTriggerCatalog()
                        .findTriggers(
                                table.getTableName(),
                                timing,
                                TriggerEvent.UPDATE
                        );

        if (triggers.isEmpty()
                || rowChanges.isEmpty()) {
            return;
        }

        for (RowChange rowChange : rowChanges) {
            Map<String, Object> oldValues =
                    createValueMap(table, rowChange.oldRow());
            Map<String, Object> newValues =
                    createValueMap(table, rowChange.newRow());

            for (TriggerDefinition trigger : triggers) {
                executeTriggerBody(
                        trigger,
                        oldValues,
                        newValues,
                        sqlExecutor
                );
            }
        }
    }

    void executeDeleteTriggers(
            Database database,
            Table table,
            List<Row> oldRows,
            TriggerTiming timing,
            Function<String, ExecuteResult> sqlExecutor
    ) {
        Objects.requireNonNull(database, "Database cannot be null.");
        Objects.requireNonNull(table, "Table cannot be null.");
        Objects.requireNonNull(oldRows, "Old rows cannot be null.");
        Objects.requireNonNull(timing, "Trigger timing cannot be null.");
        Objects.requireNonNull(sqlExecutor, "SQL executor cannot be null.");

        List<TriggerDefinition> triggers =
                database.getTriggerCatalog()
                        .findTriggers(
                                table.getTableName(),
                                timing,
                                TriggerEvent.DELETE
                        );

        if (triggers.isEmpty()
                || oldRows.isEmpty()) {
            return;
        }

        for (Row oldRow : oldRows) {
            Map<String, Object> oldValues =
                    createValueMap(table, oldRow);

            for (TriggerDefinition trigger : triggers) {
                executeTriggerBody(
                        trigger,
                        oldValues,
                        null,
                        sqlExecutor
                );
            }
        }
    }

    private void executeTriggerBody(
            TriggerDefinition trigger,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            Function<String, ExecuteResult> sqlExecutor
    ) {
        enterTrigger(trigger);

        try {
            for (String statement : splitStatements(trigger.getBody())) {
                String resolvedSql =
                        resolveRowReferences(
                                statement,
                                oldValues,
                                newValues
                        );

                try {
                    sqlExecutor.apply(
                            resolvedSql
                    );

                } catch (RuntimeException exception) {
                    throw new QueryExecutionException(
                            "Trigger execution failed: "
                                    + trigger.getTriggerName(),
                            exception
                    );
                }
            }

        } finally {
            activeTriggers.removeLast();
        }
    }

    private void enterTrigger(
            TriggerDefinition trigger
    ) {
        String triggerKey =
                trigger.getTriggerName()
                        .toLowerCase(Locale.ROOT);

        if (activeTriggers.contains(triggerKey)) {
            throw new QueryExecutionException(
                    "Recursive trigger execution detected: "
                            + trigger.getTriggerName()
            );
        }

        if (activeTriggers.size() >= MAX_TRIGGER_DEPTH) {
            throw new QueryExecutionException(
                    "Maximum trigger execution depth exceeded: "
                            + MAX_TRIGGER_DEPTH
                );
        }

        activeTriggers.addLast(triggerKey);
    }

    private Map<String, Object> createNewValueMap(
            Table table,
            InsertCommand command
    ) {
        Map<String, Object> values =
                new LinkedHashMap<>();

        for (Column column : table.getColumns()) {
            int index =
                    findColumnIndex(
                            command.getColumns(),
                            column.getName()
                    );

            if (index < 0) {
                throw new QueryExecutionException(
                        "Missing NEW value for trigger column: "
                                + column.getName()
                );
            }

            values.put(
                    column.getName()
                            .toLowerCase(Locale.ROOT),
                    command.getValues()
                            .get(index)
            );
        }

        return values;
    }

    private Map<String, Object> createValueMap(
            Table table,
            Row row
    ) {
        Objects.requireNonNull(row, "Row cannot be null.");

        Map<String, Object> values =
                new LinkedHashMap<>();

        List<Column> columns =
                table.getColumns();

        for (int index = 0;
             index < columns.size();
             index++) {

            values.put(
                    columns.get(index)
                            .getName()
                            .toLowerCase(Locale.ROOT),
                    row.getValue(index)
            );
        }

        return values;
    }

    private int findColumnIndex(
            List<String> columns,
            String targetColumn
    ) {
        for (int index = 0;
             index < columns.size();
             index++) {

            if (columns.get(index)
                    .equalsIgnoreCase(targetColumn)) {
                return index;
            }
        }

        return -1;
    }

    private List<String> splitStatements(String body) {
        java.util.ArrayList<String> statements =
                new java.util.ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;

        for (int index = 0;
             index < body.length();
             index++) {

            char character =
                    body.charAt(index);

            if (character == '\''
                    && !insideDoubleQuote) {
                insideSingleQuote = !insideSingleQuote;
            } else if (character == '"'
                    && !insideSingleQuote) {
                insideDoubleQuote = !insideDoubleQuote;
            }

            if (character == ';'
                    && !insideSingleQuote
                    && !insideDoubleQuote) {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        addStatement(statements, current);

        return List.copyOf(statements);
    }

    private void addStatement(
            List<String> statements,
            StringBuilder current
    ) {
        String statement =
                current.toString().trim();

        if (!statement.isBlank()) {
            statements.add(statement);
        }
    }

    private String resolveRowReferences(
            String statement,
            Map<String, Object> oldValues,
            Map<String, Object> newValues
    ) {
        StringBuilder resolved =
                new StringBuilder();

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;

        int index = 0;

        while (index < statement.length()) {
            char character =
                    statement.charAt(index);

            if (character == '\''
                    && !insideDoubleQuote) {
                insideSingleQuote = !insideSingleQuote;
                resolved.append(character);
                index++;
                continue;
            }

            if (character == '"'
                    && !insideSingleQuote) {
                insideDoubleQuote = !insideDoubleQuote;
                resolved.append(character);
                index++;
                continue;
            }

            if (!insideSingleQuote
                    && !insideDoubleQuote
                    && matchesRowReferenceStart(statement, index, "NEW.")) {

                Reference reference =
                        parseReference(statement, index, "NEW.");

                appendReferenceValue(
                        resolved,
                        newValues,
                        reference,
                        "NEW"
                );

                index = reference.endIndex();
                continue;
            }

            if (!insideSingleQuote
                    && !insideDoubleQuote
                    && matchesRowReferenceStart(statement, index, "OLD.")) {

                Reference reference =
                        parseReference(statement, index, "OLD.");

                appendReferenceValue(
                        resolved,
                        oldValues,
                        reference,
                        "OLD"
                );

                index = reference.endIndex();
                continue;
            }

            resolved.append(character);
            index++;
        }

        return resolved.toString();
    }

    private boolean matchesRowReferenceStart(
            String statement,
            int index,
            String prefix
    ) {
        if (index > 0
                && isIdentifierCharacter(
                statement.charAt(index - 1)
        )) {
            return false;
        }

        if (index + prefix.length() >= statement.length()) {
            return false;
        }

        return statement.regionMatches(
                true,
                index,
                prefix,
                0,
                prefix.length()
        )
                && isIdentifierCharacter(
                statement.charAt(index + prefix.length())
        );
    }

    private Reference parseReference(
            String statement,
            int index,
            String prefix
    ) {
        int columnStart =
                index + prefix.length();

        int columnEnd =
                columnStart;

        while (columnEnd < statement.length()
                && isIdentifierCharacter(
                statement.charAt(columnEnd)
        )) {
            columnEnd++;
        }

        return new Reference(
                statement.substring(
                        columnStart,
                        columnEnd
                ).toLowerCase(Locale.ROOT),
                columnEnd
        );
    }

    private void appendReferenceValue(
            StringBuilder resolved,
            Map<String, Object> values,
            Reference reference,
            String alias
    ) {
        if (values == null) {
            throw new QueryExecutionException(
                    alias + " values are not available for this trigger event."
            );
        }

        if (!values.containsKey(reference.columnName())) {
            throw new QueryExecutionException(
                    "Unknown " + alias + " column in trigger body: "
                            + reference.columnName()
            );
        }

        resolved.append(
                toSqlLiteral(
                        values.get(reference.columnName())
                )
        );
    }

    private boolean isIdentifierCharacter(char character) {
        return Character.isLetterOrDigit(character)
                || character == '_';
    }

    private String toSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Boolean) {
            return value.toString();
        }

        return "'"
                + value.toString()
                .replace("'", "''")
                + "'";
    }

    record RowChange(
            Row oldRow,
            Row newRow
    ) {
        RowChange {
            Objects.requireNonNull(oldRow, "Old row cannot be null.");
            Objects.requireNonNull(newRow, "New row cannot be null.");
        }
    }

    private record Reference(
            String columnName,
            int endIndex
    ) {
    }
}
