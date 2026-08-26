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
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.query.datasource.QueryDataSource;
import com.yekdb.query.mapper.StatementCommandMapper;
import com.yekdb.query.parser.SqlParser;
import com.yekdb.query.statement.Statement;
import com.yekdb.storage.table.TableManager;
import com.yekdb.storage.table.TableMetadata;

import java.util.Locale;
import java.util.Objects;

/**
 * Parser veya istemci katmanı tarafından oluşturulan SQL komutlarını
 * ilgili yönetim ve sorgu yürütme katmanlarına yönlendirir.
 *
 * Desteklenen Command türleri:
 *
 * - CreateDatabaseCommand
 * - UseDatabaseCommand
 * - DropDatabaseCommand
 * - CreateTableCommand
 * - DropTableCommand
 * - InsertCommand
 * - UpdateCommand
 * - DeleteCommand
 * - SelectCommand
 *
 * Eski testler ve istemciler için execute(String) desteği de bulunur.
 */
public final class QueryExecutor implements AutoCloseable {

    /**
     * Veritabanı oluşturma, seçme ve silme işlemlerini yönetir.
     */
    private final DatabaseManager databaseManager;

    /**
     * SELECT sorgularında tablo ve satır verilerinin
     * alınacağı veri kaynağıdır.
     *
     * Yalnızca yönetim komutları kullanılacaksa null olabilir.
     */
    private final QueryDataSource queryDataSource;

    /**
     * SELECT sorgularını optimizer ve tarama katmanları
     * üzerinden çalıştırır.
     */
    private final SelectExecutor selectExecutor;

    /**
     * INSERT komutlarını tablo şeması ve fiziksel
     * RecordManager üzerinden çalıştırır.
     */
    private final InsertExecutor insertExecutor;

    /**
     * UPDATE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
     */
    private final UpdateExecutor updateExecutor;

    /**
     * DELETE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
     */
    private final DeleteExecutor deleteExecutor;

    /**
     * INSERT / UPDATE / DELETE fiziksel storage yaşam döngüsünü
     * QueryExecutor dışında yönetir.
     */
    private final TableMutationExecutionSupport
            mutationExecutionSupport;

    /**
     * SELECT için QueryDataSource hazırlama ve JOIN veri yükleme
     * sorumluluğunu kapsüller.
     */
    private final SelectCommandExecutionSupport
            selectExecutionSupport;

    /**
     * Management SQL komutlarını ayrıştırır.
     */
    private final ManagementCommandParser
            managementCommandParser;

    /**
     * Aktif veritabanına bağlı tablo yöneticisidir.
     */
    private TableManager tableManager;

    /**
     * Yalnızca veritabanı ve tablo yönetimi desteği bulunan
     * QueryExecutor oluşturur.
     */
    public QueryExecutor(
            DatabaseManager databaseManager
    ) {

        this(
                databaseManager,
                null,
                new SelectExecutor(),
                new InsertExecutor(),
                new UpdateExecutor(),
                new DeleteExecutor()
        );
    }

