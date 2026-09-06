package com.yekdb.concurrency;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Concurrency kilitlerinin case-insensitive ve kararlı kaynak kimligi.
 */
public record LockResource(
        String databaseIdentity,
        LockResourceType resourceType,
        String resourceName
) {

    public LockResource {

        databaseIdentity = normalize(
                databaseIdentity,
                "DatabaseIdentity"
        );

        resourceType = Objects.requireNonNull(
                resourceType,
                "ResourceType cannot be null."
        );

        resourceName = normalize(
                resourceName,
                "ResourceName"
        );
    }

    public static LockResource table(
            Path databasePath,
            String tableName
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        return new LockResource(
                databasePath.toAbsolutePath()
                        .normalize()
                        .toString(),
                LockResourceType.TABLE,
                tableName
        );
    }

    private static String normalize(
            String value,
            String fieldName
    ) {

        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return value.trim()
                .toLowerCase(Locale.ROOT);
    }
}
