package com.yekdb.query.statement;

import com.yekdb.query.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SELECT statement modeli.
 *
 * Sprint 00-14:
 *
 * - Table / column alias
 * - WHERE
 * - GROUP BY
 * - HAVING
 * - ORDER BY
 * - LIMIT
 * - FETCH
 *
 * Aggregate ifadeleri SelectItem.expression içerisinde
 * COUNT(*), SUM(salary), AVG(age) vb. biçimde tutulabilir.
 */
public final class SelectStatement implements Statement {

    private final TableReference table;
    private final List<SelectItem> selectItems;

    private final Expression whereExpression;

    private final GroupByClause groupByClause;
    private final HavingClause havingClause;

    private final List<OrderByItem> orderByItems;

    private final LimitClause limitClause;
    private final FetchClause fetchClause;

    /**
     * Sprint 00-14 ana constructor.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems,
            Expression whereExpression,
            GroupByClause groupByClause,
            HavingClause havingClause,
            List<OrderByItem> orderByItems,
            LimitClause limitClause,
            FetchClause fetchClause
    ) {

        this.table =
                Objects.requireNonNull(
                        table,
                        "table cannot be null"
                );

        this.selectItems =
                selectItems == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                        selectItems
                );

        this.whereExpression =
                whereExpression;

        this.groupByClause =
                groupByClause;

        this.havingClause =
                havingClause;

        this.orderByItems =
                orderByItems == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                        orderByItems
                );

        this.limitClause =
                limitClause;

        this.fetchClause =
                fetchClause;

        /*
         * Aynı SELECT içerisinde LIMIT ve FETCH
         * birlikte kullanılmasın.
         */
        if (limitClause != null
                && fetchClause != null) {

            throw new IllegalArgumentException(
                    "LIMIT and FETCH cannot be used together."
            );
        }

        /*
         * HAVING normalde GROUP BY sonucunu filtreler.
         *
         * Sprint 00-14 için HAVING kullanımını
         * GROUP BY ile sınırlandırıyoruz.
         */
        if (havingClause != null
                && groupByClause == null) {

            throw new IllegalArgumentException(
                    "HAVING requires GROUP BY."
            );
        }
    }

    /**
     * ORDER BY entegrasyonunda kullanılan
     * eski Sprint 00-14 constructor.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems,
            Expression whereExpression,
            List<OrderByItem> orderByItems
    ) {

        this(
                table,
                selectItems,
                whereExpression,
                null,
                null,
                orderByItems,
                null,
                null
        );
    }

    /**
     * WHERE destekli eski constructor.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems,
            Expression whereExpression
    ) {

        this(
                table,
                selectItems,
                whereExpression,
                null,
                null,
                List.of(),
                null,
                null
        );
    }

    /**
     * Alias destekli constructor.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems
    ) {

        this(
                table,
                selectItems,
                null
        );
    }

    /**
     * Geriye dönük uyumluluk.
     */
    public SelectStatement(
            String tableName,
            List<String> columns
    ) {

        this(
                tableName,
                columns,
                null
        );
    }

    /**
     * Eski String table API + WHERE.
     */
    public SelectStatement(
            String tableName,
            List<String> columns,
            Expression whereExpression
    ) {

        this(
                new TableReference(
                        tableName
                ),
                convertColumns(
                        columns
                ),
                whereExpression
        );
    }

    private static List<SelectItem> convertColumns(
            List<String> columns
    ) {

        if (columns == null) {

            return Collections.emptyList();
        }

        List<SelectItem> items =
                new ArrayList<>();

        for (String column : columns) {

            items.add(
                    new SelectItem(
                            column
                    )
            );
        }

        return items;
    }

    public TableReference getTable() {

        return table;
    }

    public String getTableName() {

        return table.getTableName();
    }

    public String getTableAlias() {

        return table.getAlias();
    }

    public boolean hasTableAlias() {

        return table.hasAlias();
    }

    public List<SelectItem> getSelectItems() {

        return Collections.unmodifiableList(
                selectItems
        );
    }

    /*
     * Eski executor / mapper uyumluluğu.
     */
    public List<String> getColumns() {

        List<String> columns =
                new ArrayList<>();

        for (SelectItem item : selectItems) {

            columns.add(
                    item.getExpression()
            );
        }

        return Collections.unmodifiableList(
                columns
        );
    }

    /*
     * Eski StatementCommandMapper uyumluluğu.
     */
    public List<String> getSelectedColumns() {

        return getColumns();
    }

    public boolean selectsAllColumns() {

        return selectItems.size() == 1
                && "*".equals(
                selectItems
                        .get(0)
                        .getExpression()
        );
    }

    // --------------------------------------------------
    // WHERE
    // --------------------------------------------------

    public Expression getWhereExpression() {

        return whereExpression;
    }

    public boolean hasWhereClause() {

        return whereExpression != null;
    }

    // --------------------------------------------------
    // GROUP BY
    // --------------------------------------------------

    public GroupByClause getGroupByClause() {

        return groupByClause;
    }

    public boolean hasGroupBy() {

        return groupByClause != null;
    }

    // --------------------------------------------------
    // HAVING
    // --------------------------------------------------

    public HavingClause getHavingClause() {

        return havingClause;
    }

    public boolean hasHaving() {

        return havingClause != null;
    }

    // --------------------------------------------------
    // ORDER BY
    // --------------------------------------------------

    public List<OrderByItem> getOrderByItems() {

        return Collections.unmodifiableList(
                orderByItems
        );
    }

    public boolean hasOrderBy() {

        return !orderByItems.isEmpty();
    }

    // --------------------------------------------------
    // LIMIT
    // --------------------------------------------------

    public LimitClause getLimitClause() {

        return limitClause;
    }

    public boolean hasLimit() {

        return limitClause != null;
    }

    // --------------------------------------------------
    // FETCH
    // --------------------------------------------------

    public FetchClause getFetchClause() {

        return fetchClause;
    }

    public boolean hasFetch() {

        return fetchClause != null;
    }

    // --------------------------------------------------

    @Override
    public StatementType getType() {

        return StatementType.SELECT;
    }

    @Override
    public String toString() {

        return "SelectStatement{" +
                "table=" + table +
                ", selectItems=" + selectItems +
                ", whereExpression=" + whereExpression +
                ", groupByClause=" + groupByClause +
                ", havingClause=" + havingClause +
                ", orderByItems=" + orderByItems +
                ", limitClause=" + limitClause +
                ", fetchClause=" + fetchClause +
                '}';
    }
}