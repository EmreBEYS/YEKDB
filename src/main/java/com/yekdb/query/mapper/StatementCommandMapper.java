package com.yekdb.query.mapper;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.executor.QueryExecutionException;
import com.yekdb.query.statement.DeleteStatement;
import com.yekdb.query.statement.InsertStatement;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.UpdateStatement;

import java.util.Objects;

/**
 * Parser tarafından oluşturulan Statement nesnelerini,
 * QueryExecutor tarafından çalıştırılabilen Command
 * nesnelerine dönüştürür.
 *
 * <p>Bu sınıf yalnızca eşleme işlemi yapar.
 * SQL ayrıştırmaz ve komut yürütmez.</p>
 */
public final class StatementCommandMapper {

    /**
     * Utility sınıfı olduğu için nesne oluşturulamaz.
     */
    private StatementCommandMapper() {
    }

    /**
     * Verilen Statement nesnesini uygun Command
     * nesnesine dönüştürür.
     *
     * @param statement parser tarafından oluşturulan statement
     * @return çalıştırılabilir command
     */
    public static Command map(Statement statement) {
        Objects.requireNonNull(
                statement,
                "Statement cannot be null."
        );

        if (statement instanceof InsertStatement insertStatement) {
            return mapInsert(insertStatement);
        }

        if (statement instanceof SelectStatement selectStatement) {
            return mapSelect(selectStatement);
        }

        if (statement instanceof DeleteStatement deleteStatement) {
            return mapDelete(deleteStatement);
        }

        if (statement instanceof UpdateStatement) {
            throw new QueryExecutionException(
                    "UPDATE command is not supported yet."
            );
        }

        throw new QueryExecutionException(
                "Unsupported statement type: "
                        + statement.getClass().getSimpleName()
        );
    }

    /**
     * InsertStatement nesnesini InsertCommand
     * nesnesine dönüştürür.
     */
    private static InsertCommand mapInsert(
            InsertStatement statement
    ) {
        return new InsertCommand(
                statement.getTableName(),
                statement.getValues()
        );
    }

    /**
     * SelectStatement nesnesini SelectCommand
     * nesnesine dönüştürür.
     */
    private static SelectCommand mapSelect(
            SelectStatement statement
    ) {
        if (statement.selectsAllColumns()) {
            return SelectCommand.allFrom(
                    statement.getTableName()
            );
        }

        return SelectCommand.columnsFrom(
                statement.getTableName(),
                statement.getSelectedColumns()
        );
    }

    /**
     * DeleteStatement nesnesini DeleteCommand
     * nesnesine dönüştürür.
     */
    private static DeleteCommand mapDelete(
            DeleteStatement statement
    ) {
        return new DeleteCommand(
                statement.getTableName(),
                statement.getWhereClause()
        );
    }
}