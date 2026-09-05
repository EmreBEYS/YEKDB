package com.yekdb.query.executor;

import com.yekdb.query.command.CallProcedureCommand;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateProcedureCommand;
import com.yekdb.query.command.DropProcedureCommand;
import com.yekdb.query.command.ShowProcedureCommand;
import com.yekdb.query.command.ShowProceduresCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagementCommandParserProcedureTest {

    private final ManagementCommandParser parser =
            new ManagementCommandParser();

    @Test
    void shouldParseCreateProcedureCommand() {
        CreateProcedureCommand command =
                parseCreateProcedure(
                        """
                        CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                        BEGIN
                            INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                        END;
                        """
                );

        assertEquals("create_user", command.getProcedureName());
        assertEquals(2, command.getParameters().size());
        assertEquals("user_id", command.getParameters().get(0).getName());
        assertEquals(
                "INT",
                command.getParameters()
                        .get(0)
                        .getTypeDefinition()
                        .declaration()
        );
        assertEquals(
                "INSERT INTO users (id, name) VALUES (:user_id, :user_name)",
                command.getBody()
        );
    }

    @Test
    void shouldParseCreateProcedureWithoutParameters() {
        CreateProcedureCommand command =
                parseCreateProcedure(
                        """
                        create procedure seed_users()
                        begin
                            INSERT INTO users (id, name) VALUES (1, 'Ada')
                        end
                        """
                );

        assertEquals("seed_users", command.getProcedureName());
        assertTrue(command.getParameters().isEmpty());
    }

    @Test
    void shouldParseCreateOrReplaceProcedureCommand() {
        CreateProcedureCommand command =
                parseCreateProcedure(
                        """
                        CREATE OR REPLACE PROCEDURE create_user(user_id INT)
                        BEGIN
                            SELECT * FROM users
                        END;
                        """
                );

        assertEquals("create_user", command.getProcedureName());
        assertEquals(1, command.getParameters().size());
        assertTrue(command.isReplaceExisting());
    }

    @Test
    void shouldParseCreateProcedureIfNotExistsCommand() {
        CreateProcedureCommand command =
                parseCreateProcedure(
                        """
                        CREATE PROCEDURE IF NOT EXISTS create_user(user_id INT)
                        BEGIN
                            SELECT * FROM users
                        END;
                        """
                );

        assertEquals("create_user", command.getProcedureName());
        assertEquals(1, command.getParameters().size());
        assertFalse(command.isReplaceExisting());
        assertTrue(command.isIfNotExists());
    }

    @Test
    void shouldKeepEndTextInsideQuotedProcedureBody() {
        CreateProcedureCommand command =
                parseCreateProcedure(
                        """
                        CREATE PROCEDURE write_marker()
                        BEGIN
                            INSERT INTO logs (message) VALUES ('END is text');
                            SELECT * FROM logs
                        END;
                        """
                );

        assertTrue(
                command.getBody()
                        .contains("'END is text'")
        );
        assertTrue(
                command.getBody()
                        .contains("SELECT * FROM logs")
        );
    }

    @Test
    void shouldParseCallProcedureCommand() {
        Command command =
                parser.parse(
                        "CALL create_user(1, 'Ada Lovelace', true, NULL);"
                );

        CallProcedureCommand call =
                assertInstanceOf(
                        CallProcedureCommand.class,
                        command
                );

        assertEquals("create_user", call.getProcedureName());
        assertEquals(4, call.getArguments().size());
        assertEquals(1, call.getArguments().get(0));
        assertEquals("Ada Lovelace", call.getArguments().get(1));
        assertEquals(true, call.getArguments().get(2));
        assertNull(call.getArguments().get(3));
    }

    @Test
    void shouldParseCallProcedureWithNamedArguments() {
        Command command =
                parser.parse(
                        "CALL create_user(user_name => 'Ada', user_id => 1);"
                );

        CallProcedureCommand call =
                assertInstanceOf(
                        CallProcedureCommand.class,
                        command
                );

        assertEquals("create_user", call.getProcedureName());
        assertTrue(call.hasNamedArguments());
        assertEquals(2, call.getNamedArguments().size());
        assertEquals("Ada", call.getNamedArguments().get("user_name"));
        assertEquals(1, call.getNamedArguments().get("user_id"));
    }

    @Test
    void shouldRejectMixedCallArguments() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CALL create_user(1, user_name => 'Ada');"
                )
        );
    }

    @Test
    void shouldRejectDuplicateNamedCallArguments() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CALL create_user(user_id => 1, USER_ID => 2);"
                )
        );
    }

    @Test
    void shouldParseDropProcedureCommand() {
        Command command =
                parser.parse(
                        "DROP PROCEDURE create_user;"
                );

        DropProcedureCommand drop =
                assertInstanceOf(
                        DropProcedureCommand.class,
                        command
                );

        assertEquals("create_user", drop.getProcedureName());
        assertFalse(drop.isIfExists());
    }

    @Test
    void shouldParseDropProcedureIfExistsCommand() {
        Command command =
                parser.parse(
                        "DROP PROCEDURE IF EXISTS create_user;"
                );

        DropProcedureCommand drop =
                assertInstanceOf(
                        DropProcedureCommand.class,
                        command
                );

        assertEquals("create_user", drop.getProcedureName());
        assertTrue(drop.isIfExists());
    }

    @Test
    void shouldParseShowProceduresCommand() {
        assertInstanceOf(
                ShowProceduresCommand.class,
                parser.parse("SHOW PROCEDURES;")
        );
    }

    @Test
    void shouldParseShowProceduresLikeCommand() {
        Command command =
                parser.parse(
                        "SHOW PROCEDURES LIKE 'create_%';"
                );

        ShowProceduresCommand show =
                assertInstanceOf(
                        ShowProceduresCommand.class,
                        command
                );

        assertTrue(show.hasLikePattern());
        assertEquals(
                "create_%",
                show.getLikePattern()
        );
    }

    @Test
    void shouldParseShowProcedureCommand() {
        Command command =
                parser.parse(
                        "SHOW PROCEDURE create_user;"
                );

        ShowProcedureCommand show =
                assertInstanceOf(
                        ShowProcedureCommand.class,
                        command
                );

        assertEquals(
                "create_user",
                show.getProcedureName()
        );
    }

    @Test
    void shouldRejectInvalidCreateProcedureSyntax() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE PROCEDURE p BEGIN SELECT * FROM users END"
                )
        );

        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE PROCEDURE p() BEGIN END"
                )
        );
    }

    @Test
    void shouldRejectInvalidCallSyntax() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse("CALL create_user")
        );

        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse("CALL create_user(abc)")
        );
    }

    private CreateProcedureCommand parseCreateProcedure(String sql) {
        Command command =
                parser.parse(sql);

        return assertInstanceOf(
                CreateProcedureCommand.class,
                command
        );
    }
}
