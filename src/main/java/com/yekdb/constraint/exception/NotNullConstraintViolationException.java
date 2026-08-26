package com.yekdb.constraint.exception;

public final class NotNullConstraintViolationException
        extends ConstraintViolationException {

    private final String columnName;

    public NotNullConstraintViolationException(
            String columnName
    ) {
        super(
                "NOT NULL constraint violated for column: "
                        + columnName
        );

        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }
}