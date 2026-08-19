package com.yekdb.query.executor;

import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteExpressionIntegrationTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private RecordManager recordManager;

    private DeleteExecutor deleteExecutor;
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

        recordManager =
                new RecordManager(
                        storageEngine.getPageManager(),
                        PageType.DATA
                );

        deleteExecutor =
                new DeleteExecutor();

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
    void shouldDeleteRowsUsingAndExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age >= 18 AND city = 'Malatya'"
                );

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        /*
         * Yunus + Ayşe
         */
        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    @Test
    void shouldDeleteRowsUsingOrExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age < 18 OR role = 'admin'"
                );

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        /*
         * Ali + Ayşe
         */
        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    @Test
    void shouldDeleteRowsUsingNotExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "NOT active = true"
                );

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        /*
         * Sadece Ayşe active=false.
         */
        assertEquals(
                1,
                deleted
        );

        assertEquals(
                3,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    @Test
    void shouldDeleteUsingParenthesizedExpression()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age >= 18 "
                                + "AND "
                                + "(city = 'Malatya' "
                                + "OR role = 'admin')"
                );

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        /*
         * Yunus + Ayşe
         */
        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
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
        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
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
        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    @Test
    void shouldDeleteUsingComplexExpression()
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
        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
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

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                2,
                deleted
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
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

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        where
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                0,
                deleted
        );

        assertEquals(
                4,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    @Test
    void shouldDeleteAllRowsWhenWhereExpressionIsNull()
            throws IOException {

        DeleteCommand command =
                new DeleteCommand(
                        "users",
                        null
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        command,
                        recordManager
                );

        assertEquals(
                4,
                deleted
        );

        assertEquals(
                0,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }
}