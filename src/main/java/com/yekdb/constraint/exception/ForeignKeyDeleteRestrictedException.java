package com.yekdb.constraint.exception;

import java.util.List;

/**
 * Sprint 00-25 Phase 6.
 *
 * Parent row başka bir tablodaki FOREIGN KEY tarafından referanslanırken
 * DELETE işlemi yapılmaya çalışıldığında RESTRICT / NO ACTION davranışı
 * kapsamında fırlatılır.
 */
public final class ForeignKeyDeleteRestrictedException
        extends ConstraintViolationException {

    private final String parentTableName;
    private final List<String> parentColumns;
    private final String referencingTableName;
    private final List<String> referencingColumns;
    private final List<Object> values;

    public ForeignKeyDeleteRestrictedException(
            String parentTableName,
            List<String> parentColumns,
            String referencingTableName,
            List<String> referencingColumns,
            List<Object> values
    ) {
        super(
                "DELETE restricted by FOREIGN KEY. Row "
                        + parentTableName
                        + parentColumns
                        + " with values "
                        + values
                        + " is referenced by "
                        + referencingTableName
                        + referencingColumns
        );

        this.parentTableName = parentTableName;
        this.parentColumns = List.copyOf(parentColumns);
        this.referencingTableName = referencingTableName;
        this.referencingColumns = List.copyOf(referencingColumns);
        this.values = List.copyOf(values);
    }

    public String getParentTableName() {
        return parentTableName;
    }

    public List<String> getParentColumns() {
        return parentColumns;
    }

    public String getReferencingTableName() {
        return referencingTableName;
    }

    public List<String> getReferencingColumns() {
        return referencingColumns;
    }

    public List<Object> getValues() {
        return values;
    }
}
