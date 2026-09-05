package com.yekdb.query.command;

import com.yekdb.procedure.ProcedureParameter;

import java.util.List;
import java.util.Objects;

/**
 * CREATE PROCEDURE SQL komutunu temsil eder.
 */
public final class CreateProcedureCommand implements Command {

    private final String procedureName;
    private final List<ProcedureParameter> parameters;
    private final String body;
    private final boolean replaceExisting;
    private final boolean ifNotExists;

    public CreateProcedureCommand(
            String procedureName,
            List<ProcedureParameter> parameters,
            String body
    ) {
        this(
                procedureName,
                parameters,
                body,
                false,
                false
        );
    }

    public CreateProcedureCommand(
            String procedureName,
            List<ProcedureParameter> parameters,
            String body,
            boolean replaceExisting
    ) {
        this(
                procedureName,
                parameters,
                body,
                replaceExisting,
                false
        );
    }

    public CreateProcedureCommand(
            String procedureName,
            List<ProcedureParameter> parameters,
            String body,
            boolean replaceExisting,
            boolean ifNotExists
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

        this.parameters =
                parameters == null
                        ? List.of()
                        : List.copyOf(parameters);

        this.body = Objects.requireNonNull(
                body,
                "Procedure body cannot be null."
        ).trim();

        if (this.body.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure body cannot be blank."
            );
        }

        this.replaceExisting = replaceExisting;
        this.ifNotExists = ifNotExists;

        if (replaceExisting && ifNotExists) {
            throw new IllegalArgumentException(
                    "CREATE PROCEDURE cannot use both OR REPLACE and IF NOT EXISTS."
            );
        }
    }

    public String getProcedureName() {
        return procedureName;
    }

    public List<ProcedureParameter> getParameters() {
        return parameters;
    }

    public String getBody() {
        return body;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public boolean isIfNotExists() {
        return ifNotExists;
    }

    @Override
    public String toString() {
        return "CreateProcedureCommand{" +
                "procedureName='" + procedureName + '\'' +
                ", parameters=" + parameters +
                ", body='" + body + '\'' +
                ", replaceExisting=" + replaceExisting +
                ", ifNotExists=" + ifNotExists +
                '}';
    }
}
