package com.yekdb.query.command;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CALL procedure_name(...) SQL komutunu temsil eder.
 */
public final class CallProcedureCommand implements Command {

    private final String procedureName;
    private final List<Object> arguments;
    private final Map<String, Object> namedArguments;

    public CallProcedureCommand(
            String procedureName,
            List<Object> arguments
    ) {
        this(
                procedureName,
                arguments,
                null
        );
    }

    public CallProcedureCommand(
            String procedureName,
            Map<String, Object> namedArguments
    ) {
        this(
                procedureName,
                null,
                namedArguments
        );
    }

    private CallProcedureCommand(
            String procedureName,
            List<Object> arguments,
            Map<String, Object> namedArguments
    ) {
        this.procedureName = Objects.requireNonNull(
                procedureName,
                "Procedure name cannot be null."
        ).trim();

        if (this.procedureName.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure name cannot be blank."
            );
        }

        this.arguments =
                arguments == null
                        ? List.of()
                        : Collections.unmodifiableList(
                        new ArrayList<>(arguments)
                );

        this.namedArguments =
                namedArguments == null
                        ? Map.of()
                        : Collections.unmodifiableMap(
                        new LinkedHashMap<>(namedArguments)
                );
    }

    public String getProcedureName() {
        return procedureName;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public boolean hasNamedArguments() {
        return !namedArguments.isEmpty();
    }

    public Map<String, Object> getNamedArguments() {
        return namedArguments;
    }

    @Override
    public String toString() {
        return "CallProcedureCommand{" +
                "procedureName='" + procedureName + '\'' +
                ", arguments=" + arguments +
                ", namedArguments=" + namedArguments +
                '}';
    }
}
