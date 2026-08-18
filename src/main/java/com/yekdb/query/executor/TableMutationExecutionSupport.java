package com.yekdb.query.executor;

import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.table.Table;
import com.yekdb.table.TableManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * INSERT, UPDATE ve DELETE işlemlerinin ortak fiziksel storage
 * yaşam döngüsünü yönetir.
 *
 * <p>QueryExecutor komut yönlendirmesine odaklanırken bu sınıf:</p>
 * <ul>
 *     <li>tablo şemasını bulur,</li>
 *     <li>tabloya ait .data dosyasını açar,</li>
 *     <li>RecordManager oluşturur,</li>
 *     <li>ilgili mutation executor'ını çalıştırır,</li>
 *     <li>StorageEngine'i güvenli biçimde kapatır.</li>
 * </ul>
 */
final class TableMutationExecutionSupport {

    private static final String DATA_FILE_EXTENSION = ".data";

    private final InsertExecutor insertExecutor;
    private final UpdateExecutor updateExecutor;
    private final DeleteExecutor deleteExecutor;

    TableMutationExecutionSupport(
            InsertExecutor insertExecutor,
            UpdateExecutor updateExecutor,
            DeleteExecutor deleteExecutor
    ) {
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
    }

    ExecuteResult executeInsert(
            TableManager tableManager,
            InsertCommand command
    ) {
        Objects.requireNonNull(command, "InsertCommand cannot be null.");

        Table table = requireTable(tableManager, command.getTableName());
        StorageEngine storageEngine = createStorageEngine(tableManager, table);

        try {
            storageEngine.initialize();

            RecordManager recordManager = createRecordManager(storageEngine);
            Record insertedRecord = insertExecutor.execute(
                    table,
                    command,
                    recordManager
            );

            return ExecuteResult.success(
                    "Row inserted successfully into table '"
                            + table.getTableName()
                            + "'. Record ID: "
                            + insertedRecord.getRecordId()
            );

        } catch (IOException exception) {
            throw storageFailure("INSERT", table, exception);

        } finally {
            shutdownStorageEngine(storageEngine, table);
        }
    }

    ExecuteResult executeUpdate(
            TableManager tableManager,
            UpdateCommand command
    ) {
        Objects.requireNonNull(command, "UpdateCommand cannot be null.");

        Table table = requireTable(tableManager, command.getTableName());
        StorageEngine storageEngine = createStorageEngine(tableManager, table);

        try {
            storageEngine.initialize();

            RecordManager recordManager = createRecordManager(storageEngine);
            int updatedRowCount = updateExecutor.execute(
                    table,
                    command,
                    recordManager
            );

            return ExecuteResult.success(
                    "UPDATE executed successfully on table '"
                            + table.getTableName()
                            + "'. Updated row count: "
                            + updatedRowCount
            );

        } catch (IOException exception) {
            throw storageFailure("UPDATE", table, exception);

        } finally {
            shutdownStorageEngine(storageEngine, table);
        }
    }

    ExecuteResult executeDelete(
            TableManager tableManager,
            DeleteCommand command
    ) {
        Objects.requireNonNull(command, "DeleteCommand cannot be null.");

        Table table = requireTable(tableManager, command.getTableName());
        StorageEngine storageEngine = createStorageEngine(tableManager, table);

        try {
            storageEngine.initialize();

            RecordManager recordManager = createRecordManager(storageEngine);
            int deletedRowCount = deleteExecutor.execute(
                    table,
                    command,
                    recordManager
            );

            return ExecuteResult.success(
                    "DELETE executed successfully on table '"
                            + table.getTableName()
                            + "'. Deleted row count: "
                            + deletedRowCount
            );

        } catch (IOException exception) {
            throw storageFailure("DELETE", table, exception);

        } finally {
            shutdownStorageEngine(storageEngine, table);
        }
    }

    private Table requireTable(
            TableManager tableManager,
            String tableName
    ) {
        Objects.requireNonNull(
                tableManager,
                "TableManager cannot be null."
        );

        return tableManager.getTable(tableName);
    }

    private StorageEngine createStorageEngine(
            TableManager tableManager,
            Table table
    ) {
        Path tableDataFile = tableManager
                .getDatabaseDirectory()
                .resolve(
                        table.getTableName()
                                .toLowerCase(Locale.ROOT)
                                + DATA_FILE_EXTENSION
                );

        return new StorageEngine(tableDataFile);
    }

    private RecordManager createRecordManager(
            StorageEngine storageEngine
    ) throws IOException {
        return new RecordManager(
                storageEngine.getPageManager(),
                PageType.DATA
        );
    }

    private QueryExecutionException storageFailure(
            String operationName,
            Table table,
            IOException exception
    ) {
        return new QueryExecutionException(
                operationName
                        + " storage operation failed for table: "
                        + table.getTableName(),
                exception
        );
    }

    private void shutdownStorageEngine(
            StorageEngine storageEngine,
            Table table
    ) {
        if (!storageEngine.isInitialized()) {
            return;
        }

        try {
            storageEngine.shutdown();
        } catch (IOException exception) {
            throw new QueryExecutionException(
                    "Failed to close storage engine for table: "
                            + table.getTableName(),
                    exception
            );
        }
    }
}
