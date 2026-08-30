package com.yekdb.storage.type;

import com.yekdb.storage.table.ColumnTypeDefinition;
import com.yekdb.storage.table.DataType;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Defines a named user-defined type for the YEKDB V1 UDT foundation.
 *
 * V1 models UDTs as named aliases/domains backed by an existing concrete
 * YEKDB type. CREATE TYPE / ALTER TYPE SQL DDL is intentionally deferred;
 * this class and UserDefinedTypeRegistry provide the durable core contract.
 */
public final class UserDefinedTypeDefinition {

    private static final Pattern NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final String name;
    private final ColumnTypeDefinition baseType;

    public UserDefinedTypeDefinition(
            String name,
            ColumnTypeDefinition baseType
    ) {
        this.name = normalizeAndValidateName(name);
        this.baseType = Objects.requireNonNull(baseType, "baseType");

        if (baseType.dataType() == DataType.UDT) {
            throw new IllegalArgumentException(
                    "A UDT cannot directly use another UDT as its V1 base type."
            );
        }
        if (baseType.dataType() == DataType.ARRAY) {
            throw new IllegalArgumentException(
                    "ARRAY-backed UDTs are not supported in V1."
            );
        }
    }

    public String name() {
        return name;
    }

    public ColumnTypeDefinition baseType() {
        return baseType;
    }

    public String declaration() {
        return "UDT(" + name + ")";
    }

    static String normalizeAndValidateName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UDT name cannot be null or blank.");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid UDT name: " + value);
        }
        return normalized;
    }
}
