package com.yekdb.constraint.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * INSERT/UPDATE sırasında FOREIGN KEY referansı karşılanmadığında fırlatılır.
 */
public final class ForeignKeyConstraintViolationException
        extends ConstraintViolationException {

    private final List<String> columns;
    private final String referencedTableName;
    private final List<String> referencedColumns;
    private final List<Object> values;

    public ForeignKeyConstraintViolationException(
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumns,
            List<Object> values
    ) {
        super(
                "FOREIGN KEY constraint violation. Columns "
                        + columns
                        + " with values "
                        + values
                        + " reference missing row "
                        + referencedTableName
                        + referencedColumns
        );

        this.columns = List.copyOf(columns);
        this.referencedTableName = referencedTableName;
        this.referencedColumns = List.copyOf(referencedColumns);
        this.values = Collections.unmodifiableList(
                new ArrayList<>(values)
        );
    }

    public List<String> getColumns() {
        return columns;
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public List<String> getReferencedColumns() {
        return referencedColumns;
    }

    public List<Object> getValues() {
        return values;
    }
}
