package com.yekdb.constraint.exception;

import java.util.List;

/**
 * Sprint 00-27 Phase 6.
 *
 * A referenced key cannot be changed when an incoming FOREIGN KEY uses
 * ON UPDATE RESTRICT and at least one active child row still references
 * the old key value.
 */
public final class ForeignKeyUpdateRestrictedException
        extends ConstraintViolationException {

    private final String parentTableName;
    private final List<String> parentColumns;
    private final String referencingTableName;
    private final List<String> referencingColumns;
    private final List<Object> oldValues;
    private final List<Object> newValues;

    public ForeignKeyUpdateRestrictedException(
            String parentTableName,
            List<String> parentColumns,
            String referencingTableName,
            List<String> referencingColumns,
            List<Object> oldValues,
            List<Object> newValues
    ) {
        super(
                "UPDATE restricted by FOREIGN KEY. Key "
                        + parentTableName
                        + parentColumns
                        + " cannot change from "
                        + oldValues
                        + " to "
                        + newValues
                        + " because it is referenced by "
                        + referencingTableName
                        + referencingColumns
        );

        this.parentTableName = parentTableName;
        this.parentColumns = List.copyOf(parentColumns);
        this.referencingTableName = referencingTableName;
        this.referencingColumns = List.copyOf(referencingColumns);
        this.oldValues = List.copyOf(oldValues);
        this.newValues = List.copyOf(newValues);
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

    public List<Object> getOldValues() {
        return oldValues;
    }

    public List<Object> getNewValues() {
        return newValues;
    }
}
