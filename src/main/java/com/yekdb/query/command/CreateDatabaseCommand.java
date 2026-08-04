package com.yekdb.query.command;

import java.util.Objects;

/**
 * CREATE DATABASE SQL komutunu temsil eder.
 */
public final class CreateDatabaseCommand implements Command {

    private final String databaseName;

    public CreateDatabaseCommand(String databaseName) {
        this.databaseName = Objects.requireNonNull(
                databaseName,
                "Database name cannot be null."
        ).trim();

        if (this.databaseName.isBlank()) {
            throw new IllegalArgumentException(
                    "Database name cannot be blank."
            );
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String toString() {
        return "CreateDatabaseCommand{" +
                "databaseName='" + databaseName + '\'' +
                '}';
    }
}