package com.yekdb.procedure;

import java.util.Locale;

/**
 * Stored procedure ve ilgili database object adlarını doğrular.
 */
final class ProcedureNameValidator {

    private static final String OBJECT_NAME_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    private ProcedureNameValidator() {
    }

    static String validate(String procedureName) {
        return validateObjectName(
                procedureName,
                "Procedure name"
        );
    }

    static String validateObjectName(
            String objectName,
            String fieldName
    ) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be null or blank."
            );
        }

        String normalizedName =
                objectName.trim();

        if (!normalizedName.matches(OBJECT_NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName.toLowerCase(Locale.ROOT)
                            + ": " + normalizedName
            );
        }

        return normalizedName.toLowerCase(Locale.ROOT);
    }
}
