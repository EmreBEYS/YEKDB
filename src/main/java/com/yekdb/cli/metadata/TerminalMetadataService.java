package com.yekdb.cli.metadata;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseManager;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Interactive SQL terminal için metadata
 * sorgularını yönetir.
 *
 * CLI katmanının doğrudan TableManager ve
 * catalog detaylarına bağımlı olmasını önler.
 */
public final class TerminalMetadataService {

    private final DatabaseManager databaseManager;

    public TerminalMetadataService(
            DatabaseManager databaseManager
    ) {

        this.databaseManager =
                Objects.requireNonNull(
                        databaseManager,
                        "DatabaseManager cannot be null."
                );
    }

    /**
     * Aktif veritabanındaki tablo adlarını döndürür.
     */
    public List<String> listTables() {

        TableManager tableManager =
                createActiveTableManager();

        return new ArrayList<>(
                tableManager.listTableNames()
        );
    }

    /**
     * Verilen tablonun metadata bilgisini döndürür.
     */
    public Table describeTable(
            String tableName
    ) {

        if (tableName == null
                || tableName.isBlank()) {

            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        TableManager tableManager =
                createActiveTableManager();

        return tableManager.getTable(
                tableName
        );
    }

    /**
     * Verilen tablonun kolonlarını döndürür.
     */
    public List<Column> describeColumns(
            String tableName
    ) {

        return new ArrayList<>(
                describeTable(
                        tableName
                ).getColumns()
        );
    }

    /**
     * Aktif veritabanı adını döndürür.
     */
    public String getCurrentDatabaseName() {

        Database database =
                databaseManager.getCurrentDatabase();

        if (database == null) {

            throw new IllegalStateException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        return database.getName();
    }

    /**
     * Aktif veritabanı için güncel
     * TableManager oluşturur.
     *
     * Persistent catalog tekrar yüklenerek
     * terminalin güncel tablo listesini görmesi
     * sağlanır.
     */
    private TableManager createActiveTableManager() {

        Database database =
                databaseManager.getCurrentDatabase();

        if (database == null) {

            throw new IllegalStateException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        TableManager tableManager =
                new TableManager(
                        database.getDatabasePath()
                );

        tableManager.loadCatalog();

        return tableManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}