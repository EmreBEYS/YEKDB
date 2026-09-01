package com.yekdb.query.executor;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateViewCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagementCommandParserCreateViewTest {

    private final ManagementCommandParser parser =
            new ManagementCommandParser();

    @Test
    void shouldParseCreateViewCommand() {
        CreateViewCommand command =
                parseCreateView(
                        "CREATE VIEW adult_users AS " +
                                "SELECT id, name, age FROM users WHERE age >= 18"
                );

        assertEquals("adult_users", command.getViewName());
        assertEquals(
                "SELECT id, name, age FROM users WHERE age >= 18",
                command.getSourceSelect()
        );
    }

    @Test
    void shouldParseCreateViewCommandCaseInsensitive() {
        CreateViewCommand command =
                parseCreateView(
                        "create view Adult_Users as " +
                                "select id from users"
                );

        assertEquals("Adult_Users", command.getViewName());
        assertEquals(
                "select id from users",
                command.getSourceSelect()
        );
    }

    @Test
    void shouldAllowSelectAliasesAndClausesInViewSource() {
        CreateViewCommand command =
                parseCreateView(
                        "CREATE VIEW department_totals AS " +
                                "SELECT department AS dept, COUNT(*) employee_count " +
                                "FROM employees GROUP BY department " +
                                "HAVING employee_count > 1 ORDER BY department"
                );

        assertEquals("department_totals", command.getViewName());
        assertTrue(
                command.getSourceSelect()
                        .contains("GROUP BY department")
        );
    }

    @Test
    void shouldStripSingleTrailingSemicolon() {
        CreateViewCommand command =
                parseCreateView(
                        "CREATE VIEW adults AS SELECT * FROM users;"
                );

        assertEquals(
                "SELECT * FROM users",
                command.getSourceSelect()
        );
    }

    @Test
    void shouldRejectCreateViewWithoutViewName() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE VIEW"
                )
        );
    }

    @Test
    void shouldRejectCreateViewWithoutAsKeyword() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE VIEW adults SELECT * FROM users"
                )
        );
    }

    @Test
    void shouldRejectCreateViewWithoutSourceSelect() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE VIEW adults AS"
                )
        );
    }

    @Test
    void shouldRejectCreateViewWithNonSelectSource() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE VIEW adults AS INSERT INTO users (id) VALUES (1)"
                )
        );
    }

    @Test
    void shouldRejectCreateViewWithInvalidSelectSource() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE VIEW adults AS SELECT FROM users"
                )
        );
    }

    private CreateViewCommand parseCreateView(String sql) {
        Command command =
                parser.parse(sql);

        return assertInstanceOf(
                CreateViewCommand.class,
                command
        );
    }
}
