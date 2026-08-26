package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateTableCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagementCommandParserConstraintTest {

    private final ManagementCommandParser parser =
            new ManagementCommandParser();

    @Test
    void shouldParseInlinePrimaryKey() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE users (id INT PRIMARY KEY, name STRING)"
        );

        assertEquals(2, command.getColumnCount());
        assertEquals(1, command.getConstraintCount());
        assertConstraint(
                command,
                ConstraintType.PRIMARY_KEY,
                List.of("id")
        );
    }

    @Test
    void shouldParseInlineUnique() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE users (id INT, username STRING UNIQUE)"
        );

        assertConstraint(
                command,
                ConstraintType.UNIQUE,
                List.of("username")
        );
    }

    @Test
    void shouldParseInlineNotNull() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE users (id INT, email STRING NOT NULL)"
        );

        assertConstraint(
                command,
                ConstraintType.NOT_NULL,
                List.of("email")
        );
    }

    @Test
    void shouldParseMultipleInlineConstraints() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE users (" +
                        "id INT PRIMARY KEY, " +
                        "username STRING UNIQUE, " +
                        "email STRING NOT NULL)"
        );

        assertEquals(3, command.getColumnCount());
        assertEquals(3, command.getConstraintCount());
        assertConstraint(command, ConstraintType.PRIMARY_KEY, List.of("id"));
        assertConstraint(command, ConstraintType.UNIQUE, List.of("username"));
        assertConstraint(command, ConstraintType.NOT_NULL, List.of("email"));
    }

    @Test
    void shouldParseCompositePrimaryKey() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE enrollments (" +
                        "student_id INT, " +
                        "course_id INT, " +
                        "grade INT, " +
                        "PRIMARY KEY (student_id, course_id))"
        );

        assertConstraint(
                command,
                ConstraintType.PRIMARY_KEY,
                List.of("student_id", "course_id")
        );
    }

    @Test
    void shouldParseCompositeUnique() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE persons (" +
                        "id INT, " +
                        "first_name STRING, " +
                        "last_name STRING, " +
                        "UNIQUE (first_name, last_name))"
        );

        assertConstraint(
                command,
                ConstraintType.UNIQUE,
                List.of("first_name", "last_name")
        );
    }

    @Test
    void shouldParseMixedInlineAndTableConstraints() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE memberships (" +
                        "user_id INT NOT NULL, " +
                        "group_id INT NOT NULL, " +
                        "nickname STRING UNIQUE, " +
                        "PRIMARY KEY (user_id, group_id))"
        );

        assertEquals(4, command.getConstraintCount());
        assertConstraint(command, ConstraintType.NOT_NULL, List.of("user_id"));
        assertConstraint(command, ConstraintType.NOT_NULL, List.of("group_id"));
        assertConstraint(command, ConstraintType.UNIQUE, List.of("nickname"));
        assertConstraint(
                command,
                ConstraintType.PRIMARY_KEY,
                List.of("user_id", "group_id")
        );
    }

    @Test
    void shouldRemainBackwardCompatibleWithoutConstraints() {
        CreateTableCommand command = parseCreateTable(
                "CREATE TABLE users (id INT, name STRING)"
        );

        assertEquals(2, command.getColumnCount());
        assertTrue(command.getConstraints().isEmpty());
    }

    private CreateTableCommand parseCreateTable(String sql) {
        Command command = parser.parse(sql);
        return assertInstanceOf(CreateTableCommand.class, command);
    }

    private void assertConstraint(
            CreateTableCommand command,
            ConstraintType type,
            List<String> columns
    ) {
        Constraint found = command.getConstraints()
                .stream()
                .filter(value -> value.type() == type)
                .filter(value -> value.columns().equals(columns))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Constraint not found. type=" + type
                                + ", columns=" + columns
                                + ", actual=" + command.getConstraints()
                ));

        assertEquals(type, found.type());
        assertEquals(columns, found.columns());
    }
}
