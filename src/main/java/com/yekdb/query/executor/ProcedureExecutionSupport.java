package com.yekdb.query.executor;

import com.yekdb.procedure.ProcedureDefinition;
import com.yekdb.procedure.ProcedureParameter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Stored procedure gövdesi çalıştırma ve parametre bağlama işlemlerini kapsüller.
 */
final class ProcedureExecutionSupport {

    private static final int MAX_PROCEDURE_DEPTH = 16;

    private final Deque<String> activeProcedures =
            new ArrayDeque<>();

    ExecuteResult execute(
            ProcedureDefinition procedure,
            List<Object> arguments,
            Function<String, ExecuteResult> sqlExecutor
    ) {
        Objects.requireNonNull(
                procedure,
                "Procedure definition cannot be null."
        );
        Objects.requireNonNull(
                arguments,
                "Procedure arguments cannot be null."
        );
        Objects.requireNonNull(
                sqlExecutor,
                "SQL executor cannot be null."
        );

        enterProcedure(procedure);

        try {
            Map<String, Object> boundArguments =
                    bindArguments(
                            procedure,
                            arguments
                    );

            return executeBound(
                    procedure,
                    boundArguments,
                    sqlExecutor
            );

        } finally {
            activeProcedures.removeLast();
        }
    }

    ExecuteResult execute(
            ProcedureDefinition procedure,
            Map<String, Object> namedArguments,
            Function<String, ExecuteResult> sqlExecutor
    ) {
        Objects.requireNonNull(
                procedure,
                "Procedure definition cannot be null."
        );
        Objects.requireNonNull(
                namedArguments,
                "Procedure named arguments cannot be null."
        );
        Objects.requireNonNull(
                sqlExecutor,
                "SQL executor cannot be null."
        );

        enterProcedure(procedure);

        try {
            Map<String, Object> boundArguments =
                    bindNamedArguments(
                            procedure,
                            namedArguments
                    );

            return executeBound(
                    procedure,
                    boundArguments,
                    sqlExecutor
            );

        } finally {
            activeProcedures.removeLast();
        }
    }

    private ExecuteResult executeBound(
            ProcedureDefinition procedure,
            Map<String, Object> boundArguments,
            Function<String, ExecuteResult> sqlExecutor
    ) {

            ExecuteResult lastResult = null;
            int affectedRows = 0;

            for (String statement : splitStatements(
                    procedure.getBody()
            )) {
                String resolvedSql =
                        resolveArguments(
                                statement,
                                boundArguments
                        );

                try {
                    lastResult =
                            sqlExecutor.apply(
                                    resolvedSql
                            );

                    if (lastResult != null) {
                        affectedRows += lastResult.getAffectedRows();
                    }

                } catch (RuntimeException exception) {
                    throw new QueryExecutionException(
                            "Procedure execution failed: "
                                    + procedure.getProcedureName(),
                            exception
                    );
                }
            }

            if (lastResult != null
                    && (lastResult.hasColumns()
                    || lastResult.hasRows())) {
                return lastResult;
            }

            return ExecuteResult.success(
                    "Procedure executed successfully: "
                            + procedure.getProcedureName(),
                    affectedRows
            );
    }

    private Map<String, Object> bindArguments(
            ProcedureDefinition procedure,
            List<Object> arguments
    ) {
        List<ProcedureParameter> parameters =
                procedure.getParameters();

        if (parameters.size() != arguments.size()) {
            throw new QueryExecutionException(
                    "Procedure "
                            + procedure.getProcedureName()
                            + " expects "
                            + parameters.size()
                            + " arguments but got "
                            + arguments.size()
                            + "."
            );
        }

        Map<String, Object> boundArguments =
                new LinkedHashMap<>();

        for (int index = 0;
             index < parameters.size();
             index++) {

            ProcedureParameter parameter =
                    parameters.get(index);

            Object value =
                    arguments.get(index);

            try {
                parameter.validateValue(value);

            } catch (IllegalArgumentException exception) {
                throw new QueryExecutionException(
                        "Invalid value for procedure parameter '"
                                + parameter.getName()
                                + "'.",
                        exception
                );
            }

            boundArguments.put(
                    parameter.getName()
                            .toLowerCase(Locale.ROOT),
                    value
            );
        }

        return boundArguments;
    }

