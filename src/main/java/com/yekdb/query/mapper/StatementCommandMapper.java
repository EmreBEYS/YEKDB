package com.yekdb.query.mapper;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.BeginTransactionCommand;
import com.yekdb.query.command.CommitTransactionCommand;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.ExplainCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.ReleaseSavepointCommand;
import com.yekdb.query.command.RollbackTransactionCommand;
import com.yekdb.query.command.RollbackToSavepointCommand;
import com.yekdb.query.command.SavepointCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.executor.QueryExecutionException;
import com.yekdb.query.statement.BeginTransactionStatement;
import com.yekdb.query.statement.CommitTransactionStatement;
import com.yekdb.query.statement.DeleteStatement;
import com.yekdb.query.statement.ExplainStatement;
import com.yekdb.query.statement.InsertStatement;
import com.yekdb.query.statement.ReleaseSavepointStatement;
import com.yekdb.query.statement.RollbackToSavepointStatement;
import com.yekdb.query.statement.RollbackTransactionStatement;
import com.yekdb.query.statement.SavepointStatement;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.UpdateStatement;

import java.util.Objects;

/**
 * Parser tarafından oluşturulan Statement nesnelerini,
 * QueryExecutor tarafından çalıştırılabilen Command
 * nesnelerine dönüştürür.
 *
 * Bu sınıf:
 *
 * - SQL parse etmez
 * - Query çalıştırmaz
 * - Storage erişimi yapmaz
 *
 * Yalnızca:
 *
 * Statement -> Command
 *
 * dönüşümünden sorumludur.
 *
 * Sprint 00-14:
 *
 * SelectStatement artık SelectCommand içerisine
 * kayıpsız olarak aktarılır.
 */
public final class StatementCommandMapper {

    /**
     * Utility sınıfı.
     */
    private StatementCommandMapper() {
    }

    // ==================================================
    // MAP
    // ==================================================

    /**
     * Statement nesnesini uygun Command nesnesine çevirir.
     */
    public static Command map(
            Statement statement
    ) {

        Objects.requireNonNull(
                statement,
                "Statement cannot be null."
        );

        if (statement
                instanceof BeginTransactionStatement beginTransactionStatement) {

            return new BeginTransactionCommand(
                    beginTransactionStatement.getAccessMode(),
                    beginTransactionStatement.getIsolationLevel()
            );
        }

        if (statement
                instanceof CommitTransactionStatement) {

            return new CommitTransactionCommand();
        }

        if (statement
                instanceof RollbackTransactionStatement) {

            return new RollbackTransactionCommand();
        }

        if (statement
                instanceof SavepointStatement savepointStatement) {

            return new SavepointCommand(
                    savepointStatement.getSavepointName()
            );
        }

        if (statement
                instanceof RollbackToSavepointStatement rollbackToSavepointStatement) {

            return new RollbackToSavepointCommand(
                    rollbackToSavepointStatement.getSavepointName()
            );
        }

        if (statement
                instanceof ReleaseSavepointStatement releaseSavepointStatement) {

            return new ReleaseSavepointCommand(
                    releaseSavepointStatement.getSavepointName()
            );
        }

        if (statement
                instanceof InsertStatement insertStatement) {

            return mapInsert(
                    insertStatement
            );
        }

        if (statement
                instanceof ExplainStatement explainStatement) {

            return mapExplain(
                    explainStatement
            );
        }

        if (statement
                instanceof SelectStatement selectStatement) {

            return mapSelect(
                    selectStatement
            );
        }

        if (statement
                instanceof DeleteStatement deleteStatement) {

            return mapDelete(
                    deleteStatement
            );
        }

        if (statement
                instanceof UpdateStatement updateStatement) {

            return mapUpdate(
                    updateStatement
            );
        }

        throw new QueryExecutionException(
                "Unsupported statement type: "
                        + statement
                        .getClass()
                        .getSimpleName()
        );
    }

    // ==================================================
    // INSERT
    // ==================================================

    private static InsertCommand mapInsert(
            InsertStatement statement
    ) {

        return new InsertCommand(
                statement.getTableName(),
                statement.getColumns(),
                statement.getValues()
        );
    }

    // ==================================================
    // EXPLAIN
    // ==================================================

    private static ExplainCommand mapExplain(
            ExplainStatement statement
    ) {

        return new ExplainCommand(
                statement.getSelectStatement(),
                statement.getMode()
        );
    }

    // ==================================================
    // SELECT
    // ==================================================

    /**
     * Sprint 00-14:
     *
     * SelectStatement içerisindeki gelişmiş SELECT
     * bilgileri artık parçalara ayrılarak tekrar
     * oluşturulmaz.
     *
     * Tam statement doğrudan SelectCommand'a taşınır.
     *
     * Korunan bilgiler:
     *
     * - table alias
     * - select item aliases
     * - WHERE
     * - GROUP BY
     * - HAVING
     * - ORDER BY
     * - LIMIT
     * - FETCH
     * - aggregate expressions
     */
    private static SelectCommand mapSelect(
            SelectStatement statement
    ) {

        return new SelectMapper().map(
                statement
        );
    }

    // ==================================================
    // DELETE
    // ==================================================

    private static DeleteCommand mapDelete(
            DeleteStatement statement
    ) {

        return new DeleteMapper().map(
                statement
        );
    }

    // ==================================================
    // UPDATE
    // ==================================================

    private static UpdateCommand mapUpdate(
            UpdateStatement statement
    ) {

        return new UpdateMapper().map(
                statement
        );
    }
}
