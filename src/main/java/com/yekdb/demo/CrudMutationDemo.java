package com.yekdb.demo;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.query.executor.QueryExecutor;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CrudMutationDemo {

    private CrudMutationDemo() {
    }

    public static void main(String[] args)
            throws Exception {

        System.out.println(
                "========================================"
        );
        System.out.println(
                " YEKDB - SPRINT 00-12 CRUD MUTATION DEMO"
        );
        System.out.println(
                "========================================"
        );

        /*
         * Demo verilerinin tutulacağı ana dizin.
         */
        Path dataRoot =
                Path.of(
                        "data",
                        "crud-demo"
                );

        Files.createDirectories(
                dataRoot
        );

        /*
         * Her çalıştırmada yeni DB oluşturarak
         * önceki demo çalıştırmalarıyla çakışmayı önlüyoruz.
         */
        String databaseName =
                "crud_demo_"
                        + System.currentTimeMillis();

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataRoot
                );

        /*
         * -------------------------------------------------
         * QUERY EXECUTION
         * -------------------------------------------------
         */
        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            printStep(
                    "1 - CREATE DATABASE"
            );

            ExecuteResult createDatabaseResult =
                    queryExecutor.execute(
                            "CREATE DATABASE "
                                    + databaseName
                                    + ";"
                    );

            printResult(
                    createDatabaseResult
            );


            printStep(
                    "2 - USE DATABASE"
            );

            ExecuteResult useDatabaseResult =
                    queryExecutor.execute(
                            "USE DATABASE "
                                    + databaseName
                                    + ";"
                    );

            printResult(
                    useDatabaseResult
            );


            printStep(
                    "3 - CREATE TABLE"
            );

            ExecuteResult createTableResult =
                    queryExecutor.execute(
                            """
                            CREATE TABLE users (
                                id INT,
                                name STRING,
                                age INT,
                                active BOOLEAN
                            );
                            """
                    );

            printResult(
                    createTableResult
            );


            /*
             * -------------------------------------------------
             * CREATE -> INSERT
             * -------------------------------------------------
             */
            printStep(
                    "4 - INSERT ROWS"
            );

            ExecuteResult insert1 =
                    queryExecutor.execute(
                            """
                            INSERT INTO users
                            (id, name, age, active)
                            VALUES
                            (1, 'Emre', 21, true);
                            """
                    );

            printResult(
                    insert1
            );

            ExecuteResult insert2 =
                    queryExecutor.execute(
                            """
                            INSERT INTO users
                            (id, name, age, active)
                            VALUES
                            (2, 'Ali', 24, true);
                            """
                    );

            printResult(
                    insert2
            );

            ExecuteResult insert3 =
                    queryExecutor.execute(
                            """
                            INSERT INTO users
                            (id, name, age, active)
                            VALUES
                            (3, 'Ayse', 26, true);
                            """
                    );

            printResult(
                    insert3
            );


            /*
             * -------------------------------------------------
             * UPDATE
             * -------------------------------------------------
             */
            printStep(
                    "5 - UPDATE"
            );

            ExecuteResult updateResult =
                    queryExecutor.execute(
                            """
                            UPDATE users
                            SET age = 22,
                                active = false
                            WHERE id = 1;
                            """
                    );

            printResult(
                    updateResult
            );


            /*
             * -------------------------------------------------
             * DELETE
             * -------------------------------------------------
             */
            printStep(
                    "6 - DELETE"
            );

            ExecuteResult deleteResult =
                    queryExecutor.execute(
                            """
                            DELETE FROM users
                            WHERE id = 2;
                            """
                    );

            printResult(
                    deleteResult
            );
        }


        /*
         * -------------------------------------------------
         * PHYSICAL STORAGE VERIFICATION
         * -------------------------------------------------
         *
         * QueryExecutor kapandıktan sonra .data dosyasını
         * yeniden açıyoruz.
         *
         * Böylece:
         *
         * INSERT
         * UPDATE
         * DELETE
         *
         * işlemlerinin yalnızca bellekte değil,
         * fiziksel dosyada da kalıcı olduğunu doğruluyoruz.
         */
        printStep(
                "7 - PHYSICAL STORAGE VERIFICATION"
        );

        Path dataFile =
                dataRoot
                        .resolve(
                                databaseName
                        )
                        .resolve(
                                "users.data"
                        );

        System.out.println(
                "Data file : "
                        + dataFile.toAbsolutePath()
        );

        System.out.println(
                "Exists    : "
                        + Files.exists(
                        dataFile
                )
        );

        if (Files.exists(dataFile)) {

            System.out.println(
                    "File size : "
                            + Files.size(
                            dataFile
                    )
                            + " bytes"
            );
        }


        StorageEngine storageEngine =
                new StorageEngine(
                        dataFile
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            /*
             * Logical delete uygulanmış kayıtlar
             * burada görünmeyecektir.
             */
            List<Record> activeRecords =
                    recordManager.getActiveRecords();

            System.out.println();
            System.out.println(
                    "Active record count: "
                            + activeRecords.size()
            );

            System.out.println(
                    "----------------------------------------"
            );

            for (Record record : activeRecords) {

                Row row =
                        recordManager.getRow(
                                record.getRecordId()
                        );

                System.out.println(
                        "Record ID : "
                                + record.getRecordId()
                );

                System.out.println(
                        "Row       : "
                                + row.getValues()
                );

                System.out.println(
                        "----------------------------------------"
                );
            }

        } finally {

            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }


        /*
         * -------------------------------------------------
         * EXPECTED FINAL STATE
         * -------------------------------------------------
         *
         * INSERT:
         * [1, Emre, 21, true]
         * [2, Ali, 24, true]
         * [3, Ayse, 26, true]
         *
         * UPDATE:
         * id = 1
         * [1, Emre, 22, false]
         *
         * DELETE:
         * id = 2 logical delete
         *
         * ACTIVE:
         * [1, Emre, 22, false]
         * [3, Ayse, 26, true]
         */
        System.out.println();

        System.out.println(
                "========================================"
        );

        System.out.println(
                " EXPECTED FINAL ACTIVE ROWS"
        );

        System.out.println(
                " [1, Emre, 22, false]"
        );

        System.out.println(
                " [3, Ayse, 26, true]"
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                " YEKDB CRUD MUTATION DEMO COMPLETED"
        );

        System.out.println(
                "========================================"
        );
    }


    private static void printStep(
            String title
    ) {

        System.out.println();
        System.out.println(
                "----------------------------------------"
        );

        System.out.println(
                title
        );

        System.out.println(
                "----------------------------------------"
        );
    }


    private static void printResult(
            ExecuteResult result
    ) {

        System.out.println(
                "Success : "
                        + result.isSuccess()
        );

        System.out.println(
                "Message : "
                        + result.getMessage()
        );
    }
}