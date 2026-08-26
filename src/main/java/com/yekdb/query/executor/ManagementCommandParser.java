package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * QueryExecutor'ın yönetim SQL parser yolu.
 *
 * Sprint 00-24 Phase 5:
 *
 * CREATE TABLE için aşağıdaki constraint syntax'ları desteklenir:
 *
 * id INT PRIMARY KEY
 * username STRING UNIQUE
 * email STRING NOT NULL
 * PRIMARY KEY (student_id, course_id)
 * UNIQUE (first_name, last_name)
 */
final class ManagementCommandParser {

    Command parse(String sql) {
        String upperSql = sql.toUpperCase(Locale.ROOT);

        if (upperSql.startsWith("CREATE DATABASE ")) {
            return new CreateDatabaseCommand(
                    extractValueAfterKeyword(sql, "CREATE DATABASE")
            );
        }

        if (upperSql.startsWith("USE DATABASE ")) {
            return new UseDatabaseCommand(
                    extractValueAfterKeyword(sql, "USE DATABASE")
            );
        }

        if (upperSql.startsWith("USE ")) {
            return new UseDatabaseCommand(
                    extractValueAfterKeyword(sql, "USE")
            );
        }

        if (upperSql.startsWith("DROP DATABASE ")) {
            return new DropDatabaseCommand(
                    extractValueAfterKeyword(sql, "DROP DATABASE")
            );
        }

        if (upperSql.startsWith("CREATE TABLE ")) {
            return parseCreateTableCommand(sql);
        }

        if (upperSql.startsWith("DROP TABLE ")) {
            return new DropTableCommand(
                    extractValueAfterKeyword(sql, "DROP TABLE")
            );
        }

        throw new QueryExecutionException(
                "Unsupported SQL statement: " + sql
        );
    }

    private CreateTableCommand parseCreateTableCommand(String sql) {
        int openParenthesisIndex = sql.indexOf('(');
        int closeParenthesisIndex = sql.lastIndexOf(')');

        if (openParenthesisIndex < 0
                || closeParenthesisIndex < 0
                || closeParenthesisIndex <= openParenthesisIndex) {
            throw new QueryExecutionException(
                    "Invalid CREATE TABLE statement: " + sql
            );
        }

        String tableName = sql.substring(
                "CREATE TABLE".length(),
                openParenthesisIndex
        ).trim();

        if (tableName.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain a table name."
            );
        }

        String definitionSection = sql.substring(
                openParenthesisIndex + 1,
                closeParenthesisIndex
        ).trim();

