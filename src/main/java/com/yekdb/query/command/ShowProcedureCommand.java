package com.yekdb.query.command;

import java.util.Objects;

/**
 * SHOW PROCEDURE procedure_name SQL komutunu temsil eder.
 */
public final class ShowProcedureCommand implements Command {

    private final String procedureName;

    public ShowProcedureCommand(String procedureName) {
        this.procedureName = Objects.requireNonNull(
                procedureName,
                "Procedure name cannot be null."
        ).trim();

        if (this.procedureName.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure name cannot be blank."
            );
        }
    }

    public String getProcedureName() {
        return procedureName;
    }

    @Override
    public String toString() {
        return "ShowProcedureCommand{" +
                "procedureName='" + procedureName + '\'' +
                '}';
    }
}