    private Map<String, Object> bindNamedArguments(
            ProcedureDefinition procedure,
            Map<String, Object> arguments
    ) {
        List<ProcedureParameter> parameters =
                procedure.getParameters();

        if (parameters.size() != arguments.size()) {
            throw new QueryExecutionException(
                    "Procedure "
                            + procedure.getProcedureName()
                            + " expects "
                            + parameters.size()
                            + " named arguments but got "
                            + arguments.size()
                            + "."
            );
        }

        Map<String, ProcedureParameter> parametersByName =
                new LinkedHashMap<>();

        for (ProcedureParameter parameter : parameters) {
            parametersByName.put(
                    parameter.getName()
                            .toLowerCase(Locale.ROOT),
                    parameter
            );
        }

        for (String argumentName : arguments.keySet()) {
            if (!parametersByName.containsKey(
                    argumentName.toLowerCase(Locale.ROOT)
            )) {
                throw new QueryExecutionException(
                        "Unknown procedure argument: "
                                + argumentName
                );
            }
        }

        Map<String, Object> boundArguments =
                new LinkedHashMap<>();

        for (ProcedureParameter parameter : parameters) {
            String parameterName =
                    parameter.getName()
                            .toLowerCase(Locale.ROOT);

            if (!arguments.containsKey(parameterName)) {
                throw new QueryExecutionException(
                        "Missing procedure argument: "
                                + parameterName
                );
            }

            Object value =
                    arguments.get(parameterName);

            try {
                parameter.validateValue(value);

            } catch (IllegalArgumentException exception) {
                throw new QueryExecutionException(
                        "Invalid value for procedure parameter '"
                                + parameter.getName()
                                + "'.",
                        exception
                );
            }

            boundArguments.put(
                    parameterName,
                    value
            );
        }

        return boundArguments;
    }

    private void enterProcedure(
            ProcedureDefinition procedure
    ) {
        String procedureKey =
                procedure.getProcedureName()
                        .toLowerCase(Locale.ROOT);

        if (activeProcedures.contains(procedureKey)) {
            throw new QueryExecutionException(
                    "Recursive procedure execution detected: "
                            + procedure.getProcedureName()
            );
        }

        if (activeProcedures.size() >= MAX_PROCEDURE_DEPTH) {
            throw new QueryExecutionException(
                    "Maximum procedure execution depth exceeded: "
                            + MAX_PROCEDURE_DEPTH
            );
        }

        activeProcedures.addLast(procedureKey);
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

    private String resolveArguments(
            String statement,
            Map<String, Object> arguments
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
                    && character == ':'
                    && index + 1 < statement.length()
                    && isIdentifierStart(
                    statement.charAt(index + 1)
            )) {

                Reference reference =
                        parseReference(
                                statement,
                                index + 1
                        );

                Object value =
                        arguments.get(
                                reference.name()
                        );

                if (!arguments.containsKey(
                        reference.name()
                )) {
                    throw new QueryExecutionException(
                            "Unknown procedure parameter in body: "
                                    + reference.name()
                    );
                }

                resolved.append(
                        toSqlLiteral(value)
                );

                index = reference.endIndex();
                continue;
            }

            resolved.append(character);
            index++;
        }

        return resolved.toString();
    }

    private Reference parseReference(
            String statement,
            int startIndex
    ) {
        int endIndex =
                startIndex;

        while (endIndex < statement.length()
                && isIdentifierCharacter(
                statement.charAt(endIndex)
        )) {
            endIndex++;
        }

        return new Reference(
                statement.substring(
                        startIndex,
                        endIndex
                ).toLowerCase(Locale.ROOT),
                endIndex
        );
    }

    private boolean isIdentifierStart(char character) {
        return Character.isLetter(character)
                || character == '_';
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

    private record Reference(
            String name,
            int endIndex
    ) {
    }
}
