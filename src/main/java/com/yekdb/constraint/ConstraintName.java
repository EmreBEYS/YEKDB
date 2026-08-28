package com.yekdb.constraint;

import java.util.Locale;

/**
 * SQL constraint adlarının doğrulanması ve normalize edilmesi için yardımcı sınıf.
 *
 * Sprint 00-26 Phase 6.
 */
final class ConstraintName {

    private static final String IDENTIFIER_PATTERN =
            "[A-Za-z_][A-Za-z0-9_]*";

    private ConstraintName() {
        // Utility class.
    }

    static String normalizeNullable(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Constraint name cannot be blank"
            );
        }

        if (!normalized.matches(IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid constraint name: " + normalized
            );
        }

        return normalized.toLowerCase(Locale.ROOT);
    }
}