    /**
     * SELECT desteği bulunan QueryExecutor oluşturur.
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource
    ) {

        this(
                databaseManager,
                queryDataSource,
                new SelectExecutor(),
                new InsertExecutor(),
                new UpdateExecutor(),
                new DeleteExecutor()
        );
    }

    /**
     * Eski testler ve istemciler için üç parametreli
     * constructor korunur.
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            SelectExecutor selectExecutor
    ) {

        this(
                databaseManager,
                queryDataSource,
                selectExecutor,
                new InsertExecutor(),
                new UpdateExecutor(),
                new DeleteExecutor()
        );
    }

    /**
     * Eski dört parametreli constructor korunur.
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            SelectExecutor selectExecutor,
            InsertExecutor insertExecutor
    ) {

        this(
                databaseManager,
                queryDataSource,
                selectExecutor,
                insertExecutor,
                new UpdateExecutor(),
                new DeleteExecutor()
        );
    }

    /**
     * Eski beş parametreli constructor korunur.
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            SelectExecutor selectExecutor,
            InsertExecutor insertExecutor,
            UpdateExecutor updateExecutor
    ) {

        this(
                databaseManager,
                queryDataSource,
                selectExecutor,
                insertExecutor,
                updateExecutor,
                new DeleteExecutor()
        );
    }

    /**
     * Bütün bağımlılıkların dışarıdan verilebildiği constructor.
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            SelectExecutor selectExecutor,
            InsertExecutor insertExecutor,
            UpdateExecutor updateExecutor,
            DeleteExecutor deleteExecutor
    ) {

        this.databaseManager =
                Objects.requireNonNull(
                        databaseManager,
                        "DatabaseManager cannot be null."
                );

        this.queryDataSource =
                queryDataSource;

        this.selectExecutor =
                Objects.requireNonNull(
                        selectExecutor,
                        "SelectExecutor cannot be null."
                );

        this.insertExecutor =
                Objects.requireNonNull(
                        insertExecutor,
                        "InsertExecutor cannot be null."
                );

        this.updateExecutor =
                Objects.requireNonNull(
                        updateExecutor,
                        "UpdateExecutor cannot be null."
                );

        this.deleteExecutor =
                Objects.requireNonNull(
                        deleteExecutor,
                        "DeleteExecutor cannot be null."
                );

        this.mutationExecutionSupport =
                new TableMutationExecutionSupport(
                        this.insertExecutor,
                        this.updateExecutor,
                        this.deleteExecutor
                );

        this.selectExecutionSupport =
                new SelectCommandExecutionSupport(
                        this.selectExecutor
                );

        this.managementCommandParser =
                new ManagementCommandParser();

        initializeTableManager();
    }

    /**
     * SQL metnini uygun Command nesnesine dönüştürerek çalıştırır.
     *
     * Desteklenen:
     *
     * CREATE DATABASE
     * USE DATABASE
     * USE
     * DROP DATABASE
     * CREATE TABLE
     * DROP TABLE
     * INSERT
     * SELECT
     * UPDATE
     * DELETE
     */
    public ExecuteResult execute(
            String sql
    ) {

        if (sql == null
                || sql.isBlank()) {

            throw new QueryExecutionException(
                    "SQL statement cannot be null or blank."
            );
        }

        String normalizedSql =
                removeTrailingSemicolon(
                        sql.trim()
                );

        /*
         * SQL komutunun ilk keyword'ünü whitespace bağımsız
         * şekilde belirle.
         */
        String firstKeyword =
                normalizedSql
                        .split("\\s+", 2)[0]
                        .toUpperCase(
                                Locale.ROOT
                        );

        /*
         * DML / SELECT işlemleri gerçek SQL parser
         * pipeline'ından geçer.
         */
        if (firstKeyword.equals("INSERT")
                || firstKeyword.equals("SELECT")
                || firstKeyword.equals("UPDATE")
                || firstKeyword.equals("DELETE")) {

            Statement statement;

            try {

                statement =
                        new SqlParser().parse(
                                normalizedSql
                        );

            } catch (RuntimeException exception) {

                throw new QueryExecutionException(
                        "SQL parsing failed: "
                                + exception.getMessage(),
                        exception
                );
            }

            Command command;

            try {

                command =
                        StatementCommandMapper.map(
                                statement
                        );

            } catch (RuntimeException exception) {

                if (exception
                        instanceof QueryExecutionException
                        queryExecutionException) {

                    throw queryExecutionException;
                }

                throw new QueryExecutionException(
                        "Statement mapping failed: "
                                + exception.getMessage(),
                        exception
                );
            }

            return execute(
                    command
            );
        }

        /*
         * Management komutları mevcut
         * ManagementCommandParser üzerinden yürütülür.
         */
        Command command =
                parseSqlCommand(
                        normalizedSql
                );

        return execute(
                command
        );
    }

