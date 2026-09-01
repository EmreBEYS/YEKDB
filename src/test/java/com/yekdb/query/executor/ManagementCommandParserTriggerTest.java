package com.yekdb.query.executor;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateTriggerCommand;
import com.yekdb.query.command.DropTriggerCommand;
import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerTiming;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagementCommandParserTriggerTest {

    private final ManagementCommandParser parser =
            new ManagementCommandParser();

    @Test
    void shouldParseCreateTriggerCommand() {
        CreateTriggerCommand command =
                parseCreateTrigger(
                        """
                        CREATE TRIGGER users_insert_log
                        AFTER INSERT ON users
                        BEGIN
                            INSERT INTO audit_log VALUES (NEW.id)
                        END;
                        """
                );

        assertEquals("users_insert_log", command.getTriggerName());
        assertEquals("users", command.getTableName());
        assertEquals(TriggerTiming.AFTER, command.getTiming());
        assertEquals(TriggerEvent.INSERT, command.getEvent());
        assertEquals(
                "INSERT INTO audit_log VALUES (NEW.id)",
                command.getBody()
        );
    }

    @Test
    void shouldParseCreateTriggerCaseInsensitive() {
        CreateTriggerCommand command =
                parseCreateTrigger(
                        """
                        create trigger users_before_update
                        before update on users
                        begin
                            set NEW.updated_at = NOW()
                        end
                        """
                );

        assertEquals("users_before_update", command.getTriggerName());
        assertEquals("users", command.getTableName());
        assertEquals(TriggerTiming.BEFORE, command.getTiming());
        assertEquals(TriggerEvent.UPDATE, command.getEvent());
        assertEquals(
                "set NEW.updated_at = NOW()",
                command.getBody()
        );
    }

    @Test
    void shouldParseAllTimingAndEventCombinations() {
        for (TriggerTiming timing : TriggerTiming.values()) {
            for (TriggerEvent event : TriggerEvent.values()) {
                CreateTriggerCommand command =
                        parseCreateTrigger(
                                "CREATE TRIGGER audit_" +
                                        timing.name().toLowerCase() +
                                        "_" +
                                        event.name().toLowerCase() +
                                        " " +
                                        timing.name() +
                                        " " +
                                        event.name() +
                                        " ON users BEGIN body END;"
                        );

                assertEquals(timing, command.getTiming());
                assertEquals(event, command.getEvent());
                assertEquals("users", command.getTableName());
                assertEquals("body", command.getBody());
            }
        }
    }

    @Test
    void shouldKeepBodyTextBetweenBeginAndEnd() {
        CreateTriggerCommand command =
                parseCreateTrigger(
                        """
                        CREATE TRIGGER users_after_delete
                        AFTER DELETE ON users
                        BEGIN
                            INSERT INTO audit_log VALUES ('deleted END marker', OLD.id);
                            INSERT INTO audit_log VALUES ('done', OLD.id)
                        END;
                        """
                );

        assertTrue(
                command.getBody()
                        .contains("'deleted END marker'")
        );
        assertTrue(
                command.getBody()
                        .contains("INSERT INTO audit_log")
        );
    }

    @Test
    void shouldParseDropTriggerCommand() {
        Command command =
                parser.parse(
                        "DROP TRIGGER users_insert_log;"
                );

        DropTriggerCommand dropTriggerCommand =
                assertInstanceOf(
                        DropTriggerCommand.class,
                        command
                );

        assertEquals(
                "users_insert_log",
                dropTriggerCommand.getTriggerName()
        );
    }

    @Test
    void shouldRejectCreateTriggerWithoutBegin() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TRIGGER t AFTER INSERT ON users body END"
                )
        );
    }

    @Test
    void shouldRejectCreateTriggerWithoutEnd() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TRIGGER t AFTER INSERT ON users BEGIN body"
                )
        );
    }

    @Test
    void shouldRejectCreateTriggerWithoutOn() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TRIGGER t AFTER INSERT users BEGIN body END"
                )
        );
    }

    @Test
    void shouldRejectInvalidTriggerTiming() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TRIGGER t DURING INSERT ON users BEGIN body END"
                )
        );
    }

    @Test
    void shouldRejectInvalidTriggerEvent() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TRIGGER t AFTER UPSERT ON users BEGIN body END"
                )
        );
    }

    @Test
    void shouldRejectEmptyTriggerBody() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TRIGGER t AFTER INSERT ON users BEGIN END"
                )
        );
    }

    @Test
    void shouldRejectDropTriggerWithoutName() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "DROP TRIGGER"
                )
        );
    }

    private CreateTriggerCommand parseCreateTrigger(String sql) {
        Command command =
                parser.parse(sql);

        return assertInstanceOf(
                CreateTriggerCommand.class,
                command
        );
    }
}
