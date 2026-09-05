package com.yekdb.database;

import com.yekdb.trigger.TriggerCatalog;
import com.yekdb.procedure.ProcedureCatalog;
import com.yekdb.view.ViewCatalog;

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
    private final ViewCatalog viewCatalog;
    private final TriggerCatalog triggerCatalog;
    private final ProcedureCatalog procedureCatalog;

    public Database(
            String name,
            Path databasePath,
            DatabaseMetadata metadata
    ) {
        this(
                name,
                databasePath,
                metadata,
                new ViewCatalog(),
                new TriggerCatalog(),
                new ProcedureCatalog()
        );
    }

    public Database(
            String name,
            Path databasePath,
            DatabaseMetadata metadata,
            ViewCatalog viewCatalog
    ) {
        this(
                name,
                databasePath,
                metadata,
                viewCatalog,
                new TriggerCatalog(),
                new ProcedureCatalog()
        );
    }

    public Database(
            String name,
            Path databasePath,
            DatabaseMetadata metadata,
            ViewCatalog viewCatalog,
            TriggerCatalog triggerCatalog
    ) {
        this(
                name,
                databasePath,
                metadata,
                viewCatalog,
                triggerCatalog,
                new ProcedureCatalog()
        );
    }

    public Database(
            String name,
            Path databasePath,
            DatabaseMetadata metadata,
            ViewCatalog viewCatalog,
            TriggerCatalog triggerCatalog,
            ProcedureCatalog procedureCatalog
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

        this.viewCatalog = Objects.requireNonNull(
                viewCatalog,
                "View catalog cannot be null."
        );

        this.triggerCatalog = Objects.requireNonNull(
                triggerCatalog,
                "Trigger catalog cannot be null."
        );

        this.procedureCatalog = Objects.requireNonNull(
                procedureCatalog,
                "Procedure catalog cannot be null."
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

    public ViewCatalog getViewCatalog() {
        return viewCatalog;
    }

    public TriggerCatalog getTriggerCatalog() {
        return triggerCatalog;
    }

    public ProcedureCatalog getProcedureCatalog() {
        return procedureCatalog;
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
                ", viewCatalog=" + viewCatalog +
                ", triggerCatalog=" + triggerCatalog +
                ", procedureCatalog=" + procedureCatalog +
                '}';
    }
}
