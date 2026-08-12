package com.yekdb.query.mapper;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.UpdateCommand;
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
                instanceof InsertStatement insertStatement) {

            return mapInsert(
                    insertStatement
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