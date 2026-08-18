package com.yekdb.query.executor;

import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateExpressionIntegrationTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private RecordManager recordManager;

    private UpdateExecutor updateExecutor;
    private ExpressionParser expressionParser;

    private Table table;

    @BeforeEach
    void setUp() throws IOException {

        table = new Table(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "name",
                                DataType.STRING
                        ),
                        new Column(
                                "age",
                                DataType.INT
                        ),
                        new Column(
                                "city",
                                DataType.STRING
                        ),
                        new Column(
                                "active",
                                DataType.BOOLEAN
                        ),
                        new Column(
                                "role",
                                DataType.STRING
                        )
                )
        );

        Path dataFile =
                tempDir.resolve(
                        "users.data"
                );

        storageEngine =
                new StorageEngine(
                        dataFile
                );

        storageEngine.initialize();

        /*
         * YEKDB RecordManager doğrudan StorageEngine almaz.
         *
         * PageManager + PageType kullanılır.
         */
        recordManager =
                new RecordManager(
                        storageEngine.getPageManager(),
                        PageType.DATA
                );

        updateExecutor =
                new UpdateExecutor();

        expressionParser =
                new ExpressionParser();

        insertTestRows();
    }

    @AfterEach
    void tearDown() throws IOException {

        if (storageEngine != null
                && storageEngine.isInitialized()) {

            storageEngine.shutdown();
        }
    }

    /**
     * Test kayıtları:
     *
     * 0 -> 1 | Yunus | 21 | Malatya  | true  | user
     * 1 -> 2 | Ali   | 17 | Ankara   | true  | user
     * 2 -> 3 | Ayşe  | 27 | Malatya  | false | admin
     * 3 -> 4 | Can   | 30 | Istanbul | true  | banned
     */
    private void insertTestRows()
            throws IOException {

        recordManager.insert(
                new Row(
                        List.of(
                                1,
                                "Yunus",
                                21,
                                "Malatya",
                                true,
                                "user"
                        )
                )
        );

        recordManager.insert(
                new Row(
                        List.of(
                                2,
                                "Ali",
                                17,
                                "Ankara",
                                true,
                                "user"
                        )
                )
        );

        recordManager.insert(
                new Row(
                        List.of(
                                3,
                                "Ayşe",
                                27,
                                "Malatya",
                                false,
                                "admin"
                        )
                )
        );

        recordManager.insert(
                new Row(
                        List.of(
                                4,
                                "Can",
                                30,
                                "Istanbul",
                                true,
                                "banned"
                        )
                )
        );
    }

    @Test
    void shouldUpdateRowsUsingAndExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age >= 18 AND city = 'Malatya'"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "active",
                                false
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertFalse(
                recordManager
                        .getRow(0)
                        .getValue(
                                4,
                                Boolean.class
                        )
        );

        assertFalse(
                recordManager
                        .getRow(2)
                        .getValue(
                                4,
                                Boolean.class
                        )
        );
    }

    @Test
    void shouldUpdateRowsUsingOrExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age < 18 OR role = 'admin'"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "updated"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertEquals(
                "updated",
                recordManager
                        .getRow(1)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "updated",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldUpdateRowsUsingNotExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "NOT active = true"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "inactive"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                1,
                updated
        );

        assertEquals(
                "inactive",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldUpdateUsingParenthesizedExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age >= 18 "
                                + "AND "
                                + "(city = 'Malatya' "
                                + "OR role = 'admin')"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "matched"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertEquals(
                "matched",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "matched",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldRespectAndPrecedenceOverOr()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age < 18 "
                                + "OR city = 'Malatya' "
                                + "AND active = false"
                );

        /*
         * Beklenen:
         *
         * age < 18
         * OR
         * (city = 'Malatya' AND active = false)
         *
         * Ali  -> true
         * Ayşe -> true
         */
        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "precedence"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertEquals(
                "precedence",
                recordManager
                        .getRow(1)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "precedence",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldAllowParenthesesToOverridePrecedence()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "(age < 18 OR city = 'Malatya') "
                                + "AND active = true"
                );

        /*
         * Yunus -> true
         * Ali   -> true
         * Ayşe  -> false
         * Can   -> false
         */
        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "parenthesized"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertEquals(
                "parenthesized",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "parenthesized",
                recordManager
                        .getRow(1)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldUpdateUsingComplexExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "(age >= 18 AND active = true) "
                                + "OR "
                                + "(city = 'Malatya' "
                                + "AND NOT role = 'admin')"
                );

        /*
         * Yunus -> true
         * Ali   -> false
         * Ayşe  -> false
         * Can   -> true
         */
        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "active",
                                false
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertFalse(
                recordManager
                        .getRow(0)
                        .getValue(
                                4,
                                Boolean.class
                        )
        );

        assertFalse(
                recordManager
                        .getRow(3)
                        .getValue(
                                4,
                                Boolean.class
                        )
        );
    }

    @Test
    void shouldEvaluateWhereColumnCaseInsensitive()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "CITY = 'Malatya' "
                                + "AND AGE >= 18"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "case-test"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        assertEquals(
                "case-test",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "case-test",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldReturnZeroWhenNoRowsMatch()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age > 100 "
                                + "AND city = 'Malatya'"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "never"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                0,
                updated
        );

        assertEquals(
                "user",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "admin",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldUpdateMultipleColumnsWithExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "id = 1"
                );

        UpdateCommand command =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "age",
                                22,
                                "active",
                                false,
                                "role",
                                "verified"
                        ),
                        where
                );

        int updated =
                updateExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                1,
                updated
        );

        Row updatedRow =
                recordManager.getRow(
                        0
                );

        assertEquals(
                22,
                updatedRow.getValue(
                        2,
                        Integer.class
                )
        );

        assertFalse(
                updatedRow.getValue(
                        4,
                        Boolean.class
                )
        );

        assertEquals(
                "verified",
                updatedRow.getValue(
                        5,
                        String.class
                )
        );
    }
}