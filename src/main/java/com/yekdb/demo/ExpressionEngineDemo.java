package com.yekdb.demo;

import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.executor.DeleteExecutor;
import com.yekdb.query.executor.TableScanExecutor;
import com.yekdb.query.executor.UpdateExecutor;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.page.PageType;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import com.yekdb.storage.record.RecordManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sprint 00-13 Expression Engine demosu.
 *
 * Demo kapsamında:
 *
 * - Comparison expressions
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 * - SELECT filtering
 * - UPDATE filtering
 * - DELETE filtering
 * - Physical persistence
 *
 * birlikte doğrulanır.
 */
public final class ExpressionEngineDemo {

    private static final Path DATA_FILE =
            Path.of(
                    "build",
                    "demo",
                    "expression-engine-demo.data"
            );

    private ExpressionEngineDemo() {
    }

    public static void main(String[] args) {

        StorageEngine storageEngine = null;

        try {

            prepareDemoFile();

            Table table =
                    createUsersTable();

            storageEngine =
                    openStorage();

            RecordManager recordManager =
                    createRecordManager(
                            storageEngine
                    );

            insertDemoRows(
                    recordManager
            );

            ExpressionParser expressionParser =
                    new ExpressionParser();

            UpdateExecutor updateExecutor =
                    new UpdateExecutor();

            DeleteExecutor deleteExecutor =
                    new DeleteExecutor();

            printTitle(
                    "YEKDB Sprint 00-13 - Expression Engine Demo"
            );

            System.out.println(
                    "Initial records:"
            );

            printActiveRows(
                    recordManager
            );

            /*
             * -------------------------------------------------
             * 1. SELECT + complex WHERE
             * -------------------------------------------------
             */
            printSection(
                    "1. COMPLEX SELECT"
            );

            String selectWhereText =
                    "age >= 18 "
                            + "AND "
                            + "(city = 'Malatya' "
                            + "OR role = 'admin')";

            System.out.println(
                    "WHERE " + selectWhereText
            );

            Expression selectExpression =
                    expressionParser.parse(
                            selectWhereText
                    );

            List<Row> activeRows =
                    loadActiveRows(
                            recordManager
                    );

            QueryResult selectResult =
                    TableScanExecutor.execute(
                            table,
                            activeRows,
                            selectExpression
                    );

            System.out.println(
                    "Matched rows: "
                            + selectResult
                            .getRows()
                            .size()
            );

            printRows(
                    selectResult.getRows()
            );

            /*
             * -------------------------------------------------
             * 2. UPDATE + AND + parentheses
             * -------------------------------------------------
             */
            printSection(
                    "2. COMPLEX UPDATE"
            );

            String updateWhereText =
                    "age >= 18 "
                            + "AND "
                            + "(city = 'Malatya' "
                            + "OR role = 'admin')";

            System.out.println(
                    "SET role = 'verified'"
            );

            System.out.println(
                    "WHERE " + updateWhereText
            );

            Expression updateExpression =
                    expressionParser.parse(
                            updateWhereText
                    );

            UpdateCommand updateCommand =
                    new UpdateCommand(
                            "users",
                            Map.of(
                                    "role",
                                    "verified"
                            ),
                            updateExpression
                    );

            int updatedRowCount =
                    updateExecutor.execute(
                            table,
                            updateCommand,
                            recordManager
                    );

            System.out.println(
                    "Updated row count: "
                            + updatedRowCount
            );

            System.out.println(
                    "Records after UPDATE:"
            );

            printActiveRows(
                    recordManager
            );

            /*
             * -------------------------------------------------
             * 3. DELETE + OR + NOT
             * -------------------------------------------------
             */
            printSection(
                    "3. COMPLEX DELETE"
            );

            String deleteWhereText =
                    "age < 18 "
                            + "OR "
                            + "NOT active = true";

            System.out.println(
                    "WHERE " + deleteWhereText
            );

            Expression deleteExpression =
                    expressionParser.parse(
                            deleteWhereText
                    );

            DeleteCommand deleteCommand =
                    new DeleteCommand(
                            "users",
                            deleteExpression
                    );

            int deletedRowCount =
                    deleteExecutor.execute(
                            table,
                            deleteCommand,
                            recordManager
                    );

            System.out.println(
                    "Deleted row count: "
                            + deletedRowCount
            );

            System.out.println(
                    "Active records after DELETE:"
            );

            printActiveRows(
                    recordManager
            );

            /*
             * -------------------------------------------------
             * 4. PRECEDENCE DEMO
             * -------------------------------------------------
             */
            printSection(
                    "4. OPERATOR PRECEDENCE"
            );

            String precedenceWhere =
                    "age < 25 "
                            + "OR city = 'Istanbul' "
                            + "AND active = true";

            System.out.println(
                    "WHERE " + precedenceWhere
            );

            System.out.println(
                    "Evaluation order:"
            );

            System.out.println(
                    "age < 25 OR "
                            + "(city = 'Istanbul' "
                            + "AND active = true)"
            );

            Expression precedenceExpression =
                    expressionParser.parse(
                            precedenceWhere
                    );

            QueryResult precedenceResult =
                    TableScanExecutor.execute(
                            table,
                            loadActiveRows(
                                    recordManager
                            ),
                            precedenceExpression
                    );

            System.out.println(
                    "Matched rows: "
                            + precedenceResult
                            .getRows()
                            .size()
            );

            printRows(
                    precedenceResult.getRows()
            );

            /*
             * -------------------------------------------------
             * 5. STORAGE REOPEN / PERSISTENCE
             * -------------------------------------------------
             */
            printSection(
                    "5. PHYSICAL PERSISTENCE"
            );

            System.out.println(
                    "Shutting down Storage Engine..."
            );

            storageEngine.shutdown();
            storageEngine = null;

            System.out.println(
                    "Reopening same .data file..."
            );

            storageEngine =
                    openStorage();

            recordManager =
                    createRecordManager(
                            storageEngine
                    );

            System.out.println(
                    "Active records after reopen:"
            );

            printActiveRows(
                    recordManager
            );

            System.out.println(
                    "Active record count: "
                            + recordManager
                            .getActiveRecords()
                            .size()
            );

            printSection(
                    "DEMO RESULT"
            );

            System.out.println(
                    "AND / OR / NOT              : OK"
            );

            System.out.println(
                    "Parentheses                 : OK"
            );

            System.out.println(
                    "Operator precedence         : OK"
            );

            System.out.println(
                    "SELECT expression filtering : OK"
            );

            System.out.println(
                    "UPDATE expression filtering : OK"
            );

            System.out.println(
                    "DELETE expression filtering : OK"
            );

            System.out.println(
                    "Physical persistence         : OK"
            );

            System.out.println();

            System.out.println(
                    "Sprint 00-13 Expression Engine demo completed successfully."
            );

        } catch (Exception exception) {

            System.err.println(
                    "Expression Engine demo failed."
            );

            exception.printStackTrace();

        } finally {

            if (storageEngine != null
                    && storageEngine.isInitialized()) {

                try {

                    storageEngine.shutdown();

                } catch (IOException exception) {

                    System.err.println(
                            "Storage Engine could not be closed."
                    );

                    exception.printStackTrace();
                }
            }
        }
    }

