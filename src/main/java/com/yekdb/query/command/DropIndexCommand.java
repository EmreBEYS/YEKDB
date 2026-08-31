package com.yekdb.query.command;

import java.util.Objects;

/**
 * DROP INDEX SQL komutunu temsil eder.
 */
public final class DropIndexCommand
        implements Command {

    private final String indexName;

    public DropIndexCommand(
            String indexName
    ) {

        this.indexName =
                Objects.requireNonNull(
                        indexName,
                        "Index name cannot be null."
                ).trim();

        if (this.indexName.isBlank()) {

            throw new IllegalArgumentException(
                    "Index name cannot be blank."
            );
        }
    }

    public String getIndexName() {
        return indexName;
    }
}