    /**
     * Verilen Command nesnesini ilgili executor
     * veya management metoduna yönlendirir.
     */
    public ExecuteResult execute(
            Command command
    ) {

        if (command == null) {

            throw new QueryExecutionException(
                    "Command cannot be null."
            );
        }

        try {

            if (command
                    instanceof CreateDatabaseCommand value) {

                return executeCreateDatabase(
                        value
                );
            }

            if (command
                    instanceof UseDatabaseCommand value) {

                return executeUseDatabase(
                        value
                );
            }

            if (command
                    instanceof DropDatabaseCommand value) {

                return executeDropDatabase(
                        value
                );
            }

            if (command
                    instanceof CreateTableCommand value) {

                return executeCreateTable(
                        value
                );
            }

            if (command
                    instanceof DropTableCommand value) {

                return executeDropTable(
                        value
                );
            }

            if (command
                    instanceof InsertCommand value) {

                return executeInsert(
                        value
                );
            }

            if (command
                    instanceof UpdateCommand value) {

                return executeUpdate(
                        value
                );
            }

            if (command
                    instanceof SelectCommand value) {

                return executeSelect(
                        value
                );
            }

            if (command
                    instanceof DeleteCommand value) {

                return executeDelete(
                        value
                );
            }

            throw new QueryExecutionException(
                    "Unsupported command type: "
                            + command
                            .getClass()
                            .getSimpleName()
            );

        } catch (
                QueryExecutionException exception
        ) {

            throw exception;

        } catch (
                RuntimeException exception
        ) {

            throw createExecutionException(
                    command,
                    exception
            );
        }
    }



    /**
     * Alt execution katmanından gelen RuntimeException mesajını
     * kaybetmeden QueryExecutionException'a dönüştürür.
     */
    private QueryExecutionException createExecutionException(
            Command command,
            RuntimeException exception
    ) {

        String message =
                resolveExecutionErrorMessage(
                        command,
                        exception
                );

        return new QueryExecutionException(
                message,
                exception
        );
    }

    /**
     * Execution hatası için kullanıcıya gösterilecek
     * en anlamlı mesajı belirler.
     */
    private String resolveExecutionErrorMessage(
            Command command,
            RuntimeException exception
    ) {

        String exceptionMessage =
                exception.getMessage();

        /*
         * Alt katman anlamlı bir hata mesajı üretmişse
         * bunu olduğu gibi koruruz.
         */
        if (exceptionMessage != null
                && !exceptionMessage.isBlank()) {

            return exceptionMessage.trim();
        }

        /*
         * Mesajı olmayan beklenmeyen durumlarda
         * Java command sınıf adı yerine SQL operasyonunu
         * kullanıcıya gösteririz.
         */
        return resolveOperationName(
                command
        ) + " execution failed.";
    }

    /**
     * Command modelini kullanıcı dostu SQL
     * operation adına dönüştürür.
     */
    private String resolveOperationName(
            Command command
    ) {

        if (command instanceof SelectCommand) {
            return "SELECT";
        }

        if (command instanceof InsertCommand) {
            return "INSERT";
        }

        if (command instanceof UpdateCommand) {
            return "UPDATE";
        }

        if (command instanceof DeleteCommand) {
            return "DELETE";
        }

        if (command instanceof CreateTableCommand) {
            return "CREATE TABLE";
        }

        if (command instanceof DropTableCommand) {
            return "DROP TABLE";
        }

        if (command instanceof CreateDatabaseCommand) {
            return "CREATE DATABASE";
        }

        if (command instanceof DropDatabaseCommand) {
            return "DROP DATABASE";
        }

        if (command instanceof UseDatabaseCommand) {
            return "USE DATABASE";
        }

        return "Query";
    }

    /**
     * CREATE DATABASE.
     */
    private ExecuteResult executeCreateDatabase(
            CreateDatabaseCommand command
    ) {

        Database database =
                databaseManager.createDatabase(
                        command.getDatabaseName()
                );

        return ExecuteResult.success(
                "Database created successfully: "
                        + database.getName()
        );
    }

    /**
     * USE DATABASE.
     */
    private ExecuteResult executeUseDatabase(
            UseDatabaseCommand command
    ) {

        Database database =
                databaseManager.useDatabase(
                        command.getDatabaseName()
                );

        tableManager =
                new TableManager(
                        database.getDatabasePath()
                );

        return ExecuteResult.success(
                "Database selected successfully: "
                        + database.getName()
        );
    }

