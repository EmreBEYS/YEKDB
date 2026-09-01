package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseMetadata;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.trigger.TriggerDefinition;
import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerMetadata;
import com.yekdb.trigger.TriggerTiming;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriggerExecutionSupportTest {

    @Test
    void shouldResolveNewReferencesOutsideStringLiterals() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        database.getTriggerCatalog()
                .registerTrigger(
                        new TriggerDefinition(
                                "users_after_insert",
                                "users",
                                TriggerTiming.AFTER,
                                TriggerEvent.INSERT,
                                "INSERT INTO audit_log (id, message) "
                                        + "VALUES (NEW.id, 'NEW.name literal')"
                        ),
                        new TriggerMetadata(
                                "users_after_insert",
                                "users"
                        )
                );

        Table users =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        )
                );

        List<String> executedSql =
                new ArrayList<>();

        new TriggerExecutionSupport()
                .executeInsertTriggers(
                        database,
                        users,
                        new InsertCommand(
                                "users",
                                List.of("id", "name"),
                                List.of(1, "Emre")
                        ),
                        TriggerTiming.AFTER,
                        sql -> {
                            executedSql.add(sql);
                            return ExecuteResult.success("ok");
                        }
                );

        assertEquals(1, executedSql.size());
        assertEquals(
                "INSERT INTO audit_log (id, message) "
                        + "VALUES (1, 'NEW.name literal')",
                executedSql.get(0)
        );
    }

    @Test
    void shouldEscapeStringNewValues() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        database.getTriggerCatalog()
                .registerTrigger(
                        new TriggerDefinition(
                                "users_after_insert",
                                "users",
                                TriggerTiming.AFTER,
                                TriggerEvent.INSERT,
                                "INSERT INTO audit_log (message) VALUES (NEW.name)"
                        ),
                        new TriggerMetadata(
                                "users_after_insert",
                                "users"
                        )
                );

        Table users =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        )
                );

        List<String> executedSql =
                new ArrayList<>();

        new TriggerExecutionSupport()
                .executeInsertTriggers(
                        database,
                        users,
                        new InsertCommand(
                                "users",
                                List.of("id", "name"),
                                List.of(1, "Emre'nin")
                        ),
                        TriggerTiming.AFTER,
                        sql -> {
                            executedSql.add(sql);
                            return ExecuteResult.success("ok");
                        }
                );

        assertEquals(
                "INSERT INTO audit_log (message) VALUES ('Emre''nin')",
                executedSql.get(0)
        );
    }

    @Test
    void shouldRejectRecursiveTriggerExecution() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        database.getTriggerCatalog()
                .registerTrigger(
                        new TriggerDefinition(
                                "users_after_insert",
                                "users",
                                TriggerTiming.AFTER,
                                TriggerEvent.INSERT,
                                "INSERT INTO users (id, name) VALUES (2, NEW.name)"
                        ),
                        new TriggerMetadata(
                                "users_after_insert",
                                "users"
                        )
                );

        Table users =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        )
                );

        TriggerExecutionSupport triggerExecutionSupport =
                new TriggerExecutionSupport();

        InsertCommand insertCommand =
                new InsertCommand(
                        "users",
                        List.of("id", "name"),
                        List.of(1, "Emre")
                );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> triggerExecutionSupport
                                .executeInsertTriggers(
                                        database,
                                        users,
                                        insertCommand,
                                        TriggerTiming.AFTER,
                                        sql -> {
                                            triggerExecutionSupport
                                                    .executeInsertTriggers(
                                                            database,
                                                            users,
                                                            insertCommand,
                                                            TriggerTiming.AFTER,
                                                            ignored -> ExecuteResult
                                                                    .success("ok")
                                                    );

                                            return ExecuteResult
                                                    .success("ok");
                                        }
                                )
                );

        assertTrue(
                containsMessage(
                        exception,
                        "Recursive trigger execution detected"
                )
        );
    }

    private boolean containsMessage(
            Throwable throwable,
            String expectedMessage
    ) {
        Throwable current =
                throwable;

        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage()
                    .contains(expectedMessage)) {
                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }
}
