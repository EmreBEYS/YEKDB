package com.yekdb.trigger;

import java.util.Locale;

/**
 * Trigger ve ilişkili database object adlarını doğrular.
 */
final class TriggerNameValidator {

    private static final String OBJECT_NAME_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    private TriggerNameValidator() {
    }

    static String validate(String triggerName) {
        return validateObjectName(
                triggerName,
                "Trigger name"
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
