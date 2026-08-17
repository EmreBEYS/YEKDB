package com.yekdb.database;

import java.nio.file.Path;
import java.util.Objects;

/**
 * YEKDB içinde açılmış bir veritabanını temsil eder.
 *
 * <p>Database nesnesi, seçilen veritabanına ait çalışma zamanı
 * bilgilerini ve metadata bilgisini içerir.</p>
 */
public class Database {

    private final String name;
    private final Path databasePath;
    private final DatabaseMetadata metadata;

    public Database(
            String name,
            Path databasePath,
            DatabaseMetadata metadata
    ) {
        this.name = DatabaseNameValidator.validate(name);

        this.databasePath = Objects.requireNonNull(
                databasePath,
                "Database path cannot be null."
        ).normalize();

        this.metadata = Objects.requireNonNull(
                metadata,
                "Database metadata cannot be null."
        );

        validateMetadataConsistency();
    }

    public String getName() {
        return name;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public DatabaseMetadata getMetadata() {
        return metadata;
    }

    /**
     * Database nesnesinin adı ile metadata içerisinde saklanan
     * veritabanı adının aynı olduğunu doğrular.
     */
    private void validateMetadataConsistency() {

        if (!name.equals(metadata.getDatabaseName())) {
            throw new IllegalArgumentException(
                    "Database name does not match metadata database name. "
                            + "Database: " + name
                            + ", Metadata: "
                            + metadata.getDatabaseName()
            );
        }
    }

    @Override
    public String toString() {
        return "Database{" +
                "name='" + name + '\'' +
                ", databasePath=" + databasePath +
                ", metadata=" + metadata +
                '}';
    }
}