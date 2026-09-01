package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseMetadata;
import com.yekdb.query.command.CreateViewCommand;
import com.yekdb.view.exception.DuplicateViewException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CreateViewExecutorTest {

    @Test
    void shouldRegisterViewInDatabaseCatalog() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        CreateViewExecutor executor =
                new CreateViewExecutor();

        ExecuteResult result =
                executor.execute(
                        database,
                        new CreateViewCommand(
                                "adult_users",
                                "SELECT id, name FROM users WHERE age >= 18"
                        )
                );

        assertTrue(result.isSuccess());
        assertTrue(
                database.getViewCatalog()
                        .containsView("adult_users")
        );
        assertEquals(
                "SELECT id, name FROM users WHERE age >= 18",
                database.getViewCatalog()
                        .getView("adult_users")
                        .getSourceSelect()
        );
    }

    @Test
    void shouldRejectDuplicateView() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        CreateViewExecutor executor =
                new CreateViewExecutor();

        CreateViewCommand command =
                new CreateViewCommand(
                        "adult_users",
                        "SELECT * FROM users"
                );

        executor.execute(
                database,
                command
        );

        assertThrows(
                DuplicateViewException.class,
                () -> executor.execute(
                        database,
                        command
                )
        );
    }
}
