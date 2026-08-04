package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.table.TableManager;
import com.yekdb.table.TableMetadata;

import java.util.Objects;

/**
 * Parser tarafından oluşturulan SQL komut nesnelerini
 * ilgili yönetici katmanlarına yönlendirerek çalıştırır.
 *
 * <p>Sprint 00-10 kapsamında veritabanı ve tablo yönetim
 * komutları desteklenmektedir.</p>
 *
 * <p>INSERT, SELECT ve DELETE komutlarının modelleri hazırdır;
 * fiziksel tablo-kayıt bağlantıları Sprint 00-11 kapsamında
 * yürütme katmanına bağlanacaktır.</p>
 */
public final class QueryExecutor {

    private final DatabaseManager databaseManager;

    /**
     * Aktif veritabanına bağlı tablo yöneticisi.
     *
     * USE DATABASE çalıştırıldığında yeniden oluşturulur.
     */
    private TableManager tableManager;

    /**
     * Yeni QueryExecutor oluşturur.
     *
     * @param databaseManager veritabanı yöneticisi
     */
    public QueryExecutor(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager,
                "DatabaseManager cannot be null."
        );

        initializeTableManager();
    }

    /**
     * Verilen komutu çalıştırır.
     *
     * @param command çalıştırılacak komut
     * @return yürütme sonucu
     */
    public ExecuteResult execute(Command command) {
        if (command == null) {
            throw new QueryExecutionException(
                    "Command cannot be null."
            );
        }

        try {
            if (command instanceof CreateDatabaseCommand value) {
                return executeCreateDatabase(value);
            }

            if (command instanceof UseDatabaseCommand value) {
                return executeUseDatabase(value);
            }

            if (command instanceof DropDatabaseCommand value) {
                return executeDropDatabase(value);
            }

            if (command instanceof CreateTableCommand value) {
                return executeCreateTable(value);
            }

            if (command instanceof DropTableCommand value) {
                return executeDropTable(value);
            }

            if (command instanceof InsertCommand) {
                return unsupportedRecordOperation("INSERT");
            }

            if (command instanceof SelectCommand) {
                return unsupportedRecordOperation("SELECT");
            }

            if (command instanceof DeleteCommand) {
                return unsupportedRecordOperation("DELETE");
            }

            throw new QueryExecutionException(
                    "Unsupported command type: "
                            + command.getClass().getSimpleName()
            );

        } catch (QueryExecutionException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new QueryExecutionException(
                    "Query execution failed for command: "
                            + command.getClass().getSimpleName(),
                    exception
            );
        }
    }

    /**
     * CREATE DATABASE komutunu çalıştırır.
     */
    private ExecuteResult executeCreateDatabase(
            CreateDatabaseCommand command
    ) {
        Database database = databaseManager.createDatabase(
                command.getDatabaseName()
        );

        return ExecuteResult.success(
                "Database created successfully: "
                        + database.getName()
        );
    }

    /**
     * USE DATABASE komutunu çalıştırır.
     */
    private ExecuteResult executeUseDatabase(
            UseDatabaseCommand command
    ) {
        Database database = databaseManager.useDatabase(
                command.getDatabaseName()
        );

        tableManager = new TableManager(
                database.getDatabasePath()
        );

        return ExecuteResult.success(
                "Database selected successfully: "
                        + database.getName()
        );
    }

    /**
     * DROP DATABASE komutunu çalıştırır.
     */
    private ExecuteResult executeDropDatabase(
            DropDatabaseCommand command
    ) {
        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        boolean droppingCurrentDatabase =
                currentDatabase != null
                        && currentDatabase.getName().equalsIgnoreCase(
                        command.getDatabaseName()
                );

        databaseManager.dropDatabase(
                command.getDatabaseName()
        );

        if (droppingCurrentDatabase) {
            tableManager = null;
        }

        return ExecuteResult.success(
                "Database dropped successfully: "
                        + command.getDatabaseName()
        );
    }

    /**
     * CREATE TABLE komutunu çalıştırır.
     */
    private ExecuteResult executeCreateTable(
            CreateTableCommand command
    ) {
        TableManager activeTableManager =
                requireTableManager();

        TableMetadata metadata =
                activeTableManager.createTable(
                        command.getTableName(),
                        command.getColumns()
                );

        return ExecuteResult.success(
                "Table created successfully: "
                        + metadata.getTableName()
        );
    }

    /**
     * DROP TABLE komutunu çalıştırır.
     */
    private ExecuteResult executeDropTable(
            DropTableCommand command
    ) {
        TableManager activeTableManager =
                requireTableManager();

        activeTableManager.dropTable(
                command.getTableName()
        );

        return ExecuteResult.success(
                "Table dropped successfully: "
                        + command.getTableName()
        );
    }

    /**
     * Henüz kayıt yürütme katmanına bağlanmamış komutlar
     * için açıklayıcı hata üretir.
     */
    private ExecuteResult unsupportedRecordOperation(
            String operationName
    ) {
        throw new QueryExecutionException(
                operationName
                        + " execution will be implemented "
                        + "in Sprint 00-11."
        );
    }

    /**
     * QueryExecutor oluşturulurken aktif bir veritabanı
     * varsa TableManager bağlantısını hazırlar.
     */
    private void initializeTableManager() {
        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase == null) {
            tableManager = null;
            return;
        }

        tableManager = new TableManager(
                currentDatabase.getDatabasePath()
        );
    }

    /**
     * Tablo işlemlerinden önce aktif veritabanı
     * bulunmasını zorunlu kılar.
     */
    private TableManager requireTableManager() {
        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase == null) {
            throw new QueryExecutionException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        if (tableManager == null) {
            tableManager = new TableManager(
                    currentDatabase.getDatabasePath()
            );
        }

        return tableManager;
    }
}