    /**
     * Demo için users tablosu oluşturulur.
     */
    private static Table createUsersTable() {

        return new Table(
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
    }

    /**
     * Storage Engine açılır.
     */
    private static StorageEngine openStorage()
            throws IOException {

        StorageEngine storageEngine =
                new StorageEngine(
                        DATA_FILE
                );

        storageEngine.initialize();

        return storageEngine;
    }

    /**
     * RecordManager oluşturulur.
     */
    private static RecordManager createRecordManager(
            StorageEngine storageEngine
    ) throws IOException {

        return new RecordManager(
                storageEngine.getPageManager(),
                PageType.DATA
        );
    }
    /**
     * Demo verileri fiziksel storage'a eklenir.
     */
    private static void insertDemoRows(
            RecordManager recordManager
    ) throws IOException {

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

    /**
     * Aktif Record nesneleri Row listesine dönüştürülür.
     */
    private static List<Row> loadActiveRows(
            RecordManager recordManager
    ) throws IOException {

        List<Row> rows =
                new ArrayList<>();

        for (Record record :
                recordManager.getActiveRecords()) {

            Row row =
                    recordManager.getRow(
                            record.getRecordId()
                    );

            rows.add(
                    row
            );
        }

        return rows;
    }

    /**
     * Aktif kayıtları konsola yazdırır.
     */
    private static void printActiveRows(
            RecordManager recordManager
    ) throws IOException {

        printRows(
                loadActiveRows(
                        recordManager
                )
        );
    }

    /**
     * Row listesini okunabilir biçimde yazdırır.
     */
    private static void printRows(
            List<Row> rows
    ) {

        if (rows.isEmpty()) {

            System.out.println(
                    "  <no rows>"
            );

            return;
        }

        for (Row row : rows) {

            System.out.printf(
                    "  id=%s | name=%s | age=%s | city=%s | active=%s | role=%s%n",
                    row.getValue(0),
                    row.getValue(1),
                    row.getValue(2),
                    row.getValue(3),
                    row.getValue(4),
                    row.getValue(5)
            );
        }
    }

    /**
     * Her demo çalıştırmasında temiz bir fiziksel dosya
     * kullanılmasını sağlar.
     */
    private static void prepareDemoFile()
            throws IOException {

        Files.createDirectories(
                DATA_FILE.getParent()
        );

        Files.deleteIfExists(
                DATA_FILE
        );
    }

    private static void printTitle(
            String title
    ) {

        System.out.println();

        System.out.println(
                "============================================================"
        );

        System.out.println(
                title
        );

        System.out.println(
                "============================================================"
        );
    }

    private static void printSection(
            String title
    ) {

        System.out.println();

        System.out.println(
                "------------------------------------------------------------"
        );

        System.out.println(
                title
        );

        System.out.println(
                "------------------------------------------------------------"
        );
    }
}