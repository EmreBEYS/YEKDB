package com.yekdb.view;

import java.util.Locale;

/**
 * View adlarını doğrular ve normalize eder.
 */
final class ViewNameValidator {

    private static final String VIEW_NAME_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    private ViewNameValidator() {
    }

    static String validate(String viewName) {

        if (viewName == null || viewName.isBlank()) {
            throw new IllegalArgumentException(
                    "View name cannot be null or blank."
            );
        }

        String normalizedName = viewName.trim();

        if (!normalizedName.matches(VIEW_NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid view name: " + normalizedName
            );
        }

        return normalizedName.toLowerCase(Locale.ROOT);
    }
}
