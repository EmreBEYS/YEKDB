package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseManager;
import com.yekdb.index.Index;
import com.yekdb.index.IndexManager;
import com.yekdb.index.IndexType;
import com.yekdb.index.RecordPointer;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.AlterTableCommand;
import com.yekdb.query.command.BeginTransactionCommand;
import com.yekdb.query.command.CommitTransactionCommand;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateIndexCommand;
import com.yekdb.query.command.CreateProcedureCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.CreateTriggerCommand;
import com.yekdb.query.command.CreateViewCommand;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropIndexCommand;
import com.yekdb.query.command.DropProcedureCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.DropTriggerCommand;
import com.yekdb.query.command.DropViewCommand;
import com.yekdb.query.command.ExplainCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.CallProcedureCommand;
import com.yekdb.query.command.ReleaseSavepointCommand;
import com.yekdb.query.command.RollbackTransactionCommand;
import com.yekdb.query.command.RollbackToSavepointCommand;
import com.yekdb.query.command.SavepointCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.ShowSavepointsCommand;
import com.yekdb.query.command.ShowProcedureCommand;
import com.yekdb.query.command.ShowProceduresCommand;
import com.yekdb.query.command.ShowTransactionCommand;
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
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.TableReference;
import com.yekdb.procedure.ProcedureDefinition;
import com.yekdb.procedure.ProcedureMetadata;
import com.yekdb.procedure.ProcedureCatalog;
import com.yekdb.procedure.exception.ProcedureNotFoundException;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import com.yekdb.storage.table.TableMetadata;
import com.yekdb.transaction.TransactionContext;
import com.yekdb.transaction.TransactionDeadlockException;
import com.yekdb.transaction.TransactionDurabilityLog;
import com.yekdb.transaction.TransactionIsolationLevel;
import com.yekdb.transaction.TransactionManager;
import com.yekdb.transaction.TransactionRecoveryManager;
import com.yekdb.transaction.TransactionSavepointInfo;
import com.yekdb.transaction.TransactionTableReadLock;
import com.yekdb.transaction.TransactionTableReadLockManager;
import com.yekdb.transaction.TransactionTableWriteLock;
import com.yekdb.transaction.TransactionTableWriteLockManager;
import com.yekdb.trigger.TriggerDefinition;
import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerMetadata;
import com.yekdb.trigger.TriggerTiming;
import com.yekdb.view.ViewDefinition;
import com.yekdb.view.ViewMetadata;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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
     * ALTER TABLE komutlarını fiziksel şema katmanına yönlendirir.
     */
    private final AlterTableExecutor alterTableExecutor;

    /**
     * CREATE VIEW catalog kayıtlarını oluşturur.
     */
    private final CreateViewExecutor createViewExecutor;

    /**
     * Trigger body çalıştırma ve OLD / NEW bağlamını yönetir.
     */
    private final TriggerExecutionSupport triggerExecutionSupport;

    /**
     * Stored procedure body çalıştırma ve parametre bağlamını yönetir.
     */
    private final ProcedureExecutionSupport procedureExecutionSupport;

    /**
     * EXPLAIN SELECT plan satırlarını optimizer üzerinden üretir.
     */
    private final ExplainCommandExecutionSupport explainExecutionSupport;

    /**
     * Sprint 00-32 Phase 1 transaction yasam dongusunu yonetir.
     */
    private final TransactionManager transactionManager;

    /**
     * Sprint 00-33 Phase 6 transaction kararlarini kalici log'a yazar.
     */
    private final TransactionDurabilityLog transactionDurabilityLog;

    /**
     * Sprint 00-33 Phase 10 database seciminde tamamlanmamis
     * transaction kayitlarini kapatir.
     */
    private final TransactionRecoveryManager transactionRecoveryManager;

    /**
     * Sprint 00-33 Phase 5 tablo seviyesinde write izolasyonu saglar.
     */
    private final TransactionTableWriteLockManager writeLockManager;

    private final Map<String, TransactionTableWriteLock> heldWriteLocks;

    /**
     * Sprint 00-35 Phase 4 SELECT shared lock'larini yonetir.
     */
    private final TransactionTableReadLockManager readLockManager;

    private final Map<String, TransactionTableReadLock> heldReadLocks;

    private final String transactionLockOwnerId;

    private final Duration transactionLockTimeout;

    /**
     * View çözümleme sırasında recursive referansları yakalar.
     */
    private final Deque<String> activeViewStack;

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
     * Lock bekleme suresi yapilandirilabilen QueryExecutor olusturur.
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            Duration transactionLockTimeout
    ) {

        this(
                databaseManager,
                queryDataSource,
                new SelectExecutor(),
                new InsertExecutor(),
                new UpdateExecutor(),
                new DeleteExecutor(),
                transactionLockTimeout
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

        this(
                databaseManager,
                queryDataSource,
                selectExecutor,
                insertExecutor,
                updateExecutor,
                deleteExecutor,
                Duration.ZERO
        );
    }

    public QueryExecutor(
            DatabaseManager databaseManager,
            QueryDataSource queryDataSource,
            SelectExecutor selectExecutor,
            InsertExecutor insertExecutor,
            UpdateExecutor updateExecutor,
            DeleteExecutor deleteExecutor,
            Duration transactionLockTimeout
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

        this.procedureExecutionSupport =
                new ProcedureExecutionSupport();

        this.explainExecutionSupport =
                new ExplainCommandExecutionSupport();

        this.transactionManager =
                new TransactionManager();

        this.transactionDurabilityLog =
                new TransactionDurabilityLog();

        this.transactionRecoveryManager =
                new TransactionRecoveryManager();

        this.writeLockManager =
                new TransactionTableWriteLockManager();

        this.heldWriteLocks =
                new HashMap<>();

        this.readLockManager =
                new TransactionTableReadLockManager();

        this.heldReadLocks =
                new HashMap<>();

        this.transactionLockOwnerId =
                UUID.randomUUID().toString();

        this.transactionLockTimeout =
                validateTransactionLockTimeout(
                        transactionLockTimeout
                );

        this.activeViewStack =
                new ArrayDeque<>();

        this.indexManager =
                new IndexManager();

        initializeTableManager();
    }

    private Duration validateTransactionLockTimeout(
            Duration timeout
    ) {

        Objects.requireNonNull(
                timeout,
                "TransactionLockTimeout cannot be null."
        );

        if (timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "TransactionLockTimeout cannot be negative."
            );
        }

        return timeout;
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
                || firstKeyword.equals("EXPLAIN")
                || firstKeyword.equals("BEGIN")
                || firstKeyword.equals("START")
                || firstKeyword.equals("COMMIT")
                || firstKeyword.equals("ROLLBACK")
                || firstKeyword.equals("SAVEPOINT")
                || firstKeyword.equals("RELEASE")
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

            enforceTransactionCommandBoundary(
                    command
            );

            if (command
                    instanceof BeginTransactionCommand value) {

                return executeBeginTransaction(
                        value
                );
            }

            if (command
                    instanceof CommitTransactionCommand value) {

                return executeCommitTransaction(
                        value
                );
            }

            if (command
                    instanceof RollbackTransactionCommand value) {

                return executeRollbackTransaction(
                        value
                );
            }

            if (command
                    instanceof SavepointCommand value) {

                return executeSavepoint(
                        value
                );
            }

            if (command
                    instanceof RollbackToSavepointCommand value) {

                return executeRollbackToSavepoint(
                        value
                );
            }

            if (command
                    instanceof ReleaseSavepointCommand value) {

                return executeReleaseSavepoint(
                        value
                );
            }

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
                    instanceof DropIndexCommand value) {

                return executeDropIndex(
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
                    instanceof CreateViewCommand value) {

                return executeCreateView(
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
                    instanceof ShowViewsCommand value) {

                return executeShowViews(
                        value
                );
            }

            if (command
                    instanceof ShowTransactionCommand value) {

                return executeShowTransaction(
                        value
                );
            }

            if (command
                    instanceof ShowSavepointsCommand value) {

                return executeShowSavepoints(
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
                    instanceof DropTriggerCommand value) {

                return executeDropTrigger(
                        value
                );
            }

            if (command
                    instanceof ShowTriggersCommand value) {

                return executeShowTriggers(
                        value
                );
            }

            if (command
                    instanceof CreateProcedureCommand value) {

                return executeCreateProcedure(
                        value
                );
            }

            if (command
                    instanceof DropProcedureCommand value) {

                return executeDropProcedure(
                        value
                );
            }

            if (command
                    instanceof ShowProceduresCommand value) {

                return executeShowProcedures(
                        value
                );
            }

            if (command
                    instanceof ShowProcedureCommand value) {

                return executeShowProcedure(
                        value
                );
            }

            if (command
                    instanceof CallProcedureCommand value) {

                return executeCallProcedure(
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
                    instanceof ExplainCommand value) {

                return executeExplain(
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
                TransactionDeadlockException exception
        ) {

            rollbackDeadlockVictim(
                    exception
            );

            throw createExecutionException(
                    command,
                    exception
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


    private void enforceTransactionCommandBoundary(
            Command command
    ) {

        if (!transactionManager.hasActiveTransaction()) {
            return;
        }

        if (!isTransactionUnsafeCommand(
                command
        )) {
            return;
        }

        throw new QueryExecutionException(
                resolveOperationName(
                        command
                )
                        + " cannot run inside an active transaction."
        );
    }

    private boolean isTransactionUnsafeCommand(
            Command command
    ) {

        return command instanceof CreateDatabaseCommand
                || command instanceof UseDatabaseCommand
                || command instanceof DropDatabaseCommand
                || command instanceof CreateTableCommand
                || command instanceof DropTableCommand
                || command instanceof AlterTableCommand
                || command instanceof CreateIndexCommand
                || command instanceof DropIndexCommand
                || command instanceof CreateViewCommand
                || command instanceof DropViewCommand
                || command instanceof CreateTriggerCommand
                || command instanceof DropTriggerCommand
                || command instanceof CreateProcedureCommand
                || command instanceof DropProcedureCommand;
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

        if (command instanceof BeginTransactionCommand) {
            return "BEGIN";
        }

        if (command instanceof CommitTransactionCommand) {
            return "COMMIT";
        }

        if (command instanceof RollbackTransactionCommand) {
            return "ROLLBACK";
        }

        if (command instanceof SavepointCommand) {
            return "SAVEPOINT";
        }

        if (command instanceof RollbackToSavepointCommand) {
            return "ROLLBACK TO SAVEPOINT";
        }

        if (command instanceof ReleaseSavepointCommand) {
            return "RELEASE SAVEPOINT";
        }

        if (command instanceof ExplainCommand) {
            return "EXPLAIN";
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

        if (command instanceof DropIndexCommand) {
            return "DROP INDEX";
        }

        if (command instanceof DropTableCommand) {
            return "DROP TABLE";
        }

        if (command instanceof AlterTableCommand) {
            return "ALTER TABLE";
        }

        if (command instanceof CreateViewCommand) {
            return "CREATE VIEW";
        }

        if (command instanceof DropViewCommand) {
            return "DROP VIEW";
        }

        if (command instanceof ShowViewsCommand) {
            return "SHOW VIEWS";
        }

        if (command instanceof ShowTransactionCommand) {
            return "SHOW TRANSACTION";
        }

        if (command instanceof ShowSavepointsCommand) {
            return "SHOW SAVEPOINTS";
        }

        if (command instanceof CreateTriggerCommand) {
            return "CREATE TRIGGER";
        }

        if (command instanceof DropTriggerCommand) {
            return "DROP TRIGGER";
        }

        if (command instanceof ShowTriggersCommand) {
            return "SHOW TRIGGERS";
        }

        if (command instanceof CreateProcedureCommand) {
            return "CREATE PROCEDURE";
        }

        if (command instanceof DropProcedureCommand) {
            return "DROP PROCEDURE";
        }

        if (command instanceof ShowProceduresCommand) {
            return "SHOW PROCEDURES";
        }

        if (command instanceof ShowProcedureCommand) {
            return "SHOW PROCEDURE";
        }

        if (command instanceof CallProcedureCommand) {
            return "CALL";
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
     * BEGIN.
     */
    private ExecuteResult executeBeginTransaction(
            BeginTransactionCommand command
    ) {

        TransactionContext transaction =
                transactionManager.begin(
                        command.getAccessMode(),
                        command.getIsolationLevel()
                );

        appendTransactionBeginLog(
                transaction
        );

        return ExecuteResult.success(
                "Transaction started successfully: "
                        + transaction.getTransactionId()
        );
    }

    /**
     * COMMIT.
     */
    private ExecuteResult executeCommitTransaction(
            CommitTransactionCommand command
    ) {

        TransactionContext transaction;

        try {

            transaction =
                    transactionManager.commit();

        } finally {
            releaseAllTransactionLocks();
        }

        appendTransactionCompletionLog(
                transaction
        );

        return ExecuteResult.success(
                "Transaction committed successfully: "
                        + transaction.getTransactionId()
        );
    }

    /**
     * ROLLBACK.
     */
    private ExecuteResult executeRollbackTransaction(
            RollbackTransactionCommand command
    ) {

        TransactionContext transaction;

        try {

            transaction =
                    transactionManager.rollback();

        } finally {
            releaseAllTransactionLocks();
        }

        appendTransactionCompletionLog(
                transaction
        );

        return ExecuteResult.success(
                "Transaction rolled back successfully: "
                        + transaction.getTransactionId()
        );
    }

    /**
     * SAVEPOINT.
     */
    private ExecuteResult executeSavepoint(
            SavepointCommand command
    ) {

        transactionManager.createSavepoint(
                command.getSavepointName()
        );

        return ExecuteResult.success(
                "Savepoint created successfully: "
                        + command.getSavepointName()
        );
    }

    /**
     * ROLLBACK TO SAVEPOINT.
     */
    private ExecuteResult executeRollbackToSavepoint(
            RollbackToSavepointCommand command
    ) {

        transactionManager.rollbackToSavepoint(
                command.getSavepointName()
        );

        return ExecuteResult.success(
                "Rolled back to savepoint successfully: "
                        + command.getSavepointName()
        );
    }

    /**
     * SHOW TRANSACTION.
     */
    private ExecuteResult executeShowTransaction(
            ShowTransactionCommand command
    ) {

        List<Column> columns =
                List.of(
                        new Column(
                                "status",
                                DataType.STRING
                        ),
                        new Column(
                                "transaction_id",
                                DataType.LONG
                        ),
                        new Column(
                                "started_at",
                                DataType.STRING
                        ),
                        new Column(
                                "undo_actions",
                                DataType.INT
                        ),
                        new Column(
                                "savepoints",
                                DataType.INT
                        ),
                        new Column(
                                "access_mode",
                                DataType.STRING
                        ),
                        new Column(
                                "isolation_level",
                                DataType.STRING
                        )
                );

        Row row =
                transactionManager
                        .getActiveTransaction()
                        .map(transaction ->
                                new Row(
                                        List.of(
                                                transaction.getStatus()
                                                        .name(),
                                                transaction.getTransactionId(),
                                                transaction.getStartedAt()
                                                        .toString(),
                                                transactionManager.getUndoActionCount(),
                                                transactionManager.getSavepointCount(),
                                                transaction.getAccessMode()
                                                        .name(),
                                                transaction.getIsolationLevel()
                                                        .name()
                                        )
                                )
                        )
                        .orElseGet(() ->
                                new Row(
                                        java.util.Arrays.asList(
                                                "INACTIVE",
                                                null,
                                                null,
                                                0,
                                                0,
                                                null,
                                                null
                                        )
                                )
                        );

        return ExecuteResult.selectSuccess(
                "Transaction status",
                columns,
                List.of(
                        row
                )
        );
    }

    /**
     * SHOW SAVEPOINTS.
     */
    private ExecuteResult executeShowSavepoints(
            ShowSavepointsCommand command
    ) {

        List<Column> columns =
                List.of(
                        new Column(
                                "ordinal",
                                DataType.INT
                        ),
                        new Column(
                                "name",
                                DataType.STRING
                        ),
                        new Column(
                                "undo_actions",
                                DataType.INT
                        )
                );

        List<Row> rows =
                new ArrayList<>();

        for (TransactionSavepointInfo savepoint
                : transactionManager.getSavepoints()) {

            rows.add(
                    new Row(
                            List.of(
                                    savepoint.ordinal(),
                                    savepoint.name(),
                                    savepoint.undoActionCount()
                            )
                    )
            );
        }

        return ExecuteResult.selectSuccess(
                "Savepoints",
                columns,
                rows
        );
    }

    /**
     * RELEASE SAVEPOINT.
     */
    private ExecuteResult executeReleaseSavepoint(
            ReleaseSavepointCommand command
    ) {

        transactionManager.releaseSavepoint(
                command.getSavepointName()
        );

        return ExecuteResult.success(
                "Savepoint released successfully: "
                        + command.getSavepointName()
        );
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

        tableManager.loadCatalog();

        transactionRecoveryManager.recover(
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
     * CREATE VIEW.
     */
    private ExecuteResult executeCreateView(
            CreateViewCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        if (requireTableManager()
                .exists(
                        command.getViewName()
                )) {

            throw new QueryExecutionException(
                    "View name conflicts with existing table: "
                            + command.getViewName()
            );
        }

        return createViewExecutor.execute(
                database,
                command
        );
    }

    /**
     * DROP VIEW.
     */
    private ExecuteResult executeDropView(
            DropViewCommand command
    ) {

        requireCurrentDatabase()
                .getViewCatalog()
                .unregisterView(
                        command.getViewName()
                );

        return ExecuteResult.success(
                "View dropped successfully: "
                        + command.getViewName()
        );
    }

    /**
     * SHOW VIEWS.
     */
    private ExecuteResult executeShowViews(
            ShowViewsCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        List<Row> rows =
                database.getViewCatalog()
                        .listViews()
                        .stream()
                        .map(view -> {
                            ViewMetadata metadata =
                                    database.getViewCatalog()
                                            .getMetadata(
                                                    view.getViewName()
                                            );

                            return new Row(
                                    List.of(
                                            view.getViewName(),
                                            view.getSourceSelect(),
                                            metadata.getVersion(),
                                            metadata.getCreatedAt()
                                                    .toString()
                                    )
                            );
                        })
                        .toList();

        return ExecuteResult.selectSuccess(
                "Views listed successfully.",
                List.of(
                        new Column("view_name", DataType.STRING),
                        new Column("source_select", DataType.STRING),
                        new Column("version", DataType.INT),
                        new Column("created_at", DataType.STRING)
                ),
                rows
        );
    }

    /**
     * CREATE TRIGGER.
     */
    private ExecuteResult executeCreateTrigger(
            CreateTriggerCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        requireTableManager()
                .getTable(
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

        database.getTriggerCatalog()
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
     * DROP TRIGGER.
     */
    private ExecuteResult executeDropTrigger(
            DropTriggerCommand command
    ) {

        requireCurrentDatabase()
                .getTriggerCatalog()
                .unregisterTrigger(
                        command.getTriggerName()
                );

        return ExecuteResult.success(
                "Trigger dropped successfully: "
                        + command.getTriggerName()
        );
    }

    /**
     * SHOW TRIGGERS.
     */
    private ExecuteResult executeShowTriggers(
            ShowTriggersCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        List<Row> rows =
                database.getTriggerCatalog()
                        .listTriggers()
                        .stream()
                        .filter(trigger ->
                                !command.hasTableName()
                                        || trigger.getTableName()
                                        .equalsIgnoreCase(
                                                command.getTableName()
                                        )
                        )
                        .map(trigger -> {
                            TriggerMetadata metadata =
                                    database.getTriggerCatalog()
                                            .getMetadata(
                                                    trigger.getTriggerName()
                                            );

                            return new Row(
                                    List.of(
                                            trigger.getTriggerName(),
                                            trigger.getTableName(),
                                            trigger.getTiming().name(),
                                            trigger.getEvent().name(),
                                            metadata.getVersion(),
                                            metadata.getCreatedAt()
                                                    .toString()
                                    )
                            );
                        })
                        .toList();

        return ExecuteResult.selectSuccess(
                "Triggers listed successfully.",
                List.of(
                        new Column("trigger_name", DataType.STRING),
                        new Column("table_name", DataType.STRING),
                        new Column("timing", DataType.STRING),
                        new Column("event", DataType.STRING),
                        new Column("version", DataType.INT),
                        new Column("created_at", DataType.STRING)
                ),
                rows
        );
    }

    /**
     * CREATE PROCEDURE.
     */
    private ExecuteResult executeCreateProcedure(
            CreateProcedureCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        ProcedureDefinition definition =
                new ProcedureDefinition(
                        command.getProcedureName(),
                        command.getParameters(),
                        command.getBody()
                );

        ProcedureMetadata metadata =
                createProcedureMetadata(
                        database,
                        definition,
                        command.isReplaceExisting()
                );

        ProcedureCatalog catalog =
                database.getProcedureCatalog();

        if (command.isIfNotExists()
                && catalog.containsProcedure(
                definition.getProcedureName()
        )) {
            return ExecuteResult.success(
                    "Procedure already exists: "
                            + definition.getProcedureName()
            );
        }

        if (command.isReplaceExisting()
                && catalog.containsProcedure(
                definition.getProcedureName()
        )) {
            catalog.replaceProcedure(
                    definition,
                    metadata
            );

            return ExecuteResult.success(
                    "Procedure replaced successfully: "
                            + definition.getProcedureName()
            );
        }

        catalog.registerProcedure(
                        definition,
                        metadata
                );

        return ExecuteResult.success(
                "Procedure created successfully: "
                        + definition.getProcedureName()
        );
    }

    private ProcedureMetadata createProcedureMetadata(
            Database database,
            ProcedureDefinition definition,
            boolean replaceExisting
    ) {

        if (!replaceExisting
                || !database.getProcedureCatalog()
                .containsProcedure(
                        definition.getProcedureName()
                )) {

            return new ProcedureMetadata(
                    definition.getProcedureName(),
                    definition.getParameterCount()
            );
        }

        ProcedureMetadata currentMetadata =
                database.getProcedureCatalog()
                        .getMetadata(
                                definition.getProcedureName()
                        );

        return new ProcedureMetadata(
                definition.getProcedureName(),
                definition.getParameterCount(),
                currentMetadata.getCreatedAt(),
                currentMetadata.getVersion() + 1
        );
    }

    /**
     * DROP PROCEDURE.
     */
    private ExecuteResult executeDropProcedure(
            DropProcedureCommand command
    ) {

        try {
            requireCurrentDatabase()
                    .getProcedureCatalog()
                    .unregisterProcedure(
                            command.getProcedureName()
                    );

        } catch (ProcedureNotFoundException exception) {
            if (!command.isIfExists()) {
                throw exception;
            }

            return ExecuteResult.success(
                    "Procedure did not exist: "
                            + command.getProcedureName()
            );
        }

        return ExecuteResult.success(
                "Procedure dropped successfully: "
                        + command.getProcedureName()
        );
    }

    /**
     * SHOW PROCEDURES.
     */
    private ExecuteResult executeShowProcedures(
            ShowProceduresCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        List<Row> rows =
                database.getProcedureCatalog()
                        .listProcedures()
                        .stream()
                        .filter(procedure ->
                                matchesProcedureFilter(
                                        procedure.getProcedureName(),
                                        command
                                )
                        )
                        .map(procedure -> {
                            ProcedureMetadata metadata =
                                    database.getProcedureCatalog()
                                            .getMetadata(
                                                    procedure.getProcedureName()
                                            );

                            return new Row(
                                    List.of(
                                            procedure.getProcedureName(),
                                            procedure.getSignature(),
                                            procedure.getParameterCount(),
                                            metadata.getVersion(),
                                            metadata.getCreatedAt()
                                                    .toString()
                                    )
                            );
                        })
                        .toList();

        return ExecuteResult.selectSuccess(
                "Procedures listed successfully.",
                List.of(
                        new Column("procedure_name", DataType.STRING),
                        new Column("signature", DataType.STRING),
                        new Column("parameter_count", DataType.INT),
                        new Column("version", DataType.INT),
                        new Column("created_at", DataType.STRING)
                ),
                rows
        );
    }

    private boolean matchesProcedureFilter(
            String procedureName,
            ShowProceduresCommand command
    ) {
        if (!command.hasLikePattern()) {
            return true;
        }

        return procedureName
                .toLowerCase(Locale.ROOT)
                .matches(
                        toLikeRegex(
                                command.getLikePattern()
                                        .toLowerCase(Locale.ROOT)
                        )
                );
    }

    private String toLikeRegex(String pattern) {
        StringBuilder regex =
                new StringBuilder("^");

        for (int index = 0;
             index < pattern.length();
             index++) {

            char character =
                    pattern.charAt(index);

            if (character == '%') {
                regex.append(".*");
                continue;
            }

            if (character == '_') {
                regex.append('.');
                continue;
            }

            if ("\\.[]{}()*+-?^$|".indexOf(character) >= 0) {
                regex.append('\\');
            }

            regex.append(character);
        }

        regex.append('$');

        return regex.toString();
    }

    /**
     * SHOW PROCEDURE.
     */
    private ExecuteResult executeShowProcedure(
            ShowProcedureCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        ProcedureDefinition procedure =
                database.getProcedureCatalog()
                        .getProcedure(
                                command.getProcedureName()
                        );

        ProcedureMetadata metadata =
                database.getProcedureCatalog()
                        .getMetadata(
                                procedure.getProcedureName()
                        );

        return ExecuteResult.selectSuccess(
                "Procedure listed successfully.",
                List.of(
                        new Column("procedure_name", DataType.STRING),
                        new Column("signature", DataType.STRING),
                        new Column("parameter_count", DataType.INT),
                        new Column("body", DataType.STRING),
                        new Column("version", DataType.INT),
                        new Column("created_at", DataType.STRING)
                ),
                List.of(
                        new Row(
                                List.of(
                                        procedure.getProcedureName(),
                                        procedure.getSignature(),
                                        procedure.getParameterCount(),
                                        procedure.getBody(),
                                        metadata.getVersion(),
                                        metadata.getCreatedAt()
                                                .toString()
                                )
                        )
                )
        );
    }

    /**
     * CALL PROCEDURE.
     */
    private ExecuteResult executeCallProcedure(
            CallProcedureCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        ProcedureDefinition procedure =
                database.getProcedureCatalog()
                        .getProcedure(
                                command.getProcedureName()
                        );

        if (command.hasNamedArguments()) {
            return procedureExecutionSupport.execute(
                    procedure,
                    command.getNamedArguments(),
                    this::execute
            );
        }

        return procedureExecutionSupport.execute(
                procedure,
                command.getArguments(),
                this::execute
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

        return executeDmlStatement(
                command.getTableName(),
                () -> executeInsertInternal(
                        command
                )
        );
    }

    private ExecuteResult executeInsertInternal(
            InsertCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        Table table =
                requireTableManager()
                        .getTable(
                                command.getTableName()
                        );

        triggerExecutionSupport.executeInsertTriggers(
                database,
                table,
                command,
                TriggerTiming.BEFORE,
                this::execute
        );

        ExecuteResult result =
                mutationExecutionSupport
                        .executeInsert(
                                requireTableManager(),
                                command,
                                indexesForTable(
                                        command.getTableName()
                                ),
                                transactionManager
                        );

        triggerExecutionSupport.executeInsertTriggers(
                database,
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

        return executeDmlStatement(
                command.getTableName(),
                () -> executeUpdateInternal(
                        command
                )
        );
    }

    private ExecuteResult executeUpdateInternal(
            UpdateCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        Table table =
                requireTableManager()
                        .getTable(
                                command.getTableName()
                        );

        if (!hasTriggersFor(
                database,
                table,
                TriggerEvent.UPDATE
        )) {

            return mutationExecutionSupport
                    .executeUpdate(
                            requireTableManager(),
                            command,
                            indexesForTable(
                                    command.getTableName()
                            ),
                            transactionManager
                    );
        }

        List<TriggerExecutionSupport.RowChange> rowChanges =
                collectUpdateRowChanges(
                        table,
                        command
                );

        triggerExecutionSupport.executeUpdateTriggers(
                database,
                table,
                rowChanges,
                TriggerTiming.BEFORE,
                this::execute
        );

        ExecuteResult result =
                mutationExecutionSupport
                        .executeUpdate(
                                requireTableManager(),
                                command,
                                indexesForTable(
                                        command.getTableName()
                                ),
                                transactionManager
                        );

        triggerExecutionSupport.executeUpdateTriggers(
                database,
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

        return executeDmlStatement(
                command.getTableName(),
                () -> executeDeleteInternal(
                        command
                )
        );
    }

    private ExecuteResult executeDeleteInternal(
            DeleteCommand command
    ) {

        Database database =
                requireCurrentDatabase();

        Table table =
                requireTableManager()
                        .getTable(
                                command.getTableName()
                        );

        if (!hasTriggersFor(
                database,
                table,
                TriggerEvent.DELETE
        )) {

            return mutationExecutionSupport
                    .executeDelete(
                            requireTableManager(),
                            command,
                            indexesForTable(
                                    command.getTableName()
                            ),
                            transactionManager
                    );
        }

        List<Row> oldRows =
                collectDeleteRows(
                        table,
                        command
                );

        triggerExecutionSupport.executeDeleteTriggers(
                database,
                table,
                oldRows,
                TriggerTiming.BEFORE,
                this::execute
        );

        ExecuteResult result =
                mutationExecutionSupport
                        .executeDelete(
                                requireTableManager(),
                                command,
                                indexesForTable(
                                        command.getTableName()
                                ),
                                transactionManager
                        );

        triggerExecutionSupport.executeDeleteTriggers(
                database,
                table,
                oldRows,
                TriggerTiming.AFTER,
                this::execute
        );

        return result;
    }

    /**
     * DML statement'lerini transaction-aware calistirir.
     *
     * Acik transaction yoksa statement icin implicit transaction acilir.
     * Acik transaction varsa hata durumunda sadece bu statement'in undo
     * kayitlari savepoint seviyesine geri sarilir.
     */
    private ExecuteResult executeDmlStatement(
            String tableName,
            Supplier<ExecuteResult> operation
    ) {

        Objects.requireNonNull(
                tableName,
                "TableName cannot be null."
        );

        Objects.requireNonNull(
                operation,
                "Operation cannot be null."
        );

        boolean implicitTransaction =
                !transactionManager.hasActiveTransaction();

        if (!implicitTransaction
                && transactionManager.isReadOnlyTransactionActive()) {

            throw new QueryExecutionException(
                    "DML cannot run inside a READ ONLY transaction."
            );
        }

        if (implicitTransaction) {
            TransactionContext transaction =
                    transactionManager.begin();
            appendTransactionBeginLog(
                    transaction
            );
        }

        int savepoint =
                transactionManager.createSavepoint();

        try {

            acquireWriteLock(
                    tableName
            );

            ExecuteResult result =
                    operation.get();

            if (implicitTransaction) {
                TransactionContext transaction =
                        transactionManager.commit();
                appendTransactionCompletionLog(
                        transaction
                );
                releaseAllTransactionLocks();
            }

            return result;

        } catch (RuntimeException exception) {

            rollbackFailedDmlStatement(
                    implicitTransaction,
                    savepoint,
                    exception
            );

            throw exception;
        }
    }

    private void appendTransactionBeginLog(
            TransactionContext transaction
    ) {

        Database database =
                databaseManager.getCurrentDatabase();

        if (database == null) {
            return;
        }

        transactionDurabilityLog.appendBegin(
                database.getDatabasePath(),
                transaction
        );
    }
    private void appendTransactionCompletionLog(
            TransactionContext transaction
    ) {

        Database database =
                databaseManager.getCurrentDatabase();

        if (database == null) {
            return;
        }

        transactionDurabilityLog.appendCompletion(
                database.getDatabasePath(),
                transaction
        );
    }
    private TransactionTableWriteLock acquireWriteLock(
            String tableName
    ) {

        Database database =
                requireCurrentDatabase();

        TransactionTableWriteLock lock;

        if (transactionLockTimeout.isZero()) {
            lock = writeLockManager.acquire(
                    database.getDatabasePath(),
                    tableName,
                    transactionLockOwnerId
            );
        } else {
            lock = writeLockManager.acquire(
                    database.getDatabasePath(),
                    tableName,
                    transactionLockOwnerId,
                    transactionLockTimeout
            );
        }

        TransactionTableWriteLock existingLock =
                heldWriteLocks.putIfAbsent(
                        lock.getLockKey(),
                        lock
                );

        if (existingLock != null) {
            lock.close();
            return existingLock;
        }

        return lock;
    }

    private void releaseAllWriteLocks() {

        List<TransactionTableWriteLock> locks =
                new ArrayList<>(
                        heldWriteLocks.values()
                );

        heldWriteLocks.clear();

        for (TransactionTableWriteLock lock : locks) {
            lock.close();
        }
    }

    private void releaseAllReadLocks() {

        List<TransactionTableReadLock> locks =
                new ArrayList<>(
                        heldReadLocks.values()
                );

        heldReadLocks.clear();

        for (TransactionTableReadLock lock : locks) {
            lock.close();
        }
    }

    private void releaseAllTransactionLocks() {

        releaseAllReadLocks();
        releaseAllWriteLocks();
    }
    private void rollbackFailedDmlStatement(
            boolean implicitTransaction,
            int savepoint,
            RuntimeException originalException
    ) {

        try {

            if (implicitTransaction) {
                TransactionContext transaction =
                        transactionManager.rollback();
                appendTransactionCompletionLog(
                        transaction
                );
                releaseAllTransactionLocks();
            } else {
                transactionManager.rollbackToSavepoint(
                        savepoint
                );
            }

        } catch (RuntimeException rollbackException) {
            originalException.addSuppressed(
                    rollbackException
            );
        }
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

        Database database =
                databaseManager.getCurrentDatabase();

        if (database == null) {
            return executeUnlockedSelect(
                    command
            );
        }

        if (database.getViewCatalog()
                .containsView(
                        command.getTableName()
                )) {

            return executeSelectFromView(
                    database,
                    command
            );
        }

        return executeSelectWithReadLocks(
                command
        );
    }

    private ExecuteResult executeSelectWithReadLocks(
            SelectCommand command
    ) {

        TransactionIsolationLevel isolationLevel =
                activeIsolationLevel();

        if (!isolationLevel.requiresReadLock()) {
            return executeUnlockedSelect(
                    command
            );
        }

        boolean transactionScoped =
                isolationLevel
                        .holdsReadLockUntilTransactionCompletion();

        List<TransactionTableReadLock> statementLocks =
                new ArrayList<>();

        try {

            for (String tableName : selectTableNames(command)) {

                TransactionTableReadLock lock =
                        acquireReadLock(
                                tableName,
                                transactionScoped
                        );

                if (!transactionScoped) {
                    statementLocks.add(lock);
                }
            }

            return executeUnlockedSelect(
                    command
            );

        } finally {

            for (int index = statementLocks.size() - 1;
                 index >= 0;
                 index--) {

                statementLocks.get(index)
                        .close();
            }
        }
    }

    private void rollbackDeadlockVictim(
            TransactionDeadlockException originalException
    ) {

        if (!transactionManager.hasActiveTransaction()) {
            return;
        }

        try {

            TransactionContext transaction =
                    transactionManager.rollback();

            try {
                appendTransactionCompletionLog(
                        transaction
                );
            } catch (RuntimeException logException) {
                originalException.addSuppressed(
                        logException
                );
            }

        } catch (RuntimeException rollbackException) {
            originalException.addSuppressed(
                    rollbackException
            );
        } finally {
            releaseAllTransactionLocks();
        }
    }

    private ExecuteResult executeUnlockedSelect(
            SelectCommand command
    ) {

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

    private TransactionTableReadLock acquireReadLock(
            String tableName,
            boolean transactionScoped
    ) {

        Database database =
                requireCurrentDatabase();

        TransactionTableReadLock lock;

        if (transactionLockTimeout.isZero()) {
            lock = readLockManager.acquire(
                    database.getDatabasePath(),
                    tableName,
                    transactionLockOwnerId
            );
        } else {
            lock = readLockManager.acquire(
                    database.getDatabasePath(),
                    tableName,
                    transactionLockOwnerId,
                    transactionLockTimeout
            );
        }

        if (!transactionScoped) {
            return lock;
        }

        TransactionTableReadLock existingLock =
                heldReadLocks.putIfAbsent(
                        lock.getLockKey(),
                        lock
                );

        if (existingLock != null) {
            lock.close();
            return existingLock;
        }

        return lock;
    }

    private TransactionIsolationLevel activeIsolationLevel() {

        return transactionManager.getActiveTransaction()
                .map(TransactionContext::getIsolationLevel)
                .orElse(
                        TransactionIsolationLevel.READ_COMMITTED
                );
    }

    private List<String> selectTableNames(
            SelectCommand command
    ) {

        List<String> tableNames =
                new ArrayList<>();

        addTableNameIfMissing(
                tableNames,
                command.getTableName()
        );

        command.getJoins()
                .forEach(join ->
                        addTableNameIfMissing(
                                tableNames,
                                join.getTableName()
                        )
                );

        tableNames.sort(
                String.CASE_INSENSITIVE_ORDER
        );

        return List.copyOf(
                tableNames
        );
    }

    private void addTableNameIfMissing(
            List<String> tableNames,
            String tableName
    ) {

        boolean alreadyExists =
                tableNames.stream()
                        .anyMatch(existing ->
                                existing.equalsIgnoreCase(
                                        tableName
                                )
                        );

        if (!alreadyExists) {
            tableNames.add(tableName);
        }
    }

    /**
     * View SELECT işlemini source SELECT sonucunu materialize ederek yürütür.
     */
    private ExecuteResult executeSelectFromView(
            Database database,
            SelectCommand command
    ) {

        String viewName =
                command.getTableName()
                        .toLowerCase(Locale.ROOT);

        if (activeViewStack.contains(viewName)) {
            throw new QueryExecutionException(
                    "Recursive view reference detected: "
                            + command.getTableName()
            );
        }

        activeViewStack.addLast(viewName);

        try {
            ViewDefinition definition =
                    database.getViewCatalog()
                            .getView(
                                    command.getTableName()
                            );

            ExecuteResult sourceResult =
                    execute(
                            definition.getSourceSelect()
                    );

            if (!sourceResult.isSuccess()) {
                return sourceResult;
            }

            QueryDataSource materializedDataSource =
                    materializeViewResult(
                            command.getTableName(),
                            sourceResult
                    );

            SelectCommand rewrittenCommand =
                    SelectCommand.fromStatement(
                            rewriteSelectTable(
                                    command.getStatement(),
                                    command.getTableName()
                            )
                    );

            return selectExecutionSupport.execute(
                    rewrittenCommand,
                    materializedDataSource
            );

        } finally {
            activeViewStack.removeLast();
        }
    }

    /**
     * EXPLAIN SELECT.
     */
    private ExecuteResult executeExplain(
            ExplainCommand command
    ) {

        Database database =
                databaseManager.getCurrentDatabase();

        if (database != null
                && database.getViewCatalog()
                .containsView(
                        command.getSelectStatement()
                                .getTableName()
                )) {

            return executeExplainFromView(
                    database,
                    command
            );
        }

        return explainExecutionSupport.execute(
                command,
                requireQueryDataSource(),
                indexesForTable(
                        command.getSelectStatement()
                                .getTableName()
                )
        );
    }

    /**
     * View hedefli EXPLAIN komutunda alttaki source SELECT planını gösterir.
     */
    private ExecuteResult executeExplainFromView(
            Database database,
            ExplainCommand command
    ) {

        String viewName =
                command.getSelectStatement()
                        .getTableName()
                        .toLowerCase(Locale.ROOT);

        if (activeViewStack.contains(viewName)) {
            throw new QueryExecutionException(
                    "Recursive view reference detected: "
                            + command.getSelectStatement()
                            .getTableName()
            );
        }

        activeViewStack.addLast(viewName);

        try {
            ViewDefinition definition =
                    database.getViewCatalog()
                            .getView(
                                    command.getSelectStatement()
                                            .getTableName()
                            );

            SelectStatement sourceStatement =
                    parseViewSourceSelect(
                            definition
                    );

            ExecuteResult sourceExplain =
                    explainExecutionSupport.execute(
                            new ExplainCommand(
                                    sourceStatement
                            ),
                            requireQueryDataSource(),
                            indexesForTable(
                                    sourceStatement.getTableName()
                            )
                    );

            List<Row> rows =
                    new ArrayList<>();

            rows.add(
                    new Row(
                            List.of(
                                    "VIEW: "
                                            + definition.getViewName()
                            )
                    )
            );

            rows.add(
                    new Row(
                            List.of(
                                    "VIEW_SOURCE: "
                                            + definition.getSourceSelect()
                            )
                    )
            );

            rows.addAll(
                    sourceExplain.getRows()
            );

            return ExecuteResult.selectSuccess(
                    "EXPLAIN view query executed successfully.",
                    sourceExplain.getColumns(),
                    rows
            );

        } finally {
            activeViewStack.removeLast();
        }
    }

    private SelectStatement parseViewSourceSelect(
            ViewDefinition definition
    ) {

        Statement statement =
                new SqlParser()
                        .parse(
                                definition.getSourceSelect()
                        );

        if (!(statement instanceof SelectStatement selectStatement)) {
            throw new QueryExecutionException(
                    "View source must be a SELECT statement: "
                            + definition.getViewName()
            );
        }

        return selectStatement;
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

    private boolean hasTriggersFor(
            Database database,
            Table table,
            TriggerEvent event
    ) {

        return !database.getTriggerCatalog()
                .findTriggers(
                        table.getTableName(),
                        TriggerTiming.BEFORE,
                        event
                )
                .isEmpty()
                || !database.getTriggerCatalog()
                .findTriggers(
                        table.getTableName(),
                        TriggerTiming.AFTER,
                        event
                )
                .isEmpty();
    }

    private List<TriggerExecutionSupport.RowChange> collectUpdateRowChanges(
            Table table,
            UpdateCommand command
    ) {

        QueryDataSource dataSource =
                requireQueryDataSource();

        List<TriggerExecutionSupport.RowChange> changes =
                new ArrayList<>();

        for (Row row : dataSource.getRows(
                command.getTableName()
        )) {

            if (command.hasWhereExpression()
                    && !WhereEvaluator.evaluate(
                    command.getWhereExpression(),
                    row,
                    table
            )) {
                continue;
            }

            changes.add(
                    new TriggerExecutionSupport.RowChange(
                            row,
                            createUpdatedRow(
                                    table,
                                    row,
                                    command.getUpdatedValues()
                            )
                    )
            );
        }

        return List.copyOf(changes);
    }

    private List<Row> collectDeleteRows(
            Table table,
            DeleteCommand command
    ) {

        QueryDataSource dataSource =
                requireQueryDataSource();

        List<Row> rows =
                new ArrayList<>();

        for (Row row : dataSource.getRows(
                command.getTableName()
        )) {

            if (command.hasWhereExpression()
                    && !WhereEvaluator.evaluate(
                    command.getWhereExpression(),
                    row,
                    table
            )) {
                continue;
            }

            rows.add(row);
        }

        return List.copyOf(rows);
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

        for (Map.Entry<String, Object> entry
                : updatedValues.entrySet()) {

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

    private QueryDataSource materializeViewResult(
            String viewName,
            ExecuteResult sourceResult
    ) {

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                new Table(
                        viewName,
                        sourceResult.getColumns()
                ),
                sourceResult.getRows()
        );

        return dataSource;
    }

    private SelectStatement rewriteSelectTable(
            SelectStatement statement,
            String tableName
    ) {

        return new SelectStatement(
                new TableReference(
                        tableName,
                        statement.getTableAlias()
                ),
                statement.getSelectItems(),
                statement.getJoins(),
                statement.getWhereExpression(),
                statement.getGroupByClause(),
                statement.getHavingClause(),
                statement.getOrderByItems(),
                statement.getLimitClause(),
                statement.getFetchClause()
        );
    }

    private Database requireCurrentDatabase() {

        Database database =
                databaseManager.getCurrentDatabase();

        if (database == null) {
            throw new QueryExecutionException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        return database;
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

        try {

            if (transactionManager.hasActiveTransaction()) {
                TransactionContext transaction =
                        transactionManager.rollback();
                appendTransactionCompletionLog(
                        transaction
                );
            }

        } finally {
            releaseAllTransactionLocks();
        }

        tableManager = null;
        indexManager = new IndexManager();
    }
}