        if (definitionSection.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain columns."
            );
        }

        String remainingText = sql.substring(
                closeParenthesisIndex + 1
        ).trim();

        if (!remainingText.isEmpty()) {
            throw new QueryExecutionException(
                    "Unexpected text after CREATE TABLE definition: "
                            + remainingText
            );
        }

        ParsedTableDefinition parsed =
                parseTableDefinitions(definitionSection);

        return new CreateTableCommand(
                tableName,
                parsed.columns(),
                parsed.constraints()
        );
    }

    /**
     * CREATE TABLE içindeki tanımları yalnızca top-level virgüllerden böler.
     * Böylece PRIMARY KEY(a,b) ve UNIQUE(a,b) bozulmaz.
     */
    private List<String> splitTopLevelDefinitions(String section) {
        List<String> definitions = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (int index = 0; index < section.length(); index++) {
            char character = section.charAt(index);

            if (character == '(') {
                depth++;
                current.append(character);
                continue;
            }

            if (character == ')') {
                depth--;

                if (depth < 0) {
                    throw new QueryExecutionException(
                            "Unbalanced parentheses in CREATE TABLE definition."
                    );
                }

                current.append(character);
                continue;
            }

            if (character == ',' && depth == 0) {
                addDefinition(definitions, current);
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        if (depth != 0) {
            throw new QueryExecutionException(
                    "Unbalanced parentheses in CREATE TABLE definition."
            );
        }

        addDefinition(definitions, current);
        return List.copyOf(definitions);
    }

    private void addDefinition(
            List<String> definitions,
            StringBuilder current
    ) {
        String value = current.toString().trim();

        if (value.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE definition cannot be blank."
            );
        }

        definitions.add(value);
    }

    private ParsedTableDefinition parseTableDefinitions(String section) {
        List<Column> columns = new ArrayList<>();
        List<Constraint> constraints = new ArrayList<>();

        for (String definition : splitTopLevelDefinitions(section)) {
            String upper = definition.toUpperCase(Locale.ROOT);

            if (upper.startsWith("PRIMARY KEY")) {
                constraints.add(
                        new PrimaryKeyConstraint(
                                parseConstraintColumnList(
                                        definition,
                                        "PRIMARY KEY"
                                )
                        )
                );
                continue;
            }

            if (upper.startsWith("UNIQUE")) {
                constraints.add(
                        new UniqueConstraint(
                                parseConstraintColumnList(
                                        definition,
                                        "UNIQUE"
                                )
                        )
                );
                continue;
            }

            parseColumnDefinition(
                    definition,
                    columns,
                    constraints
            );
        }

        if (columns.isEmpty()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain valid columns."
            );
        }

        return new ParsedTableDefinition(
                List.copyOf(columns),
                List.copyOf(constraints)
        );
    }

    private void parseColumnDefinition(
            String definition,
            List<Column> columns,
            List<Constraint> constraints
    ) {
        String[] parts = definition.trim().split("\\s+");

        if (parts.length < 2) {
            throw new QueryExecutionException(
                    "Invalid column definition: " + definition
            );
        }

        String columnName = parts[0];
        DataType dataType = parseDataType(parts[1]);

        columns.add(
                new Column(
                        columnName,
                        dataType
                )
        );

        int index = 2;

        while (index < parts.length) {
            String token = parts[index].toUpperCase(Locale.ROOT);

            if ("NOT".equals(token)) {
                if (index + 1 >= parts.length
                        || !"NULL".equalsIgnoreCase(parts[index + 1])) {
                    throw new QueryExecutionException(
                            "Expected NULL after NOT in column definition: "
                                    + definition
                    );
                }

                constraints.add(
                        new NotNullConstraint(columnName)
                );
                index += 2;
                continue;
            }

            if ("UNIQUE".equals(token)) {
                constraints.add(
                        new UniqueConstraint(columnName)
                );
                index++;
                continue;
            }

            if ("PRIMARY".equals(token)) {
                if (index + 1 >= parts.length
                        || !"KEY".equalsIgnoreCase(parts[index + 1])) {
                    throw new QueryExecutionException(
                            "Expected KEY after PRIMARY in column definition: "
                                    + definition
                    );
                }

                constraints.add(
                        new PrimaryKeyConstraint(columnName)
                );
                index += 2;
                continue;
            }

            throw new QueryExecutionException(
                    "Unsupported column constraint in definition: "
                            + definition
            );
        }
    }

    private List<String> parseConstraintColumnList(
            String definition,
            String keyword
    ) {
        String remaining = definition.substring(
                keyword.length()
        ).trim();

        if (!remaining.startsWith("(") || !remaining.endsWith(")")) {
            throw new QueryExecutionException(
                    keyword + " constraint must use '(column, ...)' syntax: "
                            + definition
            );
        }

        String content = remaining.substring(
                1,
                remaining.length() - 1
        ).trim();

        if (content.isBlank()) {
            throw new QueryExecutionException(
                    keyword + " constraint must contain at least one column."
            );
        }

        List<String> columns = new ArrayList<>();

        for (String part : content.split(",")) {
            String columnName = part.trim();

            if (columnName.isBlank()) {
                throw new QueryExecutionException(
                        "Invalid empty column in " + keyword + " constraint."
                );
            }

            columns.add(columnName);
        }

        return List.copyOf(columns);
    }

    private DataType parseDataType(String value) {
        String normalizedType = value
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (normalizedType) {
            case "INT", "INTEGER" -> DataType.INT;
            case "LONG", "BIGINT" -> DataType.LONG;
            case "DOUBLE", "FLOAT", "REAL" -> DataType.DOUBLE;
            case "BOOLEAN", "BOOL" -> DataType.BOOLEAN;
            case "STRING", "TEXT", "VARCHAR" -> DataType.STRING;
            default -> throw new QueryExecutionException(
                    "Unsupported data type: " + value
            );
        };
    }

    private String extractValueAfterKeyword(
            String sql,
            String keyword
    ) {
        String value = sql.substring(keyword.length()).trim();

        if (value.isBlank()) {
            throw new QueryExecutionException(
                    keyword + " statement requires a name."
            );
        }

        if (value.contains(" ")) {
            throw new QueryExecutionException(
                    "Invalid value after "
                            + keyword
                            + ": "
                            + value
            );
        }

        return value;
    }

    private record ParsedTableDefinition(
            List<Column> columns,
            List<Constraint> constraints
    ) {
    }
}
