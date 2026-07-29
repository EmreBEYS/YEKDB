package com.yekdb.database;

import com.yekdb.database.exception.DatabaseOperationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sprint 00-06 Database Management demo uygulaması.
 */
public final class DatabaseManagerDemo {

    private static final String DEMO_DATABASE_NAME =
            "YekdbDemo";

    private DatabaseManagerDemo() {
    }

    public static void main(String[] args) {
        Path dataDirectory =
                Path.of("data", "demo");

        DatabaseManager manager =
                new DatabaseManager(dataDirectory);

        printTitle();

        try {
            cleanPreviousDemo(manager);

            createDatabase(manager);
            listDatabases(manager);
            useDatabase(manager);
            showMetadata(manager);
            dropDatabase(manager);

            System.out.println();
            System.out.println(
                    "Sprint 00-06 demo completed successfully."
            );

        } catch (RuntimeException exception) {
            System.err.println();
            System.err.println(
                    "Demo failed: " + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private static void createDatabase(
            DatabaseManager manager
    ) {
        System.out.println();
        System.out.println("1. CREATE DATABASE");

        Database database =
                manager.createDatabase(DEMO_DATABASE_NAME);

        System.out.println(
                "Database created : " + database.getName()
        );

        System.out.println(
                "Database path    : "
                        + database.getDatabasePath()
                        .toAbsolutePath()
        );

        Path metadataFile = database
                .getDatabasePath()
                .resolve("database.meta");

        System.out.println(
                "Metadata exists  : "
                        + Files.isRegularFile(metadataFile)
        );
    }

    private static void listDatabases(
            DatabaseManager manager
    ) {
        System.out.println();
        System.out.println("2. LIST DATABASES");

        List<String> databases =
                manager.listDatabases();

        if (databases.isEmpty()) {
            System.out.println("No databases found.");
            return;
        }

        databases.forEach(databaseName ->
                System.out.println("- " + databaseName)
        );
    }

    private static void useDatabase(
            DatabaseManager manager
    ) {
        System.out.println();
        System.out.println("3. USE DATABASE");

        Database selectedDatabase =
                manager.useDatabase(DEMO_DATABASE_NAME);

        System.out.println(
                "Selected database: "
                        + selectedDatabase.getName()
        );

        System.out.println(
                "Current database : "
                        + manager.getCurrentDatabase().getName()
        );
    }

    private static void showMetadata(
            DatabaseManager manager
    ) {
        System.out.println();
        System.out.println("4. DATABASE METADATA");

        Database currentDatabase =
                manager.getCurrentDatabase();

        if (currentDatabase == null) {
            throw new DatabaseOperationException(
                    "No active database selected."
            );
        }

        DatabaseMetadata metadata =
                currentDatabase.getMetadata();

        System.out.println(
                "Name          : "
                        + metadata.getDatabaseName()
        );

        System.out.println(
                "Version       : "
                        + metadata.getVersion()
        );

        System.out.println(
                "Created       : "
                        + metadata.getCreatedAt()
        );

        System.out.println(
                "Last modified : "
                        + metadata.getLastModifiedAt()
        );

        System.out.println(
                "Encoding      : "
                        + metadata.getEncoding()
        );

        System.out.println(
                "Page size     : "
                        + metadata.getPageSize()
                        + " bytes"
        );
    }

    private static void dropDatabase(
            DatabaseManager manager
    ) {
        System.out.println();
        System.out.println("5. DROP DATABASE");

        manager.dropDatabase(DEMO_DATABASE_NAME);

        System.out.println(
                "Database exists  : "
                        + manager.exists(DEMO_DATABASE_NAME)
        );

        System.out.println(
                "Current database : "
                        + manager.getCurrentDatabase()
        );
    }

    private static void cleanPreviousDemo(
            DatabaseManager manager
    ) {
        if (manager.exists(DEMO_DATABASE_NAME)) {
            manager.dropDatabase(DEMO_DATABASE_NAME);
        }
    }

    private static void printTitle() {
        System.out.println("================================");
        System.out.println(" YEKDB DATABASE MANAGER DEMO");
        System.out.println(" Sprint 00-06");
        System.out.println("================================");
    }
}