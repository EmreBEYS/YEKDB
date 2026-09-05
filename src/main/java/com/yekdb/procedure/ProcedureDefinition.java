package com.yekdb.procedure;

import java.util.List;
import java.util.Objects;

/**
 * YEKDB stored procedure tanımını temsil eder.
 */
public final class ProcedureDefinition {

    private final String procedureName;
    private final List<ProcedureParameter> parameters;
    private final String body;

    public ProcedureDefinition(
            String procedureName,
            List<ProcedureParameter> parameters,
            String body
    ) {
        this.procedureName =
                ProcedureNameValidator.validate(
                        procedureName
                );

        this.parameters =
                parameters == null
                        ? List.of()
                        : List.copyOf(parameters);

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure body cannot be null or blank."
            );
        }

        this.body = body.trim();
    }

    public String getProcedureName() {
        return procedureName;
    }

    public List<ProcedureParameter> getParameters() {
        return parameters;
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public String getBody() {
        return body;
    }

    public String getSignature() {
        if (parameters.isEmpty()) {
            return procedureName + "()";
        }

        return procedureName
                + "("
                + String.join(
                ", ",
                parameters.stream()
                        .map(ProcedureParameter::toString)
                        .toList()
        )
                + ")";
    }

    @Override
    public String toString() {
        return "ProcedureDefinition{" +
                "procedureName='" + procedureName + '\'' +
                ", parameters=" + parameters +
                ", body='" + body + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ProcedureDefinition that)) {
            return false;
        }

        return procedureName.equals(that.procedureName)
                && parameters.equals(that.parameters)
                && body.equals(that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                procedureName,
                parameters,
                body
        );
    }
}
