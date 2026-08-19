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
     *
     * Sprint 00-12 kapsamında eklenmiştir.
     */
    private final InsertExecutor insertExecutor;

    /**
     * UPDATE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
     *
     * Sprint 00-12 kapsamında eklenmiştir.
     */
    private final UpdateExecutor updateExecutor;

    /**
     * DELETE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
     *
     * Sprint 00-12 kapsamında eklenmiştir.
     */
    private final DeleteExecutor deleteExecutor;

    /**
     * INSERT / UPDATE / DELETE fiziksel storage yaşam döngüsünü
     * QueryExecutor dışında yönetir.
     */
    private final TableMutationExecutionSupport mutationExecutionSupport;

    /**
     * SELECT için QueryDataSource hazırlama ve JOIN veri yükleme
     * sorumluluğunu kapsüller.
     */
    private final SelectCommandExecutionSupport selectExecutionSupport;

    /**
     * Geriye dönük management SQL komutlarını ayrıştırır.
     */
    private final ManagementCommandParser managementCommandParser;

    /**
     * Aktif veritabanına bağlı tablo yöneticisidir.
     */
    private TableManager tableManager;

    /**
     * Yalnızca veritabanı ve tablo yönetimi desteği bulunan
     * QueryExecutor oluşturur.
     *
     * @param databaseManager veritabanı yöneticisi
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
     *
     * @param databaseManager veritabanı yöneticisi
     * @param queryDataSource SELECT veri kaynağı
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
     *
     * @param databaseManager veritabanı yöneticisi
     * @param queryDataSource sorgu veri kaynağı
     * @param selectExecutor SELECT yürütücüsü
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
     *
     * @param databaseManager veritabanı yöneticisi
     * @param queryDataSource sorgu veri kaynağı
     * @param selectExecutor SELECT yürütücüsü
     * @param insertExecutor INSERT yürütücüsü
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
     *
     * @param databaseManager veritabanı yöneticisi
     * @param queryDataSource sorgu veri kaynağı
     * @param selectExecutor SELECT yürütücüsü
     * @param insertExecutor INSERT yürütücüsü
     * @param updateExecutor UPDATE yürütücüsü
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
     *
     * @param databaseManager veritabanı yöneticisi
     * @param queryDataSource sorgu veri kaynağı
     * @param selectExecutor SELECT yürütücüsü
     * @param insertExecutor INSERT yürütücüsü
     * @param updateExecutor UPDATE yürütücüsü
     * @param deleteExecutor DELETE yürütücüsü
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            SelectExecutor selectExecutor,
            InsertExecutor insertExecutor,
            UpdateExecutor updateExecutor,
            DeleteExecutor deleteExecutor
    ) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager,
                "DatabaseManager cannot be null."
        );

        this.queryDataSource = queryDataSource;

        this.selectExecutor = Objects.requireNonNull(
                selectExecutor,
                "SelectExecutor cannot be null."
        );

        this.insertExecutor = Objects.requireNonNull(
                insertExecutor,
                "InsertExecutor cannot be null."
        );

        this.updateExecutor = Objects.requireNonNull(
                updateExecutor,
                "UpdateExecutor cannot be null."
        );

        this.deleteExecutor = Objects.requireNonNull(
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
     * Bu metot eski testlerle geriye uyumluluk sağlar.
     *
     * Desteklenen SQL metinleri:
     *
     * CREATE DATABASE database_name
     * USE DATABASE database_name
     * USE database_name
     * DROP DATABASE database_name
     * CREATE TABLE table_name (...)
     * DROP TABLE table_name
     * INSERT INTO table_name (...) VALUES (...)
     * SELECT ...
     * UPDATE table_name SET ... [WHERE ...]
     * DELETE FROM table_name [WHERE ...]
     *
     * @param sql çalıştırılacak SQL metni
     * @return yürütme sonucu
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
         * SQL komutunun ilk keyword'ünü whitespace bağımsız al.
         *
         * Böylece:
         *
         * SELECT * FROM users
         *
         * ve
         *
         * SELECT
         *     department,
         *     COUNT(*)
         * FROM employees
         *
         * aynı şekilde SELECT olarak algılanır.
         */
        String firstKeyword =
                normalizedSql
                        .split("\\s+", 2)[0]
                        .toUpperCase(
                                Locale.ROOT
                        );

        /*
         * Data query / mutation işlemleri yeni parser
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

            } catch (
                    RuntimeException exception
            ) {

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

            } catch (
                    RuntimeException exception
            ) {

                if (exception
                        instanceof QueryExecutionException queryExecutionException) {

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
         * CREATE DATABASE
         * USE DATABASE
         * DROP DATABASE
         * CREATE TABLE
         * DROP TABLE
         *
         * mevcut management parser yolunda kalır.
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
     * Verilen Command nesnesini çalıştırır.
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

            if (command instanceof InsertCommand value) {
                return executeInsert(value);
            }

            if (command instanceof UpdateCommand value) {
                return executeUpdate(value);
            }

            if (command instanceof SelectCommand value) {
                return executeSelect(value);
            }

            if (command instanceof DeleteCommand value) {
                return executeDelete(value);
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
     * INSERT komutunu çalıştırır.
     *
     * .tbl dosyası tablo şemasını tuttuğu için fiziksel kayıtlar
     * tablo adına ait ayrı bir .data dosyasında saklanır.
     */
    private ExecuteResult executeInsert(
            InsertCommand command
    ) {
        return mutationExecutionSupport.executeInsert(
                requireTableManager(),
                command
        );
    }

    /**
     * UPDATE komutunu fiziksel storage katmanında çalıştırır.
     *
     * .tbl dosyası tablo şemasını tuttuğu için fiziksel kayıtlar
     * tablo adına ait ayrı .data dosyasından okunur ve güncellenir.
     */
    private ExecuteResult executeUpdate(
            UpdateCommand command
    ) {
        return mutationExecutionSupport.executeUpdate(
                requireTableManager(),
                command
        );
    }

    /**
     * DELETE komutunu fiziksel storage katmanında çalıştırır.
     *
     * .tbl dosyası tablo şemasını tuttuğu için fiziksel kayıtlar
     * tablo adına ait ayrı .data dosyasından okunur.
     *
     * Silme işlemi RecordManager.delete(recordId) üzerinden
     * logical delete olarak gerçekleştirilir.
     */
    private ExecuteResult executeDelete(
            DeleteCommand command
    ) {
        return mutationExecutionSupport.executeDelete(
                requireTableManager(),
                command
        );
    }

    /**
     * SELECT komutunu çalıştırır.
     *
     * Sprint 00-16 SELECT + JOIN pipeline:
     *
     * SQL
     *   ->
     * SqlParser
     *   ->
     * SelectStatement
     *   ->
     * StatementCommandMapper
     *   ->
     * SelectCommand
     *   ->
     * SelectExecutor.executeStatement(...)
     *
     * Execution (JOIN yoksa):
     *
     * WHERE
     *   ->
     * GROUP BY
     *   ->
     * Aggregate
     *   ->
     * HAVING
     *   ->
     * ORDER BY
     *   ->
     * LIMIT / FETCH
     *   ->
     * QueryResult
     */
    private ExecuteResult executeSelect(
            SelectCommand command
    ) {
        return selectExecutionSupport.execute(
                command,
                requireQueryDataSource()
        );
    }

    /**
     * SQL metnini uygun Command nesnesine dönüştürür.
     */
    private Command parseSqlCommand(String sql) {
        return managementCommandParser.parse(sql);
    }

    /**
     * CREATE TABLE SQL metnini CreateTableCommand nesnesine dönüştürür.
     *
     * Örnek:
     *
     * CREATE TABLE users (
     *     id INT,
     *     name STRING,
     *     age INT
     * )
     */


    /**
     * CREATE TABLE içindeki sütun tanımlarını ayrıştırır.
     */


    /**
     * SQL veri tipini YEKDB DataType değerine dönüştürür.
     */


    /**
     * Belirtilen SQL anahtar kelimesinden sonraki değeri döndürür.
     */


    /**
     * SQL sonundaki noktalı virgülleri kaldırır.
     */
    private String removeTrailingSemicolon(
            String sql
    ) {
        String result = sql.trim();

        while (result.endsWith(";")) {
            result = result.substring(
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
     * Henüz yürütme katmanına bağlanmamış kayıt işlemleri
     * için açıklayıcı hata üretir.
     */


    /**
     * QueryExecutor oluşturulurken aktif bir veritabanı
     * bulunuyorsa TableManager bağlantısını hazırlar.
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