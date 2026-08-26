package com.yekdb.cli.integration;

import com.yekdb.cli.executor.SqlTerminalExecutor;
import com.yekdb.database.DatabaseManager;
import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.query.executor.QueryExecutionException;
import com.yekdb.query.executor.QueryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintSqlTerminalIntegrationTest {

    @TempDir
    Path tempDirectory;

    private SqlTerminalExecutor terminalExecutor;

    @BeforeEach
    void setUp() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager
                );

        terminalExecutor =
                new SqlTerminalExecutor(
                        queryExecutor
                );

        terminalExecutor.execute(
                "CREATE DATABASE phase7db;"
        );

        terminalExecutor.execute(
                "USE DATABASE phase7db;"
        );
    }

    @Test
    void shouldExecuteCreateTableAndValidInsertWithConstraints() {

        terminalExecutor.execute(
                "CREATE TABLE users (" +
                        "id INT PRIMARY KEY, " +
                        "username STRING UNIQUE, " +
                        "email STRING NOT NULL" +
                        ");"
        );

        ExecuteResult result =
                assertDoesNotThrow(() ->
                        terminalExecutor.execute(
                                "INSERT INTO users " +
                                        "(id, username, email) " +
                                        "VALUES (1, 'emre', 'emre@example.com');"
                        )
                );

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldSurfaceNotNullViolationThroughSqlTerminal() {

        createUsersTable();

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> terminalExecutor.execute(
                                "INSERT INTO users " +
                                        "(id, username, email) " +
                                        "VALUES (1, 'emre', NULL);"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("NOT NULL")
        );
    }

    @Test
    void shouldSurfaceUniqueViolationThroughSqlTerminal() {

        createUsersTable();

        terminalExecutor.execute(
                "INSERT INTO users " +
                        "(id, username, email) " +
                        "VALUES (1, 'emre', 'a@example.com');"
        );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> terminalExecutor.execute(
                                "INSERT INTO users " +
                                        "(id, username, email) " +
                                        "VALUES (2, 'emre', 'b@example.com');"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("UNIQUE")
        );
    }

    @Test
    void shouldSurfacePrimaryKeyViolationThroughSqlTerminal() {

        createUsersTable();

        terminalExecutor.execute(
                "INSERT INTO users " +
                        "(id, username, email) " +
                        "VALUES (1, 'emre', 'a@example.com');"
        );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> terminalExecutor.execute(
                                "INSERT INTO users " +
                                        "(id, username, email) " +
                                        "VALUES (1, 'yunus', 'b@example.com');"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("PRIMARY KEY")
        );
    }

    @Test
    void shouldSupportCompositePrimaryKeyThroughSqlTerminal() {

        terminalExecutor.execute(
                "CREATE TABLE enrollments (" +
                        "student_id INT, " +
                        "course_id INT, " +
                        "grade INT, " +
                        "PRIMARY KEY (student_id, course_id)" +
                        ");"
        );

        terminalExecutor.execute(
                "INSERT INTO enrollments " +
                        "(student_id, course_id, grade) " +
                        "VALUES (1, 100, 85);"
        );

        assertDoesNotThrow(() ->
                terminalExecutor.execute(
                        "INSERT INTO enrollments " +
                                "(student_id, course_id, grade) " +
                                "VALUES (1, 101, 90);"
                )
        );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> terminalExecutor.execute(
                                "INSERT INTO enrollments " +
                                        "(student_id, course_id, grade) " +
                                        "VALUES (1, 100, 95);"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("PRIMARY KEY")
        );
    }

    private void createUsersTable() {

        terminalExecutor.execute(
                "CREATE TABLE users (" +
                        "id INT PRIMARY KEY, " +
                        "username STRING UNIQUE, " +
                        "email STRING NOT NULL" +
                        ");"
        );
    }
}
