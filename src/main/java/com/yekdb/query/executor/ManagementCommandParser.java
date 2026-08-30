package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.query.command.*;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.ColumnTypeDefinition;
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
 * FOREIGN KEY (user_id) REFERENCES users(id)
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

        if (upperSql.startsWith("ALTER TABLE ")) {
            return parseAlterTableCommand(sql);
        }

        throw new QueryExecutionException(
                "Unsupported SQL statement: " + sql
        );
    }


    private AlterTableCommand parseAlterTableCommand(String sql) {
        String remaining = sql.substring("ALTER TABLE".length()).trim();

        int firstSpace = remaining.indexOf(' ');
        if (firstSpace <= 0) {
            throw new QueryExecutionException(
                    "ALTER TABLE statement must contain a table name and action."
            );
        }

        String tableName = remaining.substring(0, firstSpace).trim();
        String actionText = remaining.substring(firstSpace + 1).trim();

        if (tableName.isBlank() || actionText.isBlank()) {
            throw new QueryExecutionException(
                    "ALTER TABLE statement must contain a table name and action."
            );
        }

        String upperAction = actionText.toUpperCase(Locale.ROOT);

        if (upperAction.startsWith("ADD COLUMN ")) {
            String definition = actionText.substring("ADD COLUMN".length()).trim();
            String[] parts = definition.split("\\s+");
            if (parts.length != 2) {
                throw new QueryExecutionException(
                        "ADD COLUMN must use 'ADD COLUMN name type' syntax."
                );
            }
            parseDataType(parts[1]);
            return new AlterTableCommand(
                    tableName,
                    new AddColumnAlterAction(parts[0], parts[1])
            );
        }

        if (upperAction.startsWith("DROP COLUMN ")) {
            String columnName = extractSingleAlterValue(actionText, "DROP COLUMN");
            return new AlterTableCommand(
                    tableName,
                    new DropColumnAlterAction(columnName)
            );
        }

        if (upperAction.startsWith("RENAME COLUMN ")) {
            String rename = actionText.substring("RENAME COLUMN".length()).trim();
            String[] parts = rename.split("(?i)\\s+TO\\s+", -1);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()
                    || parts[0].trim().contains(" ") || parts[1].trim().contains(" ")) {
                throw new QueryExecutionException(
                        "RENAME COLUMN must use 'RENAME COLUMN old_name TO new_name' syntax."
                );
            }
            return new AlterTableCommand(
                    tableName,
                    new RenameColumnAlterAction(parts[0].trim(), parts[1].trim())
            );
        }

        if (upperAction.startsWith("RENAME TO ")) {
            String newTableName = extractSingleAlterValue(actionText, "RENAME TO");
            return new AlterTableCommand(
                    tableName,
                    new RenameTableAlterAction(newTableName)
            );
        }

        if (upperAction.startsWith("ALTER COLUMN ")) {
            String alter = actionText.substring("ALTER COLUMN".length()).trim();
            String[] parts = alter.split("\\s+", 2);
            if (parts.length != 2 || parts[0].isBlank()) {
                throw new QueryExecutionException(
                        "ALTER COLUMN requires a column name and operation."
                );
            }

            String operation = parts[1].trim().toUpperCase(Locale.ROOT);
            if (operation.equals("SET NOT NULL")) {
                return new AlterTableCommand(
                        tableName,
                        new AlterColumnSetNotNullAction(parts[0])
                );
            }
            if (operation.equals("DROP NOT NULL")) {
                return new AlterTableCommand(
                        tableName,
                        new AlterColumnDropNotNullAction(parts[0])
                );
            }

            throw new QueryExecutionException(
                    "Unsupported ALTER COLUMN operation: " + parts[1].trim()
            );
        }

        if (upperAction.startsWith("ADD ")) {
            String constraintDefinition = actionText.substring("ADD".length()).trim();
            String constraintName = null;
            String upperConstraint = constraintDefinition.toUpperCase(Locale.ROOT);

            if (upperConstraint.startsWith("CONSTRAINT ")) {
                String namedDefinition =
                        constraintDefinition.substring("CONSTRAINT".length()).trim();

                int nameEnd = namedDefinition.indexOf(' ');

                if (nameEnd <= 0 || nameEnd == namedDefinition.length() - 1) {
                    throw new QueryExecutionException(
                            "ADD CONSTRAINT requires a constraint name and definition."
                    );
                }

                constraintName = namedDefinition.substring(0, nameEnd).trim();
                constraintDefinition = namedDefinition.substring(nameEnd + 1).trim();
                upperConstraint = constraintDefinition.toUpperCase(Locale.ROOT);
            }

            Constraint constraint;

            try {
                if (upperConstraint.startsWith("PRIMARY KEY")) {
                    constraint = new PrimaryKeyConstraint(
                            constraintName,
                            parseConstraintColumnList(constraintDefinition, "PRIMARY KEY")
                    );
                } else if (upperConstraint.startsWith("UNIQUE")) {
                    constraint = new UniqueConstraint(
                            constraintName,
                            parseConstraintColumnList(constraintDefinition, "UNIQUE")
                    );
                } else if (upperConstraint.startsWith("FOREIGN KEY")) {
                    constraint = parseForeignKeyConstraint(
                            constraintDefinition,
                            constraintName
                    );
                } else {
                    throw new QueryExecutionException(
                            "Unsupported ALTER TABLE ADD operation: " + constraintDefinition
                    );
                }
            } catch (IllegalArgumentException exception) {
                throw new QueryExecutionException(
                        "Invalid ALTER TABLE constraint: "
                                + constraintDefinition,
                        exception
                );
            }

            return new AlterTableCommand(
                    tableName,
                    new AddConstraintAlterAction(constraint)
            );
        }

        if (upperAction.startsWith("DROP CONSTRAINT ")) {
            String constraintName = extractSingleAlterValue(actionText, "DROP CONSTRAINT");
            return new AlterTableCommand(
                    tableName,
                    new DropConstraintAlterAction(constraintName)
            );
        }

        throw new QueryExecutionException(
                "Unsupported ALTER TABLE action: " + actionText
        );
    }

    private String extractSingleAlterValue(
            String actionText,
            String keyword
    ) {
        String value = actionText.substring(keyword.length()).trim();

        if (value.isBlank() || value.chars().anyMatch(Character::isWhitespace)) {
            throw new QueryExecutionException(
                    keyword + " requires exactly one name."
            );
        }

        return value;
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

            if (upper.startsWith("FOREIGN KEY")) {
                constraints.add(
                        parseForeignKeyConstraint(definition)
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
        ColumnTypeDefinition typeDefinition = parseColumnType(parts[1]);

        columns.add(
                Column.fromTypeDefinition(
                        columnName,
                        typeDefinition
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


    private ForeignKeyConstraint parseForeignKeyConstraint(
            String definition
    ) {
        return parseForeignKeyConstraint(definition, null);
    }

    private ForeignKeyConstraint parseForeignKeyConstraint(
            String definition,
            String constraintName
    ) {
        String remaining = definition.substring(
                "FOREIGN KEY".length()
        ).trim();

        if (!remaining.startsWith("(")) {
            throw new QueryExecutionException(
                    "FOREIGN KEY constraint must start with a local column list: "
                            + definition
            );
        }

        int localCloseIndex = remaining.indexOf(')');

        if (localCloseIndex <= 1) {
            throw new QueryExecutionException(
                    "FOREIGN KEY constraint must contain at least one local column: "
                            + definition
            );
        }

        String localColumnsPart = remaining.substring(
                1,
                localCloseIndex
        ).trim();

        List<String> localColumns = parseColumnNames(
                localColumnsPart,
                "FOREIGN KEY"
        );

        String referenceSection = remaining.substring(
                localCloseIndex + 1
        ).trim();

        String referenceUpper = referenceSection.toUpperCase(Locale.ROOT);

        if (!referenceUpper.startsWith("REFERENCES")) {
            throw new QueryExecutionException(
                    "FOREIGN KEY constraint must contain REFERENCES: "
                            + definition
            );
        }

        String referenceTarget = referenceSection.substring(
                "REFERENCES".length()
        ).trim();

        int referenceOpenIndex = referenceTarget.indexOf('(');
        int referenceCloseIndex = referenceOpenIndex < 0
                ? -1
                : referenceTarget.indexOf(')', referenceOpenIndex + 1);

        if (referenceOpenIndex <= 0
                || referenceCloseIndex <= referenceOpenIndex) {
            throw new QueryExecutionException(
                    "REFERENCES must use 'table(column, ...)' syntax: "
                            + definition
            );
        }

        String referencedTableName = referenceTarget.substring(
                0,
                referenceOpenIndex
        ).trim();

        if (referencedTableName.isBlank()
                || referencedTableName.chars().anyMatch(Character::isWhitespace)) {
            throw new QueryExecutionException(
                    "FOREIGN KEY must reference a valid table name: "
                            + definition
            );
        }

        String referencedColumnsPart = referenceTarget.substring(
                referenceOpenIndex + 1,
                referenceCloseIndex
        ).trim();

        List<String> referencedColumns = parseColumnNames(
                referencedColumnsPart,
                "FOREIGN KEY REFERENCES"
        );

        if (localColumns.size() != referencedColumns.size()) {
            throw new QueryExecutionException(
                    "FOREIGN KEY local and referenced column counts must match: "
                            + definition
            );
        }

        String referentialActionSection = referenceTarget.substring(
                referenceCloseIndex + 1
        ).trim();

        ReferentialActions referentialActions = parseReferentialActions(
                referentialActionSection,
                definition
        );

        try {
            return new ForeignKeyConstraint(
                    constraintName,
                    localColumns,
                    referencedTableName,
                    referencedColumns,
                    referentialActions.onDelete(),
                    referentialActions.onUpdate()
            );
        } catch (IllegalArgumentException exception) {
            throw new QueryExecutionException(
                    "Invalid FOREIGN KEY constraint: " + definition,
                    exception
            );
        }
    }

    private ReferentialActions parseReferentialActions(
            String actionSection,
            String definition
    ) {
        ReferentialAction onDelete = ReferentialAction.RESTRICT;
        ReferentialAction onUpdate = ReferentialAction.RESTRICT;

        if (actionSection == null || actionSection.isBlank()) {
            return new ReferentialActions(onDelete, onUpdate);
        }

        String[] tokens = actionSection.trim().split("\\s+");
        boolean deleteDefined = false;
        boolean updateDefined = false;
        int index = 0;

        while (index < tokens.length) {
            if (!tokens[index].equalsIgnoreCase("ON") || index + 2 >= tokens.length) {
                throw invalidReferentialActionSyntax(definition);
            }

            String event = tokens[index + 1].toUpperCase(Locale.ROOT);
            boolean isDelete = event.equals("DELETE");
            boolean isUpdate = event.equals("UPDATE");

            if (!isDelete && !isUpdate) {
                throw invalidReferentialActionSyntax(definition);
            }

            if (isDelete && deleteDefined) {
                throw new QueryExecutionException(
                        "FOREIGN KEY cannot define ON DELETE more than once: "
                                + definition
                );
            }

            if (isUpdate && updateDefined) {
                throw new QueryExecutionException(
                        "FOREIGN KEY cannot define ON UPDATE more than once: "
                                + definition
                );
            }

            String actionToken = tokens[index + 2].toUpperCase(Locale.ROOT);
            ReferentialAction action;
            int consumed;

            switch (actionToken) {
                case "CASCADE" -> {
                    action = ReferentialAction.CASCADE;
                    consumed = 3;
                }
                case "RESTRICT" -> {
                    action = ReferentialAction.RESTRICT;
                    consumed = 3;
                }
                case "SET" -> {
                    if (index + 3 >= tokens.length
                            || !tokens[index + 3].equalsIgnoreCase("NULL")) {
                        throw invalidReferentialActionSyntax(definition);
                    }
                    action = ReferentialAction.SET_NULL;
                    consumed = 4;
                }
                default -> throw invalidReferentialActionSyntax(definition);
            }

            if (isDelete) {
                onDelete = action;
                deleteDefined = true;
            } else {
                onUpdate = action;
                updateDefined = true;
            }

            index += consumed;
        }

        return new ReferentialActions(onDelete, onUpdate);
    }

    private QueryExecutionException invalidReferentialActionSyntax(
            String definition
    ) {
        return new QueryExecutionException(
                "FOREIGN KEY referential action must use "
                        + "'ON DELETE|UPDATE CASCADE|RESTRICT|SET NULL' syntax: "
                        + definition
        );
    }

    private record ReferentialActions(
            ReferentialAction onDelete,
            ReferentialAction onUpdate
    ) {
    }

    private List<String> parseColumnNames(
            String content,
            String constraintName
    ) {
        if (content.isBlank()) {
            throw new QueryExecutionException(
                    constraintName + " constraint must contain at least one column."
            );
        }

        List<String> columns = new ArrayList<>();

        for (String part : content.split(",", -1)) {
            String columnName = part.trim();

            if (columnName.isBlank()) {
                throw new QueryExecutionException(
                        "Invalid empty column in "
                                + constraintName
                                + " constraint."
                );
            }

            columns.add(columnName);
        }

        return List.copyOf(columns);
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

        return parseColumnNames(
                content,
                keyword
        );
    }

    private DataType parseDataType(String value) {
        return parseColumnType(value).dataType();
    }

    private ColumnTypeDefinition parseColumnType(String value) {
        try {
            return ColumnTypeDefinition.parse(value);
        } catch (RuntimeException exception) {
            throw new QueryExecutionException(
                    "Unsupported data type: " + value,
                    exception
            );
        }
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
