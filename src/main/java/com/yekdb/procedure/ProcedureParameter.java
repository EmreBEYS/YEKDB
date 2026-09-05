package com.yekdb.procedure;

import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.ColumnTypeDefinition;
import com.yekdb.storage.table.ColumnValueValidator;

import java.util.Objects;

/**
 * Stored procedure parametre tanımını temsil eder.
 */
public final class ProcedureParameter {

    private final String name;
    private final ColumnTypeDefinition typeDefinition;

    public ProcedureParameter(
            String name,
            ColumnTypeDefinition typeDefinition
    ) {
        this.name =
                ProcedureNameValidator.validateObjectName(
                        name,
                        "Parameter name"
                );

        this.typeDefinition =
                Objects.requireNonNull(
                        typeDefinition,
                        "Parameter type cannot be null."
                );
    }

    public String getName() {
        return name;
    }

    public ColumnTypeDefinition getTypeDefinition() {
        return typeDefinition;
    }

    public void validateValue(Object value) {
        ColumnValueValidator.validate(
                Column.fromTypeDefinition(
                        name,
                        typeDefinition
                ),
                value
        );
    }

    @Override
    public String toString() {
        return name + " " + typeDefinition.declaration();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ProcedureParameter that)) {
            return false;
        }

        return name.equals(that.name)
                && typeDefinition.declaration()
                .equals(that.typeDefinition.declaration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                typeDefinition.declaration()
        );
    }
}
