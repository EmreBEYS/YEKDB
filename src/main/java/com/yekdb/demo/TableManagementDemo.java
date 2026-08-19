package com.yekdb.demo;

import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import com.yekdb.storage.table.TableMetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sprint 00-07 Table Management Layer demo uygulaması.
 *
 * Bu demo:
 * - Tablo oluşturur
 * - Fiziksel .tbl dosyalarını üretir
 * - Tabloları listeler
 * - Metadata bilgilerini gösterir
 * - Bir tabloyu siler
 */
public class TableManagementDemo {

    public static void main(String[] args) {

        printHeader();

        Path databaseDirectory = Path.of(
                "data",
                "demo_company"
        );

        TableManager tableManager =
                new TableManager(databaseDirectory);

        try {
            cleanPreviousDemo(tableManager);

            Table usersTable = createUsersTable();
            Table productsTable = createProductsTable();
            Table ordersTable = createOrdersTable();

            System.out.println(
                    "[1] Creating tables..."
            );

            TableMetadata usersMetadata =
                    tableManager.createTable(usersTable);

            TableMetadata productsMetadata =
                    tableManager.createTable(productsTable);

            TableMetadata ordersMetadata =
                    tableManager.createTable(ordersTable);

            System.out.println(
                    "    Table created: "
                            + usersMetadata.getTableName()
            );

            System.out.println(
                    "    Table created: "
                            + productsMetadata.getTableName()
            );

            System.out.println(
                    "    Table created: "
                            + ordersMetadata.getTableName()
            );

            printSeparator();

            System.out.println(
                    "[2] Current tables"
            );

            printTableList(tableManager);

            printSeparator();

            System.out.println(
                    "[3] Table metadata"
            );

            printMetadata(tableManager);

            printSeparator();

            System.out.println(
                    "[4] Physical table files"
            );

            printPhysicalFiles(
                    databaseDirectory,
                    tableManager
            );

            printSeparator();

            System.out.println(
                    "[5] Inspecting users table"
            );

            printTableSchema(
                    tableManager.getTable("users")
            );

            printSeparator();

            System.out.println(
                    "[6] Dropping table: orders"
            );

            tableManager.dropTable("orders");

            System.out.println(
                    "    Table dropped successfully."
            );

            printSeparator();

            System.out.println(
                    "[7] Final table list"
            );

            printTableList(tableManager);

            printSeparator();

            System.out.println(
                    "[8] Demo result"
            );

            System.out.println(
                    "    Database directory : "
                            + tableManager.getDatabaseDirectory()
            );

            System.out.println(
                    "    Table count        : "
                            + tableManager.getTableCount()
            );

            System.out.println(
                    "    users exists       : "
                            + tableManager.exists("users")
            );

            System.out.println(
                    "    products exists    : "
                            + tableManager.exists("products")
            );

            System.out.println(
                    "    orders exists      : "
                            + tableManager.exists("orders")
            );

            printSeparator();

            System.out.println(
                    "Sprint 00-07 demo completed successfully."
            );

        } catch (Exception exception) {

            System.err.println();
            System.err.println(
                    "Demo failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    /**
     * Users tablosunu oluşturur.
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
                                "email",
                                DataType.STRING
                        ),
                        new Column(
                                "active",
                                DataType.BOOLEAN
                        )
                )
        );
    }

    /**
     * Products tablosunu oluşturur.
     */
    private static Table createProductsTable() {

        return new Table(
                "products",
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
                                "price",
                                DataType.DOUBLE
                        ),
                        new Column(
                                "stock",
                                DataType.INT
                        )
                )
        );
    }

    /**
     * Orders tablosunu oluşturur.
     */
    private static Table createOrdersTable() {

        return new Table(
                "orders",
                List.of(
                        new Column(
                                "id",
                                DataType.LONG
                        ),
                        new Column(
                                "user_id",
                                DataType.INT
                        ),
                        new Column(
                                "product_id",
                                DataType.INT
                        ),
                        new Column(
                                "total",
                                DataType.DOUBLE
                        )
                )
        );
    }

    /**
     * Katalogdaki tabloları listeler.
     */
    private static void printTableList(
            TableManager tableManager
    ) {

        if (tableManager.listTableNames().isEmpty()) {
            System.out.println(
                    "    No tables found."
            );
            return;
        }

        for (String tableName
                : tableManager.listTableNames()) {

            System.out.println(
                    "    - " + tableName
            );
        }
    }

    /**
     * Metadata bilgilerini yazdırır.
     */
    private static void printMetadata(
            TableManager tableManager
    ) {

        for (String tableName
                : tableManager.listTableNames()) {

            TableMetadata metadata =
                    tableManager.getMetadata(tableName);

            System.out.println(
                    "    Table        : "
                            + metadata.getTableName()
            );

            System.out.println(
                    "    Column count : "
                            + metadata.getColumnCount()
            );

            System.out.println(
                    "    File name    : "
                            + metadata.getFileName()
            );

            System.out.println(
                    "    Version      : "
                            + metadata.getVersion()
            );

            System.out.println(
                    "    Created at   : "
                            + metadata.getCreatedAt()
            );

            System.out.println();
        }
    }

    /**
     * Fiziksel .tbl dosyalarını kontrol eder.
     */
    private static void printPhysicalFiles(
            Path databaseDirectory,
            TableManager tableManager
    ) {

        for (String tableName
                : tableManager.listTableNames()) {

            Path tableFile =
                    databaseDirectory.resolve(
                            tableName + ".tbl"
                    );

            System.out.println(
                    "    "
                            + tableFile
                            + " -> "
                            + Files.exists(tableFile)
            );
        }
    }

    /**
     * Bir tablonun sütun şemasını yazdırır.
     */
    private static void printTableSchema(
            Table table
    ) {

        System.out.println(
                "    Table name   : "
                        + table.getTableName()
        );

        System.out.println(
                "    Column count : "
                        + table.getColumnCount()
        );

        System.out.println(
                "    Columns"
        );

        table.getColumns().forEach(
                column -> System.out.println(
                        "      - "
                                + column.getName()
                                + " "
                                + column.getDataType()
                )
        );
    }

    /**
     * Önceki demo çalışmasından kalan tabloları temizler.
     */
    private static void cleanPreviousDemo(
            TableManager tableManager
    ) {

        List<String> tableNames = List.of(
                "users",
                "products",
                "orders"
        );

        for (String tableName : tableNames) {

            if (tableManager.exists(tableName)) {

                try {
                    tableManager.dropTable(tableName);
                } catch (Exception ignored) {
                    /*
                     * Yeni TableManager açıldığında katalog boş olabilir,
                     * fakat diskte eski demo dosyaları bulunabilir.
                     * Bu durum aşağıda fiziksel olarak temizlenir.
                     */
                }
            }

            Path tableFile =
                    tableManager
                            .getDatabaseDirectory()
                            .resolve(
                                    tableName + ".tbl"
                            );

            try {
                Files.deleteIfExists(tableFile);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Previous demo file could not be deleted: "
                                + tableFile,
                        exception
                );
            }
        }
    }

    private static void printHeader() {

        System.out.println();
        System.out.println(
                "========================================"
        );

        System.out.println(
                "       YEKDB TABLE MANAGEMENT DEMO"
        );

        System.out.println(
                "             Sprint 00-07"
        );

        System.out.println(
                "========================================"
        );

        System.out.println();
    }

    private static void printSeparator() {

        System.out.println();
        System.out.println(
                "----------------------------------------"
        );

        System.out.println();
    }
}