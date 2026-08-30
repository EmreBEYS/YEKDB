package com.yekdb.storage.table;

/**
 * Central runtime value validator for table columns.
 *
 * Sprint 00-28 Phase 15 hardens INSERT/UPDATE validation by keeping
 * type, precision/scale, length and structured/temporal checks in a
 * single place. This prevents the two mutation paths from drifting.
 */
public final class ColumnValueValidator {

    private ColumnValueValidator() {
        // Utility class.
    }

    /**
     * Validates a runtime value against a column definition.
     *
     * NULL is accepted here. NOT NULL semantics remain the responsibility
     * of the constraint validation layer.
     *
     * @param column target column
     * @param value runtime value
     * @throws IllegalArgumentException when the value is incompatible
     */
    public static void validate(Column column, Object value) {
        if (column == null) {
            throw new IllegalArgumentException("Column cannot be null.");
        }

        if (value == null) {
            return;
        }

        DataType dataType = column.getDataType();

        boolean validType = switch (dataType) {
            case INT -> value instanceof Integer;
            case LONG -> value instanceof Long;
            case DOUBLE -> value instanceof Double;
            case NUMERIC -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case STRING, CHAR, VARCHAR, TEXT,
                 UUID, DATE, TIME, TIMESTAMP, INTERVAL,
                 ARRAY, JSON, HSTORE, UDT -> value instanceof String;
        };

        if (!validType) {
            throw new IllegalArgumentException(
                    "Invalid value type for column '" +
                            column.getName() +
                            "'. Expected: " +
                            dataType +
                            ", actual: " +
                            value.getClass().getSimpleName()
            );
        }

        if (value instanceof Number numberValue
                && !column.acceptsNumericValue(numberValue)) {
            throw new IllegalArgumentException(
                    "Numeric value exceeds precision/scale for column '" +
                            column.getName() +
                            "'. Declared type: " +
                            column.getTypeDeclaration() +
                            ", value: " +
                            value +
                            "."
            );
        }

        if (value instanceof String stringValue
                && !column.acceptsTypedStringValue(stringValue)) {
            throw new IllegalArgumentException(
                    "Invalid " +
                            column.getDataType() +
                            " value for column '" +
                            column.getName() +
                            "': " +
                            stringValue +
                            "."
            );
        }

        if (value instanceof String stringValue
                && !column.acceptsStringLength(stringValue)) {
            throw new IllegalArgumentException(
                    "Value too long for column '" +
                            column.getName() +
                            "'. Maximum length: " +
                            column.getLength() +
                            ", actual length: " +
                            stringValue.length() +
                            "."
            );
        }
    }
}
