package com.yekdb.query.command;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.TableReference;

import java.util.List;
import java.util.Objects;

/**
 * SELECT SQL komutunu execution katmanında temsil eder.
 *
 * Sprint 00-14 final tasarımı:
 *
 * SelectCommand artık gelişmiş SelectStatement modelini
 * doğrudan taşır.
 *
 * Böylece aşağıdaki bilgiler Statement -> Command
 * geçişinde kaybolmaz:
 *
 * - SELECT items
 * - Column alias
 * - Table alias
 * - WHERE
 * - GROUP BY
 * - HAVING
 * - ORDER BY
 * - LIMIT
 * - FETCH
 * - Aggregate expressions
 *
 * Eski factory metodları geriye dönük uyumluluk için
 * korunmuştur.
 */
public final class SelectCommand implements Command {

    /**
     * Parser tarafından oluşturulmuş tam SELECT modeli.
     */
    private final SelectStatement statement;

    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    /**
     * Tam SelectStatement üzerinden command oluşturur.
     */
    private SelectCommand(
            SelectStatement statement
    ) {

        this.statement =
                Objects.requireNonNull(
                        statement,
                        "SelectStatement cannot be null."
                );
    }

    // ==================================================
    // SPRINT 00-14 FACTORY
    // ==================================================

    /**
     * Parser / Mapper tarafından oluşturulan gelişmiş
     * SelectStatement'ı kayıpsız şekilde command'a taşır.
     */
    public static SelectCommand fromStatement(
            SelectStatement statement
    ) {

        return new SelectCommand(
                statement
        );
    }

    // ==================================================
    // BACKWARD COMPATIBILITY
    // ==================================================

    /**
     * SELECT * FROM table
     */
    public static SelectCommand allFrom(
            String tableName
    ) {

        return new SelectCommand(
                new SelectStatement(
                        tableName,
                        List.of("*")
                )
        );
    }

    /**
     * SELECT * FROM table WHERE ...
     */
    public static SelectCommand allFromWhere(
            String tableName,
            Expression whereExpression
    ) {

        Objects.requireNonNull(
                whereExpression,
                "WHERE expression cannot be null."
        );

        return new SelectCommand(
                new SelectStatement(
                        tableName,
                        List.of("*"),
                        whereExpression
                )
        );
    }

    /**
     * SELECT column1, column2 FROM table
     */
    public static SelectCommand columnsFrom(
            String tableName,
            List<String> selectedColumns
    ) {

        Objects.requireNonNull(
                selectedColumns,
                "Selected column list cannot be null."
        );

        return new SelectCommand(
                new SelectStatement(
                        tableName,
                        selectedColumns
                )
        );
    }

    /**
     * SELECT column1, column2
     * FROM table
     * WHERE ...
     */
    public static SelectCommand columnsFromWhere(
            String tableName,
            List<String> selectedColumns,
            Expression whereExpression
    ) {

        Objects.requireNonNull(
                selectedColumns,
                "Selected column list cannot be null."
        );

        Objects.requireNonNull(
                whereExpression,
                "WHERE expression cannot be null."
        );

        return new SelectCommand(
                new SelectStatement(
                        tableName,
                        selectedColumns,
                        whereExpression
                )
        );
    }

    // ==================================================
    // FULL STATEMENT
    // ==================================================

    /**
     * Tam SELECT statement modelini döndürür.
     *
     * QueryExecutor Sprint 00-14 execution pipeline'ı
     * bu modeli SelectExecutor.executeStatement(...)
     * metoduna gönderir.
     */
    public SelectStatement getStatement() {
        return statement;
    }

    // ==================================================
    // COMPATIBILITY GETTERS
    // ==================================================

    public String getTableName() {
        return statement.getTableName();
    }

    public String getTableAlias() {
        return statement.getTableAlias();
    }

    public boolean hasTableAlias() {
        return statement.hasTableAlias();
    }

    public TableReference getTable() {
        return statement.getTable();
    }

    public List<SelectItem> getSelectItems() {
        return statement.getSelectItems();
    }

    public List<String> getSelectedColumns() {
        return statement.getSelectedColumns();
    }

    public boolean isSelectAll() {
        return statement.selectsAllColumns();
    }

    public boolean selectsAllColumns() {
        return statement.selectsAllColumns();
    }

    // ==================================================
    // WHERE
    // ==================================================

    public Expression getWhereExpression() {
        return statement.getWhereExpression();
    }

    public boolean hasWhereExpression() {
        return statement.hasWhereClause();
    }

    // ==================================================
    // GROUP BY
    // ==================================================

    public GroupByClause getGroupByClause() {
        return statement.getGroupByClause();
    }

    public boolean hasGroupBy() {
        return statement.hasGroupBy();
    }

    // ==================================================
    // HAVING
    // ==================================================

    public HavingClause getHavingClause() {
        return statement.getHavingClause();
    }

    public boolean hasHaving() {
        return statement.hasHaving();
    }

    // ==================================================
    // ORDER BY
    // ==================================================

    public List<OrderByItem> getOrderByItems() {
        return statement.getOrderByItems();
    }

    public boolean hasOrderBy() {
        return statement.hasOrderBy();
    }

    // ==================================================
    // LIMIT
    // ==================================================

    public LimitClause getLimitClause() {
        return statement.getLimitClause();
    }

    public boolean hasLimit() {
        return statement.hasLimit();
    }

    // ==================================================
    // FETCH
    // ==================================================

    public FetchClause getFetchClause() {
        return statement.getFetchClause();
    }

    public boolean hasFetch() {
        return statement.hasFetch();
    }

    // ==================================================
    // TO STRING
    // ==================================================

    @Override
    public String toString() {

        return "SelectCommand{" +
                "statement=" + statement +
                '}';
    }
}