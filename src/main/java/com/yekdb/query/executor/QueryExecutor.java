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
import com.yekdb.query.datasource.QueryDataSource;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import com.yekdb.table.TableManager;
import com.yekdb.table.TableMetadata;

import java.util.ArrayList;
import java.util.List;
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
                new SelectExecutor()
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
                new SelectExecutor()
        );
    }

    /**
     * Bütün bağımlılıkların dışarıdan verilebildiği constructor.
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
        this.databaseManager = Objects.requireNonNull(
                databaseManager,
                "DatabaseManager cannot be null."
        );

        this.queryDataSource = queryDataSource;

        this.selectExecutor = Objects.requireNonNull(
                selectExecutor,
                "SelectExecutor cannot be null."
        );

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
     *
     * @param sql çalıştırılacak SQL metni
     * @return yürütme sonucu
     */
    public ExecuteResult execute(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new QueryExecutionException(
                    "SQL statement cannot be null or blank."
            );
        }

        String normalizedSql = removeTrailingSemicolon(
                sql.trim()
        );

        Command command = parseSqlCommand(
                normalizedSql
        );

        return execute(command);
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

            if (command instanceof InsertCommand) {
                return unsupportedRecordOperation(
                        "INSERT"
                );
            }

            if (command instanceof SelectCommand value) {
                return executeSelect(value);
            }

            if (command instanceof DeleteCommand) {
                return unsupportedRecordOperation(
                        "DELETE"
                );
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
     * SELECT komutunu çalıştırır.
     */
    private ExecuteResult executeSelect(
            SelectCommand command
    ) {
        QueryDataSource activeDataSource =
                requireQueryDataSource();

        Table table = activeDataSource.getTable(
                command.getTableName()
        );

        List<Row> rows = activeDataSource.getRows(
                command.getTableName()
        );

        QueryResult queryResult =
                selectExecutor.execute(
                        table,
                        rows,
                        command.getWhereExpression()
                );

        String message =
                "SELECT query executed successfully. "
                        + "Returned row count: "
                        + queryResult.getRows().size()
                        + ", execution time: "
                        + queryResult.getExecutionTimeMillis()
                        + " ms";

        return ExecuteResult.success(
                message,
                queryResult.getRows()
        );
    }

    /**
     * SQL metnini uygun Command nesnesine dönüştürür.
     */
    private Command parseSqlCommand(String sql) {
        String upperSql = sql.toUpperCase(
                Locale.ROOT
        );

        if (upperSql.startsWith("CREATE DATABASE ")) {
            String databaseName = extractValueAfterKeyword(
                    sql,
                    "CREATE DATABASE"
            );

            return new CreateDatabaseCommand(
                    databaseName
            );
        }

        if (upperSql.startsWith("USE DATABASE ")) {
            String databaseName = extractValueAfterKeyword(
                    sql,
                    "USE DATABASE"
            );

            return new UseDatabaseCommand(
                    databaseName
            );
        }

        if (upperSql.startsWith("USE ")) {
            String databaseName = extractValueAfterKeyword(
                    sql,
                    "USE"
            );

            return new UseDatabaseCommand(
                    databaseName
            );
        }

        if (upperSql.startsWith("DROP DATABASE ")) {
            String databaseName = extractValueAfterKeyword(
                    sql,
                    "DROP DATABASE"
            );

            return new DropDatabaseCommand(
                    databaseName
            );
        }

        if (upperSql.startsWith("CREATE TABLE ")) {
            return parseCreateTableCommand(
                    sql
            );
        }

        if (upperSql.startsWith("DROP TABLE ")) {
            String tableName = extractValueAfterKeyword(
                    sql,
                    "DROP TABLE"
            );

            return new DropTableCommand(
                    tableName
            );
        }

        throw new QueryExecutionException(
                "Unsupported SQL statement: " + sql
        );
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
    private CreateTableCommand parseCreateTableCommand(
            String sql
    ) {
        int openParenthesisIndex =
                sql.indexOf('(');

        int closeParenthesisIndex =
                sql.lastIndexOf(')');

        if (openParenthesisIndex < 0
                || closeParenthesisIndex < 0
                || closeParenthesisIndex
                <= openParenthesisIndex) {

            throw new QueryExecutionException(
                    "Invalid CREATE TABLE statement: "
                            + sql
            );
        }

        String tableName = sql.substring(
                "CREATE TABLE".length(),
                openParenthesisIndex
        ).trim();

        if (tableName.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain a table name."
            );
        }

        String columnDefinitionSection =
                sql.substring(
                        openParenthesisIndex + 1,
                        closeParenthesisIndex
                ).trim();

        if (columnDefinitionSection.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain columns."
            );
        }

        String remainingText = sql.substring(
                closeParenthesisIndex + 1
        ).trim();

        if (!remainingText.isEmpty()) {
            throw new QueryExecutionException(
                    "Unexpected text after CREATE TABLE definition: "
                            + remainingText
            );
        }

        List<Column> columns =
                parseColumnDefinitions(
                        columnDefinitionSection
                );

        return new CreateTableCommand(
                tableName,
                columns
        );
    }

    /**
     * CREATE TABLE içindeki sütun tanımlarını ayrıştırır.
     */
    private List<Column> parseColumnDefinitions(
            String columnDefinitionSection
    ) {
        List<Column> columns =
                new ArrayList<>();

        String[] definitions =
                columnDefinitionSection.split(",");

        for (String definition : definitions) {
            String normalizedDefinition =
                    definition.trim();

            if (normalizedDefinition.isBlank()) {
                throw new QueryExecutionException(
                        "Column definition cannot be blank."
                );
            }

            String[] parts =
                    normalizedDefinition.split("\\s+");

            if (parts.length != 2) {
                throw new QueryExecutionException(
                        "Invalid column definition: "
                                + normalizedDefinition
                );
            }

            String columnName = parts[0];

            DataType dataType =
                    parseDataType(
                            parts[1]
                    );

            columns.add(
                    new Column(
                            columnName,
                            dataType
                    )
            );
        }

        if (columns.isEmpty()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain valid columns."
            );
        }

        return List.copyOf(columns);
    }

    /**
     * SQL veri tipini YEKDB DataType değerine dönüştürür.
     */
    private DataType parseDataType(String value) {
        String normalizedType = value
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (normalizedType) {
            case "INT", "INTEGER" ->
                    DataType.INT;

            case "LONG", "BIGINT" ->
                    DataType.LONG;

            case "DOUBLE", "FLOAT", "REAL" ->
                    DataType.DOUBLE;

            case "BOOLEAN", "BOOL" ->
                    DataType.BOOLEAN;

            case "STRING", "TEXT", "VARCHAR" ->
                    DataType.STRING;

            default -> throw new QueryExecutionException(
                    "Unsupported data type: " + value
            );
        };
    }

    /**
     * Belirtilen SQL anahtar kelimesinden sonraki değeri döndürür.
     */
    private String extractValueAfterKeyword(
            String sql,
            String keyword
    ) {
        String value = sql.substring(
                keyword.length()
        ).trim();

        if (value.isBlank()) {
            throw new QueryExecutionException(
                    keyword + " statement requires a name."
            );
        }

        if (value.contains(" ")) {
            throw new QueryExecutionException(
                    "Invalid value after "
                            + keyword
                            + ": "
                            + value
            );
        }

        return value;
    }

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
    private ExecuteResult unsupportedRecordOperation(
            String operationName
    ) {
        throw new QueryExecutionException(
                operationName
                        + " execution is not implemented yet."
        );
    }

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