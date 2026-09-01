package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.query.command.CreateViewCommand;
import com.yekdb.view.ViewDefinition;
import com.yekdb.view.ViewMetadata;

import java.util.Objects;

/**
 * CREATE VIEW komutunu view catalog kaydına dönüştürür.
 */
final class CreateViewExecutor {

    ExecuteResult execute(
            Database database,
            CreateViewCommand command
    ) {
        Objects.requireNonNull(
                database,
                "Database cannot be null."
        );

        Objects.requireNonNull(
                command,
                "Create view command cannot be null."
        );

        ViewDefinition definition =
                new ViewDefinition(
                        command.getViewName(),
                        command.getSourceSelect()
                );

        ViewMetadata metadata =
                new ViewMetadata(
                        definition.getViewName()
                );

        database.getViewCatalog()
                .registerView(
                        definition,
                        metadata
                );

        return ExecuteResult.success(
                "View created successfully: "
                        + definition.getViewName()
        );
    }
}
