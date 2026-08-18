package com.yekdb.query.executor;

import com.yekdb.query.command.DeleteCommand;
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

class CrudExpressionPersistenceIntegrationTest {

    @TempDir
    Path tempDir;

    private Path dataFile;

    private StorageEngine storageEngine;
    private RecordManager recordManager;

    private UpdateExecutor updateExecutor;
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

        dataFile =
                tempDir.resolve(
                        "users.data"
                );

        openStorage();

        updateExecutor =
                new UpdateExecutor();

        deleteExecutor =
                new DeleteExecutor();

        expressionParser =
                new ExpressionParser();

        insertTestRows();
    }

    @AfterEach
    void tearDown() throws IOException {

        shutdownStorage();
    }

    /**
     * Storage Engine açılır ve RecordManager oluşturulur.
     */
    private void openStorage()
            throws IOException {

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
    }

    /**
     * Storage Engine güvenli şekilde kapatılır.
     */
    private void shutdownStorage()
            throws IOException {

        if (storageEngine != null
                && storageEngine.isInitialized()) {

            storageEngine.shutdown();
        }
    }

    /**
     * Storage kapatılıp aynı fiziksel dosya üzerinden
     * yeniden açılır.
     */
    private void reopenStorage()
            throws IOException {

        shutdownStorage();

        openStorage();
    }

    /**
     * Başlangıç kayıtları:
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
    void shouldPersistComplexUpdateAfterStorageReopen()
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
                2,
                updated
        );

        /*
         * Bellekte doğru mu?
         */
        assertEquals(
                "verified",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "verified",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );

        /*
         * Storage yeniden açılıyor.
         */
        reopenStorage();

        /*
         * Fiziksel persistence doğrulaması.
         */
        Row yunus =
                recordManager.getRow(
                        0
                );

        Row ayse =
                recordManager.getRow(
                        2
                );

        assertFalse(
                yunus.getValue(
                        4,
                        Boolean.class
                )
        );

        assertEquals(
                "verified",
                yunus.getValue(
                        5,
                        String.class
                )
        );

        assertFalse(
                ayse.getValue(
                        4,
                        Boolean.class
                )
        );

        assertEquals(
                "verified",
                ayse.getValue(
                        5,
                        String.class
                )
        );
    }

    @Test
    void shouldPersistLogicalDeleteAfterStorageReopen()
            throws IOException {

        Expression where =
                expressionParser.parse(
                        "age < 18 "
                                + "OR role = 'banned'"
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
         * Ali + Can
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

        reopenStorage();

        /*
         * Deleted flag fiziksel dosyada korunmalı.
         */
        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );

        List<Row> activeRows =
                recordManager
                        .getActiveRecords()
                        .stream()
                        .map(record -> {
                            try {
                                return recordManager.getRow(
                                        record.getRecordId()
                                );
                            } catch (IOException exception) {
                                throw new RuntimeException(
                                        exception
                                );
                            }
                        })
                        .toList();

        assertEquals(
                2,
                activeRows.size()
        );

        /*
         * Yunus ve Ayşe aktif kalmalı.
         */
        assertTrue(
                activeRows.stream()
                        .anyMatch(
                                row ->
                                        row.getValue(
                                                0,
                                                Integer.class
                                        ) == 1
                        )
        );

        assertTrue(
                activeRows.stream()
                        .anyMatch(
                                row ->
                                        row.getValue(
                                                0,
                                                Integer.class
                                        ) == 3
                        )
        );
    }

    @Test
    void shouldPersistUpdateAndDeleteInSameLifecycle()
            throws IOException {

        /*
         * Önce:
         *
         * Yunus + Can güncellenecek.
         */
        Expression updateWhere =
                expressionParser.parse(
                        "(age >= 18 AND active = true) "
                                + "OR role = 'banned'"
                );

        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "processed"
                        ),
                        updateWhere
                );

        int updated =
                updateExecutor.execute(
                        table,
                        updateCommand,
                        recordManager
                );

        assertEquals(
                2,
                updated
        );

        /*
         * Sonra:
         *
         * Ayşe silinecek.
         */
        Expression deleteWhere =
                expressionParser.parse(
                        "city = 'Malatya' "
                                + "AND NOT active = true"
                );

        DeleteCommand deleteCommand =
                new DeleteCommand(
                        "users",
                        deleteWhere
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        deleteCommand,
                        recordManager
                );

        assertEquals(
                1,
                deleted
        );

        reopenStorage();

        /*
         * Ayşe logical delete olduğu için
         * üç aktif kayıt kalmalı.
         */
        assertEquals(
                3,
                recordManager
                        .getActiveRecords()
                        .size()
        );

        /*
         * Yunus update'i korunmalı.
         */
        assertEquals(
                "processed",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        /*
         * Can update'i korunmalı.
         */
        assertEquals(
                "processed",
                recordManager
                        .getRow(3)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }

    @Test
    void shouldPreserveUnaffectedRowsAfterUpdateAndDelete()
            throws IOException {

        Expression updateWhere =
                expressionParser.parse(
                        "id = 1"
                );

        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "age",
                                22
                        ),
                        updateWhere
                );

        int updated =
                updateExecutor.execute(
                        table,
                        updateCommand,
                        recordManager
                );

        assertEquals(
                1,
                updated
        );

        Expression deleteWhere =
                expressionParser.parse(
                        "id = 4"
                );

        DeleteCommand deleteCommand =
                new DeleteCommand(
                        "users",
                        deleteWhere
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        deleteCommand,
                        recordManager
                );

        assertEquals(
                1,
                deleted
        );

        reopenStorage();

        /*
         * Üç aktif kayıt kalmalı.
         */
        assertEquals(
                3,
                recordManager
                        .getActiveRecords()
                        .size()
        );

        /*
         * Yunus güncellendi.
         */
        assertEquals(
                22,
                recordManager
                        .getRow(0)
                        .getValue(
                                2,
                                Integer.class
                        )
        );

        /*
         * Ali değişmemeli.
         */
        Row ali =
                recordManager.getRow(
                        1
                );

        assertEquals(
                17,
                ali.getValue(
                        2,
                        Integer.class
                )
        );

        assertEquals(
                "user",
                ali.getValue(
                        5,
                        String.class
                )
        );

        /*
         * Ayşe değişmemeli.
         */
        Row ayse =
                recordManager.getRow(
                        2
                );

        assertEquals(
                27,
                ayse.getValue(
                        2,
                        Integer.class
                )
        );

        assertEquals(
                "admin",
                ayse.getValue(
                        5,
                        String.class
                )
        );
    }

    @Test
    void shouldPersistCaseInsensitiveComplexExpressionOperations()
            throws IOException {

        Expression updateWhere =
                expressionParser.parse(
                        "CITY = 'Malatya' "
                                + "AND "
                                + "(AGE >= 18 OR ROLE = 'user')"
                );

        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        Map.of(
                                "role",
                                "case-verified"
                        ),
                        updateWhere
                );

        int updated =
                updateExecutor.execute(
                        table,
                        updateCommand,
                        recordManager
                );

        /*
         * Yunus + Ayşe
         */
        assertEquals(
                2,
                updated
        );

        Expression deleteWhere =
                expressionParser.parse(
                        "ROLE = 'banned' "
                                + "OR "
                                + "(AGE < 18 AND ACTIVE = true)"
                );

        DeleteCommand deleteCommand =
                new DeleteCommand(
                        "users",
                        deleteWhere
                );

        int deleted =
                deleteExecutor.execute(
                        table,
                        deleteCommand,
                        recordManager
                );

        /*
         * Can + Ali
         */
        assertEquals(
                2,
                deleted
        );

        reopenStorage();

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );

        assertEquals(
                "case-verified",
                recordManager
                        .getRow(0)
                        .getValue(
                                5,
                                String.class
                        )
        );

        assertEquals(
                "case-verified",
                recordManager
                        .getRow(2)
                        .getValue(
                                5,
                                String.class
                        )
        );
    }
}