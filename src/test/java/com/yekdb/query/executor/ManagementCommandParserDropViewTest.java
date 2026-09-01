package com.yekdb.query.executor;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.DropViewCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagementCommandParserDropViewTest {

    private final ManagementCommandParser parser =
            new ManagementCommandParser();

    @Test
    void shouldParseDropViewCommand() {
        Command command =
                parser.parse(
                        "DROP VIEW adult_users;"
                );

        DropViewCommand dropViewCommand =
                assertInstanceOf(
                        DropViewCommand.class,
                        command
                );

        assertEquals(
                "adult_users",
                dropViewCommand.getViewName()
        );
    }

    @Test
    void shouldRejectDropViewWithoutName() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "DROP VIEW"
                )
        );
    }
}
