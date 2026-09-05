package com.yekdb.query.command;

import java.util.Objects;

/**
 * DROP PROCEDURE SQL komutunu temsil eder.
 */
public final class DropProcedureCommand implements Command {

    private final String procedureName;
    private final boolean ifExists;

    public DropProcedureCommand(String procedureName) {
        this(
                procedureName,
                false
        );
    }

    public DropProcedureCommand(
            String procedureName,
            boolean ifExists
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

        this.ifExists = ifExists;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public boolean isIfExists() {
        return ifExists;
    }

    @Override
    public String toString() {
        return "DropProcedureCommand{" +
                "procedureName='" + procedureName + '\'' +
                ", ifExists=" + ifExists +
                '}';
    }
}
