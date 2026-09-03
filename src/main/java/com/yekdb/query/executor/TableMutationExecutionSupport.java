package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.index.RecordPointer;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import com.yekdb.transaction.TransactionManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
        return executeInsert(
                tableManager,
                command,
                List.of()
        );
    }

    ExecuteResult executeInsert(
            TableManager tableManager,
            InsertCommand command,
            List<Index<?>> indexes
    ) {
        return executeInsert(
                tableManager,
                command,
                indexes,
                null
        );
    }

    ExecuteResult executeInsert(
            TableManager tableManager,
            InsertCommand command,
            List<Index<?>> indexes,
            TransactionManager transactionManager
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
                    recordManager,
                    tableManager,
                            indexes == null ? List.of() : indexes
            );

            registerInsertUndoIfNeeded(
                    tableManager,
                    table,
                    insertedRecord,
                    recordManager,
                    indexes,
                    transactionManager
            );

            return ExecuteResult.success(
                    "Row inserted successfully into table '"
                            + table.getTableName()
                            + "'. Record ID: "
                            + insertedRecord.getRecordId(),
                    1
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
        return executeUpdate(
                tableManager,
                command,
                List.of()
        );
    }

    ExecuteResult executeUpdate(
            TableManager tableManager,
            UpdateCommand command,
            List<Index<?>> indexes
    ) {
        return executeUpdate(
                tableManager,
                command,
                indexes,
                null
        );
    }

    ExecuteResult executeUpdate(
            TableManager tableManager,
            UpdateCommand command,
            List<Index<?>> indexes,
            TransactionManager transactionManager
    ) {
        Objects.requireNonNull(command, "UpdateCommand cannot be null.");

        Table table = requireTable(tableManager, command.getTableName());
        StorageEngine storageEngine = createStorageEngine(tableManager, table);

        try {
            storageEngine.initialize();

            RecordManager recordManager = createRecordManager(storageEngine);
            List<RowSnapshot> snapshots =
                    transactionManager != null
                            && transactionManager.hasActiveTransaction()
                            ? collectUpdateSnapshots(
                            table,
                            command,
                            recordManager
                    )
                            : List.of();

            int updatedRowCount = updateExecutor.execute(
                    table,
                    command,
                    recordManager,
                    tableManager,
                            indexes == null ? List.of() : indexes
            );

            registerUpdateUndoIfNeeded(
                    tableManager,
                    table,
                    snapshots,
                    indexes,
                    transactionManager
            );

            return ExecuteResult.success(
                    "UPDATE executed successfully on table '"
                            + table.getTableName()
                            + "'. Updated row count: "
                            + updatedRowCount,
                    updatedRowCount
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
        return executeDelete(
                tableManager,
                command,
                List.of()
        );
    }

    ExecuteResult executeDelete(
            TableManager tableManager,
            DeleteCommand command,
            List<Index<?>> indexes
    ) {
        return executeDelete(
                tableManager,
                command,
                indexes,
                null
        );
    }

    ExecuteResult executeDelete(
            TableManager tableManager,
            DeleteCommand command,
            List<Index<?>> indexes,
            TransactionManager transactionManager
    ) {
        Objects.requireNonNull(command, "DeleteCommand cannot be null.");

        Table table = requireTable(tableManager, command.getTableName());
        StorageEngine storageEngine = createStorageEngine(tableManager, table);

        try {
            storageEngine.initialize();

            RecordManager recordManager = createRecordManager(storageEngine);
            List<RowSnapshot> snapshots =
                    transactionManager != null
                            && transactionManager.hasActiveTransaction()
                            ? collectDeleteSnapshots(
                            table,
                            command,
                            recordManager
                    )
                            : List.of();

            int deletedRowCount = deleteExecutor.execute(
                    table,
                    command,
                    recordManager,
                    tableManager,
                            indexes == null ? List.of() : indexes
            );

            registerDeleteUndoIfNeeded(
                    tableManager,
                    table,
                    snapshots,
                    indexes,
                    transactionManager
            );

            return ExecuteResult.success(
                    "DELETE executed successfully on table '"
                            + table.getTableName()
                            + "'. Deleted row count: "
                            + deletedRowCount,
                    deletedRowCount
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

    private List<RowSnapshot> collectUpdateSnapshots(
            Table table,
            UpdateCommand command,
            RecordManager recordManager
    ) throws IOException {

        List<RowSnapshot> snapshots =
                new ArrayList<>();

        for (Record record : recordManager.getActiveRecords()) {

            long recordId =
                    record.getRecordId();

            Row currentRow =
                    recordManager.getRow(
                            recordId
                    );

            if (!matchesUpdateWhere(
                    table,
                    currentRow,
                    command
            )) {
                continue;
            }

            snapshots.add(
                    new RowSnapshot(
                            recordId,
                            new Row(
                                    currentRow.getValues()
                            )
                    )
            );
        }

        return snapshots;
    }

    private List<RowSnapshot> collectDeleteSnapshots(
            Table table,
            DeleteCommand command,
            RecordManager recordManager
    ) throws IOException {

        List<RowSnapshot> snapshots =
                new ArrayList<>();

        for (Record record : recordManager.getActiveRecords()) {

            long recordId =
                    record.getRecordId();

            Row currentRow =
                    recordManager.getRow(
                            recordId
                    );

            if (!matchesDeleteWhere(
                    table,
                    currentRow,
                    command
            )) {
                continue;
            }

            snapshots.add(
                    new RowSnapshot(
                            recordId,
                            new Row(
                                    currentRow.getValues()
                            )
                    )
            );
        }

        return snapshots;
    }

    private boolean matchesUpdateWhere(
            Table table,
            Row row,
            UpdateCommand command
    ) {

        if (!command.hasWhereExpression()) {
            return true;
        }

        return com.yekdb.query.evaluator.WhereEvaluator.evaluate(
                command.getWhereExpression(),
                row,
                table
        );
    }

    private boolean matchesDeleteWhere(
            Table table,
            Row row,
            DeleteCommand command
    ) {

        if (!command.hasWhereExpression()) {
            return true;
        }

        return com.yekdb.query.evaluator.WhereEvaluator.evaluate(
                command.getWhereExpression(),
                row,
                table
        );
    }

    private void registerInsertUndoIfNeeded(
            TableManager tableManager,
            Table table,
            Record insertedRecord,
            RecordManager recordManager,
            List<Index<?>> indexes,
            TransactionManager transactionManager
    ) throws IOException {

        if (transactionManager == null
                || !transactionManager.hasActiveTransaction()) {
            return;
        }

        long recordId =
                insertedRecord.getRecordId();

        Row insertedRow =
                recordManager.getRow(
                        recordId
                );

        com.yekdb.storage.record.RecordId physicalRecordId =
                recordManager.findPhysicalRecordId(
                        recordId
                );

        if (physicalRecordId == null) {
            throw new IllegalStateException(
                    "Physical RecordId could not be resolved for INSERT undo."
            );
        }

        transactionManager.registerUndoAction(
                () -> rollbackInsert(
                        tableManager,
                        table.getTableName(),
                        recordId,
                        insertedRow,
                        RecordPointer.fromRecordId(
                                physicalRecordId
                        ),
                        safeIndexes(indexes)
                )
        );
    }

    private void registerUpdateUndoIfNeeded(
            TableManager tableManager,
            Table table,
            List<RowSnapshot> snapshots,
            List<Index<?>> indexes,
            TransactionManager transactionManager
    ) {

        if (transactionManager == null
                || !transactionManager.hasActiveTransaction()
                || snapshots.isEmpty()) {
            return;
        }

        List<RowSnapshot> safeSnapshots =
                List.copyOf(
                        snapshots
                );

        transactionManager.registerUndoAction(
                () -> rollbackUpdate(
                        tableManager,
                        table.getTableName(),
                        safeSnapshots,
                        safeIndexes(indexes)
                )
        );
    }

    private void registerDeleteUndoIfNeeded(
            TableManager tableManager,
            Table table,
            List<RowSnapshot> snapshots,
            List<Index<?>> indexes,
            TransactionManager transactionManager
    ) {

        if (transactionManager == null
                || !transactionManager.hasActiveTransaction()
                || snapshots.isEmpty()) {
            return;
        }

        List<RowSnapshot> safeSnapshots =
                List.copyOf(
                        snapshots
                );

        transactionManager.registerUndoAction(
                () -> rollbackDelete(
                        tableManager,
                        table.getTableName(),
                        safeSnapshots,
                        safeIndexes(indexes)
                )
        );
    }

    private void rollbackInsert(
            TableManager tableManager,
            String tableName,
            long recordId,
            Row insertedRow,
            RecordPointer pointer,
            List<Index<?>> indexes
    ) {

        Table table =
                requireTable(
                        tableManager,
                        tableName
                );

        StorageEngine storageEngine =
                createStorageEngine(
                        tableManager,
                        table
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    createRecordManager(
                            storageEngine
                    );

            if (!recordManager.isActive(
                    recordId
            )) {
                return;
            }

            recordManager.delete(
                    recordId
            );

            IndexMaintenanceSupport.applyDelete(
                    table,
                    insertedRow,
                    pointer,
                    indexes
            );

        } catch (IOException exception) {
            throw storageFailure(
                    "ROLLBACK INSERT",
                    table,
                    exception
            );

        } finally {
            shutdownStorageEngine(
                    storageEngine,
                    table
            );
        }
    }

    private void rollbackUpdate(
            TableManager tableManager,
            String tableName,
            List<RowSnapshot> snapshots,
            List<Index<?>> indexes
    ) {

        Table table =
                requireTable(
                        tableManager,
                        tableName
                );

        StorageEngine storageEngine =
                createStorageEngine(
                        tableManager,
                        table
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    createRecordManager(
                            storageEngine
                    );

            for (int index = snapshots.size() - 1;
                 index >= 0;
                 index--) {

                RowSnapshot snapshot =
                        snapshots.get(
                                index
                        );

                if (!recordManager.isActive(
                        snapshot.recordId()
                )) {
                    continue;
                }

                Row currentRow =
                        recordManager.getRow(
                                snapshot.recordId()
                        );

                RecordPointer currentPointer =
                        RecordPointer.fromRecordId(
                                recordManager.findPhysicalRecordId(
                                        snapshot.recordId()
                                )
                        );

                recordManager.update(
                        snapshot.recordId(),
                        snapshot.row()
                );

                RecordPointer restoredPointer =
                        RecordPointer.fromRecordId(
                                recordManager.findPhysicalRecordId(
                                        snapshot.recordId()
                                )
                        );

                IndexMaintenanceSupport.applyUpdate(
                        table,
                        currentRow,
                        snapshot.row(),
                        currentPointer,
                        restoredPointer,
                        indexes
                );
            }

        } catch (IOException exception) {
            throw storageFailure(
                    "ROLLBACK UPDATE",
                    table,
                    exception
            );

        } finally {
            shutdownStorageEngine(
                    storageEngine,
                    table
            );
        }
    }

    private void rollbackDelete(
            TableManager tableManager,
            String tableName,
            List<RowSnapshot> snapshots,
            List<Index<?>> indexes
    ) {

        Table table =
                requireTable(
                        tableManager,
                        tableName
                );

        StorageEngine storageEngine =
                createStorageEngine(
                        tableManager,
                        table
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    createRecordManager(
                            storageEngine
                    );

            for (int index = snapshots.size() - 1;
                 index >= 0;
                 index--) {

                RowSnapshot snapshot =
                        snapshots.get(
                                index
                        );

                if (recordManager.isActive(
                        snapshot.recordId()
                )) {
                    continue;
                }

                recordManager.restoreDeleted(
                        snapshot.recordId(),
                        snapshot.row()
                );

                RecordPointer restoredPointer =
                        RecordPointer.fromRecordId(
                                recordManager.findPhysicalRecordId(
                                        snapshot.recordId()
                                )
                        );

                IndexMaintenanceSupport.applyInsert(
                        table,
                        snapshot.row(),
                        restoredPointer,
                        indexes
                );
            }

        } catch (IOException exception) {
            throw storageFailure(
                    "ROLLBACK DELETE",
                    table,
                    exception
            );

        } finally {
            shutdownStorageEngine(
                    storageEngine,
                    table
            );
        }
    }

    private List<Index<?>> safeIndexes(
            List<Index<?>> indexes
    ) {

        return indexes == null
                ? List.of()
                : List.copyOf(
                        indexes
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

    private record RowSnapshot(
            long recordId,
            Row row
    ) {

        private RowSnapshot {
            Objects.requireNonNull(
                    row,
                    "Row cannot be null."
            );
        }
    }
}