    /**
     * DROP DATABASE.
     */
    private ExecuteResult executeDropDatabase(
            DropDatabaseCommand command
    ) {

        Database currentDatabase =
                databaseManager
                        .getCurrentDatabase();

        boolean droppingCurrentDatabase =
                currentDatabase != null
                        && currentDatabase
                        .getName()
                        .equalsIgnoreCase(
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
     * CREATE TABLE.
     */
    private ExecuteResult executeCreateTable(
            CreateTableCommand command
    ) {

        TableManager activeTableManager =
                requireTableManager();

        TableMetadata metadata =
                activeTableManager.createTable(
                        command.getTableName(),
                        command.getColumns(),
                        command.getConstraints()
                );

        return ExecuteResult.success(
                "Table created successfully: "
                        + metadata.getTableName()
        );
    }

    /**
     * DROP TABLE.
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
     * INSERT.
     *
     * Fiziksel storage yaşam döngüsü
     * TableMutationExecutionSupport tarafından yönetilir.
     */
    private ExecuteResult executeInsert(
            InsertCommand command
    ) {

        return mutationExecutionSupport
                .executeInsert(
                        requireTableManager(),
                        command
                );
    }

    /**
     * UPDATE.
     */
    private ExecuteResult executeUpdate(
            UpdateCommand command
    ) {

        return mutationExecutionSupport
                .executeUpdate(
                        requireTableManager(),
                        command
                );
    }

    /**
     * DELETE.
     */
    private ExecuteResult executeDelete(
            DeleteCommand command
    ) {

        return mutationExecutionSupport
                .executeDelete(
                        requireTableManager(),
                        command
                );
    }

    /**
     * SELECT.
     *
     * SQL
     *   ↓
     * SelectCommand
     *   ↓
     * SelectCommandExecutionSupport
     *   ↓
     * QueryDataSource
     *   ↓
     * SelectExecutor
     *   ↓
     * QueryResult
     *   ↓
     * ExecuteResult
     *
     * Phase 7-C kapsamında QueryResult içerisindeki
     * sütun bilgileri de ExecuteResult'a aktarılacaktır.
     */
    private ExecuteResult executeSelect(
            SelectCommand command
    ) {

        return selectExecutionSupport
                .execute(
                        command,
                        requireQueryDataSource()
                );
    }

    /**
     * Management SQL komutunu ayrıştırır.
     */
    private Command parseSqlCommand(
            String sql
    ) {

        return managementCommandParser
                .parse(
                        sql
                );
    }

    /**
     * SQL sonundaki noktalı virgülleri kaldırır.
     */
    private String removeTrailingSemicolon(
            String sql
    ) {

        String result =
                sql.trim();

        while (result.endsWith(";")) {

            result =
                    result.substring(
                            0,
                            result.length() - 1
                    ).trim();
        }

        if (result.isBlank()) {

            throw new QueryExecutionException(
                    "SQL statement cannot be empty."
            );
        }

        return result;
    }

    /**
     * QueryExecutor oluşturulurken aktif bir
     * veritabanı bulunuyorsa TableManager
     * bağlantısını hazırlar.
     */
    private void initializeTableManager() {

        Database currentDatabase =
                databaseManager
                        .getCurrentDatabase();

        if (currentDatabase == null) {

            tableManager = null;

            return;
        }

        tableManager =
                new TableManager(
                        currentDatabase
                                .getDatabasePath()
                );
    }

    /**
     * Tablo işlemlerinden önce aktif
     * veritabanı bulunmasını zorunlu kılar.
     */
    private TableManager requireTableManager() {

        Database currentDatabase =
                databaseManager
                        .getCurrentDatabase();

        if (currentDatabase == null) {

            throw new QueryExecutionException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        if (tableManager == null) {

            tableManager =
                    new TableManager(
                            currentDatabase
                                    .getDatabasePath()
                    );
        }

        return tableManager;
    }

    /**
     * SELECT işlemlerinden önce QueryDataSource
     * bulunmasını zorunlu kılar.
     */
    private QueryDataSource requireQueryDataSource() {

        if (queryDataSource == null) {

            throw new QueryExecutionException(
                    "SELECT execution requires a QueryDataSource."
            );
        }

        return queryDataSource;
    }

    /**
     * QueryExecutor tarafından tutulan geçici
     * yönetici referanslarını temizler.
     */
    @Override
    public void close() {

        tableManager = null;
    }
}