package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseManager;
import com.yekdb.index.Index;
import com.yekdb.index.IndexManager;
import com.yekdb.index.IndexType;
import com.yekdb.index.RecordPointer;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.AlterTableCommand;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateIndexCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.CreateTriggerCommand;
import com.yekdb.query.command.CreateViewCommand;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropIndexCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.DropTriggerCommand;
import com.yekdb.query.command.DropViewCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.ShowTriggersCommand;
import com.yekdb.query.command.ShowViewsCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.query.datasource.QueryDataSource;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.query.evaluator.WhereEvaluator;
import com.yekdb.query.mapper.StatementCommandMapper;
import com.yekdb.query.parser.SqlParser;
import com.yekdb.query.statement.Statement;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import com.yekdb.storage.table.TableMetadata;
import com.yekdb.trigger.TriggerDefinition;
import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerMetadata;
import com.yekdb.trigger.TriggerTiming;
import com.yekdb.view.ViewDefinition;
import com.yekdb.view.ViewMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * - CreateTriggerCommand
 * - CreateViewCommand
 * - DropTableCommand
 * - DropTriggerCommand
 * - DropViewCommand
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
     * ALTER TABLE komutlarını fiziksel şema katmanına yönlendirir.
     */
    private final AlterTableExecutor alterTableExecutor;

    /**
     * CREATE VIEW komutunu aktif database view catalog'una kaydeder.
     */
    private final CreateViewExecutor createViewExecutor;

    /**
     * INSERT pipeline içerisinde trigger body'lerini çalıştırır.
     */
    private final TriggerExecutionSupport triggerExecutionSupport;

    /**
     * Aktif terminal/query session içerisindeki B+ Tree index kataloğudur.
     *
     * Phase 16 kapsamında index metadata yaşam döngüsü session-scope'tur.
     */
    private IndexManager indexManager;

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

        this.alterTableExecutor =
                new AlterTableExecutor();

        this.createViewExecutor =
                new CreateViewExecutor();

        this.triggerExecutionSupport =
                new TriggerExecutionSupport();

        this.indexManager =
                new IndexManager();

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
     * CREATE TRIGGER
     * CREATE VIEW
     * DROP TABLE
     * DROP TRIGGER
     * DROP VIEW
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
                    instanceof CreateIndexCommand value) {

                return executeCreateIndex(
                        value
                );
            }

            if (command
                    instanceof CreateViewCommand value) {

                return executeCreateView(
                        value
                );
            }

            if (command
                    instanceof CreateTriggerCommand value) {

                return executeCreateTrigger(
                        value
                );
            }

            if (command
                    instanceof ShowTriggersCommand value) {

                return executeShowTriggers(
                        value
                );
            }

            if (command instanceof ShowViewsCommand) {
                return executeShowViews();
            }

            if (command
                    instanceof DropIndexCommand value) {

                return executeDropIndex(
                        value
                );
            }

            if (command
                    instanceof DropViewCommand value) {

                return executeDropView(
                        value
                );
            }

            if (command
                    instanceof DropTriggerCommand value) {

                return executeDropTrigger(
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
                    instanceof AlterTableCommand value) {

                return executeAlterTable(
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

        if (command instanceof CreateIndexCommand) {
            return "CREATE INDEX";
        }

        if (command instanceof CreateViewCommand) {
            return "CREATE VIEW";
        }

        if (command instanceof CreateTriggerCommand) {
            return "CREATE TRIGGER";
        }

        if (command instanceof ShowViewsCommand) {
            return "SHOW VIEWS";
        }

        if (command instanceof ShowTriggersCommand) {
            return "SHOW TRIGGERS";
        }

        if (command instanceof DropIndexCommand) {
            return "DROP INDEX";
        }

        if (command instanceof DropViewCommand) {
            return "DROP VIEW";
        }

        if (command instanceof DropTriggerCommand) {
            return "DROP TRIGGER";
        }

        if (command instanceof DropTableCommand) {
            return "DROP TABLE";
        }

        if (command instanceof AlterTableCommand) {
            return "ALTER TABLE";
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

        indexManager =
                new IndexManager();

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
            indexManager = new IndexManager();
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

        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase != null) {
            indexManager.dropIndexesForTable(
                    currentDatabase.getName(),
                    command.getTableName()
            );
        }

        return ExecuteResult.success(
                "Table dropped successfully: "
                        + command.getTableName()
        );
    }

    /**
     * CREATE INDEX / CREATE UNIQUE INDEX.
     *
     * Index oluşturulduğunda tabloda mevcut olan aktif kayıtlar da
     * B+ Tree içerisine backfill edilir.
     */
    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private ExecuteResult executeCreateIndex(
            CreateIndexCommand command
    ) {
        TableManager activeTableManager =
                requireTableManager();

        Database database =
                databaseManager.getCurrentDatabase();

        Table table =
                activeTableManager.getTable(
                        command.getTableName()
                );

        table.getColumn(
                command.getColumnName()
        );

        Index index =
                indexManager.createIndex(
                        command.getIndexName(),
                        database.getName(),
                        table.getTableName(),
                        command.getColumnName(),
                        command.isUnique()
                                ? IndexType.UNIQUE
                                : IndexType.NON_UNIQUE
                );

        try {
            if (queryDataSource
                    instanceof StorageQueryDataSource storageDataSource) {

                int columnIndex =
                        findColumnIndex(
                                table,
                                command.getColumnName()
                        );

                for (Map.Entry<RecordPointer, Row> entry :
                        storageDataSource
                                .getRowsByPointer(
                                        table.getTableName()
                                )
                                .entrySet()) {

                    Object key =
                            entry.getValue()
                                    .getValue(
                                            columnIndex
                                    );

                    if (key == null) {
                        continue;
                    }

                    if (!(key instanceof Comparable comparable)) {
                        throw new QueryExecutionException(
                                "Indexed column value must implement Comparable."
                        );
                    }

                    index.insert(
                            comparable,
                            entry.getKey()
                    );
                }
            }

        } catch (RuntimeException exception) {
            if (indexManager.indexExists(
                    command.getIndexName()
            )) {
                indexManager.dropIndex(
                        command.getIndexName()
                );
            }
            throw exception;
        }

        return ExecuteResult.success(
                (command.isUnique()
                        ? "Unique index created successfully: "
                        : "Index created successfully: ")
                        + command.getIndexName()
        );
    }

    /**
     * CREATE VIEW.
     *
     * Phase 3 kapsamında view fiziksel veri saklamaz;
     * yalnızca aktif database view catalog'una kaydedilir.
     */
    private ExecuteResult executeCreateView(
            CreateViewCommand command
    ) {
        Database currentDatabase =
                requireCurrentDatabase();

        TableManager activeTableManager =
                requireTableManager();

        if (tableExists(
                activeTableManager,
                command.getViewName()
        )) {
            throw new QueryExecutionException(
                    "View name conflicts with existing table: "
                            + command.getViewName()
            );
        }

        return createViewExecutor.execute(
                currentDatabase,
                command
        );
    }

    /**
     * CREATE TRIGGER.
     */
    private ExecuteResult executeCreateTrigger(
            CreateTriggerCommand command
    ) {
        Database currentDatabase =
                requireCurrentDatabase();

        TableManager activeTableManager =
                requireTableManager();

        activeTableManager.getTable(
                command.getTableName()
        );

        TriggerDefinition definition =
                new TriggerDefinition(
                        command.getTriggerName(),
                        command.getTableName(),
                        command.getTiming(),
                        command.getEvent(),
                        command.getBody()
                );

        TriggerMetadata metadata =
                new TriggerMetadata(
                        definition.getTriggerName(),
                        definition.getTableName()
                );

        currentDatabase.getTriggerCatalog()
                .registerTrigger(
                        definition,
                        metadata
                );

        return ExecuteResult.success(
                "Trigger created successfully: "
                        + definition.getTriggerName()
        );
    }

    /**
     * SHOW TRIGGERS.
     */
    private ExecuteResult executeShowTriggers(
            ShowTriggersCommand command
    ) {
        Database currentDatabase =
                requireCurrentDatabase();

        List<Column> columns =
                List.of(
                        new Column("trigger_name", DataType.STRING),
                        new Column("table_name", DataType.STRING),
                        new Column("timing", DataType.STRING),
                        new Column("event", DataType.STRING),
                        new Column("version", DataType.INT),
                        new Column("created_at", DataType.STRING)
                );

        List<Row> rows =
                new ArrayList<>();

        for (TriggerDefinition definition :
                currentDatabase
                        .getTriggerCatalog()
                        .listTriggers()) {

            if (command.hasTableName()
                    && !definition.getTableName()
                    .equalsIgnoreCase(
                            command.getTableName()
                    )) {
                continue;
            }

            TriggerMetadata metadata =
                    currentDatabase
                            .getTriggerCatalog()
                            .getMetadata(
                                    definition.getTriggerName()
                            );

            rows.add(
                    new Row(
                            List.of(
                                    definition.getTriggerName(),
                                    definition.getTableName(),
                                    definition.getTiming().name(),
                                    definition.getEvent().name(),
                                    metadata.getVersion(),
                                    metadata.getCreatedAt().toString()
                            )
                    )
            );
        }

        return ExecuteResult.selectSuccess(
                "Triggers listed successfully.",
                columns,
                rows
        );
    }

    /**
     * SHOW VIEWS.
     */
    private ExecuteResult executeShowViews() {
        Database currentDatabase =
                requireCurrentDatabase();

        List<Column> columns =
                List.of(
                        new Column("view_name", DataType.STRING),
                        new Column("source_select", DataType.STRING),
                        new Column("version", DataType.INT),
                        new Column("created_at", DataType.STRING)
                );

        List<Row> rows =
                new ArrayList<>();

        for (ViewDefinition definition :
                currentDatabase
                        .getViewCatalog()
                        .listViews()) {

            ViewMetadata metadata =
                    currentDatabase
                            .getViewCatalog()
                            .getMetadata(
                                    definition.getViewName()
                            );

            rows.add(
                    new Row(
                            List.of(
                                    definition.getViewName(),
                                    definition.getSourceSelect(),
                                    metadata.getVersion(),
                                    metadata.getCreatedAt().toString()
                            )
                    )
            );
        }

        return ExecuteResult.selectSuccess(
                "Views listed successfully.",
                columns,
                rows
        );
    }

    private boolean tableExists(
            TableManager tableManager,
            String tableName
    ) {
        try {
            tableManager.getTable(tableName);
            return true;

        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * DROP INDEX.
     */
    private ExecuteResult executeDropIndex(
            DropIndexCommand command
    ) {
        indexManager.dropIndex(
                command.getIndexName()
        );

        return ExecuteResult.success(
                "Index dropped successfully: "
                        + command.getIndexName()
        );
    }

    /**
     * DROP VIEW.
     */
    private ExecuteResult executeDropView(
            DropViewCommand command
    ) {
        Database currentDatabase =
                requireCurrentDatabase();

        currentDatabase.getViewCatalog()
                .unregisterView(
                        command.getViewName()
                );

        return ExecuteResult.success(
                "View dropped successfully: "
                        + command.getViewName()
        );
    }

    /**
     * DROP TRIGGER.
     */
    private ExecuteResult executeDropTrigger(
            DropTriggerCommand command
    ) {
        Database currentDatabase =
                requireCurrentDatabase();

        currentDatabase.getTriggerCatalog()
                .unregisterTrigger(
                        command.getTriggerName()
                );

        return ExecuteResult.success(
                "Trigger dropped successfully: "
                        + command.getTriggerName()
        );
    }

    /**
     * ALTER TABLE.
     */
    private ExecuteResult executeAlterTable(
            AlterTableCommand command
    ) {

        return alterTableExecutor.execute(
                requireTableManager(),
                command
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
        Database currentDatabase =
                requireCurrentDatabase();

        TableManager activeTableManager =
                requireTableManager();

        Table table =
                activeTableManager.getTable(
                        command.getTableName()
                );

        triggerExecutionSupport.executeInsertTriggers(
                currentDatabase,
                table,
                command,
                TriggerTiming.BEFORE,
                this::execute
        );

        ExecuteResult result =
                mutationExecutionSupport
                        .executeInsert(
                                activeTableManager,
                                command,
                                indexesForTable(
                                        command.getTableName()
                                )
                        );

        triggerExecutionSupport.executeInsertTriggers(
                currentDatabase,
                table,
                command,
                TriggerTiming.AFTER,
                this::execute
        );

        return result;
    }

    /**
     * UPDATE.
     */
    private ExecuteResult executeUpdate(
            UpdateCommand command
    ) {

        Database currentDatabase =
                requireCurrentDatabase();

        TableManager activeTableManager =
                requireTableManager();

        Table table =
                activeTableManager.getTable(
                        command.getTableName()
                );

        List<TriggerExecutionSupport.RowChange> rowChanges =
                collectUpdateRowChanges(
                        table,
                        command
                );

        triggerExecutionSupport.executeUpdateTriggers(
                currentDatabase,
                table,
                rowChanges,
                TriggerTiming.BEFORE,
                this::execute
        );

        ExecuteResult result =
                mutationExecutionSupport
                .executeUpdate(
                        activeTableManager,
                        command,
                        indexesForTable(
                                command.getTableName()
                        )
                );

        triggerExecutionSupport.executeUpdateTriggers(
                currentDatabase,
                table,
                rowChanges,
                TriggerTiming.AFTER,
                this::execute
        );

        return result;
    }

    /**
     * DELETE.
     */
    private ExecuteResult executeDelete(
            DeleteCommand command
    ) {

        Database currentDatabase =
                requireCurrentDatabase();

        TableManager activeTableManager =
                requireTableManager();

        Table table =
                activeTableManager.getTable(
                        command.getTableName()
                );

        List<Row> oldRows =
                collectDeleteRows(
                        table,
                        command
                );

        triggerExecutionSupport.executeDeleteTriggers(
                currentDatabase,
                table,
                oldRows,
                TriggerTiming.BEFORE,
                this::execute
        );

        ExecuteResult result =
                mutationExecutionSupport
                .executeDelete(
                        activeTableManager,
                        command,
                        indexesForTable(
                                command.getTableName()
                        )
                );

        triggerExecutionSupport.executeDeleteTriggers(
                currentDatabase,
                table,
                oldRows,
                TriggerTiming.AFTER,
                this::execute
        );

        return result;
    }

    private List<TriggerExecutionSupport.RowChange> collectUpdateRowChanges(
            Table table,
            UpdateCommand command
    ) {
        List<Row> matchedRows =
                collectMatchingRows(
                        table,
                        command.hasWhereExpression()
                                ? command.getWhereExpression()
                                : null
                );

        List<TriggerExecutionSupport.RowChange> rowChanges =
                new ArrayList<>(matchedRows.size());

        for (Row oldRow : matchedRows) {
            rowChanges.add(
                    new TriggerExecutionSupport.RowChange(
                            oldRow,
                            createUpdatedRow(
                                    table,
                                    oldRow,
                                    command.getUpdatedValues()
                            )
                    )
            );
        }

        return List.copyOf(rowChanges);
    }

    private List<Row> collectDeleteRows(
            Table table,
            DeleteCommand command
    ) {
        return collectMatchingRows(
                table,
                command.hasWhereExpression()
                        ? command.getWhereExpression()
                        : null
        );
    }

    private List<Row> collectMatchingRows(
            Table table,
            com.yekdb.query.expression.Expression whereExpression
    ) {
        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        List<Row> rows =
                dataSource.getRows(
                        table.getTableName()
                );

        if (whereExpression == null) {
            return rows;
        }

        List<Row> matchedRows =
                new ArrayList<>();

        for (Row row : rows) {
            if (WhereEvaluator.evaluate(
                    whereExpression,
                    row,
                    table
            )) {
                matchedRows.add(row);
            }
        }

        return List.copyOf(matchedRows);
    }

    private Row createUpdatedRow(
            Table table,
            Row currentRow,
            Map<String, Object> updatedValues
    ) {
        Row updatedRow =
                new Row(
                        currentRow.getValues()
                );

        for (Map.Entry<String, Object> entry :
                updatedValues.entrySet()) {

            updatedRow.setValue(
                    findColumnIndex(
                            table,
                            entry.getKey()
                    ),
                    entry.getValue()
            );
        }

        return updatedRow;
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
        return executeSelect(
                command,
                new HashSet<>()
        );
    }

    /**
     * SELECT.
     *
     * View varsa kaynak SELECT önce çalıştırılır, sonucu geçici
     * data source olarak dış SELECT'e verilir.
     */
    private ExecuteResult executeSelect(
            SelectCommand command,
            Set<String> resolvingViews
    ) {

        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase != null
                && currentDatabase
                .getViewCatalog()
                .containsView(
                        command.getTableName()
                )) {

            return executeSelectFromView(
                    command,
                    currentDatabase,
                    resolvingViews
            );
        }

        QueryDataSource dataSource =
                requireQueryDataSource();

        List<Index<?>> indexes =
                indexesForTable(
                        command.getTableName()
                );

        if (indexes.isEmpty()
                || !(dataSource
                instanceof StorageQueryDataSource storageDataSource)) {

            return selectExecutionSupport.execute(
                    command,
                    dataSource
            );
        }

        Map<RecordPointer, Row> rowsByPointer =
                storageDataSource.getRowsByPointer(
                        command.getTableName()
                );

        return selectExecutionSupport.execute(
                command,
                dataSource,
                indexes,
                rowsByPointer::get
        );
    }

    private ExecuteResult executeSelectFromView(
            SelectCommand outerCommand,
            Database currentDatabase,
            Set<String> resolvingViews
    ) {
        String viewName =
                outerCommand.getTableName()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (!resolvingViews.add(viewName)) {
            throw new QueryExecutionException(
                    "Recursive view reference detected: "
                            + viewName
            );
        }

        try {
            ViewDefinition definition =
                    currentDatabase.getViewCatalog()
                            .getView(viewName);

            SelectCommand sourceCommand =
                    parseViewSourceSelect(
                            definition.getSourceSelect()
                    );

            ExecuteResult sourceResult =
                    executeSelect(
                            sourceCommand,
                            resolvingViews
                    );

            InMemoryQueryDataSource viewDataSource =
                    new InMemoryQueryDataSource();

            viewDataSource.register(
                    new Table(
                            viewName,
                            sourceResult.getColumns()
                    ),
                    sourceResult.getRows()
            );

            return selectExecutionSupport.execute(
                    outerCommand,
                    viewDataSource
            );

        } finally {
            resolvingViews.remove(viewName);
        }
    }

    private SelectCommand parseViewSourceSelect(
            String sourceSelect
    ) {
        Statement statement =
                new SqlParser()
                        .parse(
                                sourceSelect
                        );

        Command command =
                StatementCommandMapper.map(
                        statement
                );

        if (command instanceof SelectCommand selectCommand) {
            return selectCommand;
        }

        throw new QueryExecutionException(
                "View source must be a SELECT statement."
        );
    }

    /**
     * Aktif veritabanındaki tabloya ait index'leri döndürür.
     */
    private List<Index<?>> indexesForTable(
            String tableName
    ) {
        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase == null
                || indexManager == null) {
            return List.of();
        }

        return indexManager.getIndexesForTable(
                currentDatabase.getName(),
                tableName
        );
    }

    /**
     * Table schema içerisinde kolon index'ini case-insensitive bulur.
     */
    private int findColumnIndex(
            Table table,
            String columnName
    ) {
        for (int i = 0;
             i < table.getColumns().size();
             i++) {

            if (table.getColumns()
                    .get(i)
                    .getName()
                    .equalsIgnoreCase(
                            columnName
                    )) {
                return i;
            }
        }

        throw new QueryExecutionException(
                "Column not found for index: "
                        + columnName
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
                requireCurrentDatabase();

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
     * Yönetim komutlarından önce aktif database bulunmasını zorunlu kılar.
     */
    private Database requireCurrentDatabase() {

        Database currentDatabase =
                databaseManager
                        .getCurrentDatabase();

        if (currentDatabase == null) {

            throw new QueryExecutionException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        return currentDatabase;
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
        indexManager = new IndexManager();
    }
}
