package com.yekdb.query.executor;

import com.yekdb.query.command.AddColumnAlterAction;
import com.yekdb.query.command.AddConstraintAlterAction;
import com.yekdb.query.command.AlterTableAction;
import com.yekdb.query.command.AlterTableCommand;
import com.yekdb.query.command.AlterColumnDropNotNullAction;
import com.yekdb.query.command.AlterColumnSetNotNullAction;
import com.yekdb.query.command.DropColumnAlterAction;
import com.yekdb.query.command.DropConstraintAlterAction;
import com.yekdb.query.command.RenameColumnAlterAction;
import com.yekdb.query.command.RenameTableAlterAction;
import com.yekdb.storage.table.ColumnTypeDefinition;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.TableManager;

import java.util.Locale;
import java.util.Objects;

/**
 * ALTER TABLE komutlarının execution katmanıdır.
 *
 * Sprint 00-26 Phase 3 kapsamında ilk fiziksel şema işlemleri:
 * - ADD COLUMN
 * - DROP COLUMN
 * - RENAME COLUMN
 *
 * ADD/DROP COLUMN mevcut kayıtların fiziksel olarak yeniden yazılmasını
 * gerektirdiğinden bu phase'te yalnızca boş tablolarda desteklenir.
 */
public final class AlterTableExecutor {

    public ExecuteResult execute(
            TableManager tableManager,
            AlterTableCommand command
    ) {
        Objects.requireNonNull(tableManager, "TableManager cannot be null.");
        Objects.requireNonNull(command, "AlterTableCommand cannot be null.");

        AlterTableAction action = command.action();
        String tableName = command.tableName();

        if (action instanceof AddColumnAlterAction value) {
            ColumnTypeDefinition typeDefinition = parseColumnType(value.dataType());
            tableManager.addColumn(
                    tableName,
                    value.columnName(),
                    typeDefinition
            );
            return ExecuteResult.success(
                    "Column added successfully: " + value.columnName()
            );
        }

        if (action instanceof DropColumnAlterAction value) {
            tableManager.dropColumn(
                    tableName,
                    value.columnName()
            );
            return ExecuteResult.success(
                    "Column dropped successfully: " + value.columnName()
            );
        }

        if (action instanceof RenameColumnAlterAction value) {
            tableManager.renameColumn(
                    tableName,
                    value.oldColumnName(),
                    value.newColumnName()
            );
            return ExecuteResult.success(
                    "Column renamed successfully: "
                            + value.oldColumnName()
                            + " -> "
                            + value.newColumnName()
            );
        }

        if (action instanceof RenameTableAlterAction value) {
            tableManager.renameTable(
                    tableName,
                    value.newTableName()
            );
            return ExecuteResult.success(
                    "Table renamed successfully: "
                            + tableName
                            + " -> "
                            + value.newTableName()
            );
        }

        if (action instanceof AlterColumnSetNotNullAction value) {
            tableManager.setColumnNotNull(
                    tableName,
                    value.columnName()
            );
            return ExecuteResult.success(
                    "NOT NULL added successfully: " + value.columnName()
            );
        }

        if (action instanceof AlterColumnDropNotNullAction value) {
            tableManager.dropColumnNotNull(
                    tableName,
                    value.columnName()
            );
            return ExecuteResult.success(
                    "NOT NULL dropped successfully: " + value.columnName()
            );
        }

        if (action instanceof AddConstraintAlterAction value) {
            tableManager.addConstraint(
                    tableName,
                    value.constraint()
            );
            String constraintLabel = value.constraint().isNamed()
                    ? value.constraint().name()
                    : value.constraint().type()
                    + " "
                    + value.constraint().columns();

            return ExecuteResult.success(
                    "Constraint added successfully: "
                            + constraintLabel
            );
        }

        if (action instanceof DropConstraintAlterAction value) {
            tableManager.dropConstraint(
                    tableName,
                    value.constraintName()
            );
            return ExecuteResult.success(
                    "Constraint dropped successfully: "
                            + value.constraintName()
            );
        }

        throw new QueryExecutionException(
                "ALTER TABLE action is not executable yet: "
                        + action.getClass().getSimpleName()
        );
    }

    private DataType parseDataType(String value) {
        return parseColumnType(value).dataType();
    }

    private ColumnTypeDefinition parseColumnType(String value) {
        try {
            return ColumnTypeDefinition.parse(value);
        } catch (RuntimeException exception) {
            throw new QueryExecutionException(
                    "Unsupported column data type: " + value,
                    exception
            );
        }
    }
}
