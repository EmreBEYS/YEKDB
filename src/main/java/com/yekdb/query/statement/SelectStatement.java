package com.yekdb.query.statement;

import com.yekdb.query.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SELECT statement modeli.
 *
 * Sprint 00-15:
 *
 * - Table / column alias
 * - INNER JOIN foundation
 * - Multiple JOIN clause support
 * - WHERE
 * - GROUP BY
 * - HAVING
 * - ORDER BY
 * - LIMIT
 * - FETCH
 *
 * Aggregate ifadeleri SelectItem.expression içerisinde
 * COUNT(*), SUM(salary), AVG(age) vb. biçimde tutulabilir.
 *
 * JOIN ifadeleri JoinClause listesi içerisinde tutulur.
 *
 * Örnek:
 *
 * SELECT e.name, d.name
 * FROM employee e
 * INNER JOIN department d
 * ON e.department_id = d.id;
 */
public final class SelectStatement implements Statement {

    private final TableReference table;
    private final List<SelectItem> selectItems;

    /*
     * Sprint 00-15
     *
     * SELECT sorgusuna bağlı JOIN ifadeleri.
     *
     * Liste kullanılmasının sebebi ileride:
     *
     * employee e
     * JOIN department d ...
     * JOIN company c ...
     *
     * gibi çoklu JOIN desteğinin doğrudan
     * eklenebilmesini sağlamaktır.
     */
    private final List<JoinClause> joins;

    private final Expression whereExpression;

    private final GroupByClause groupByClause;
    private final HavingClause havingClause;

    private final List<OrderByItem> orderByItems;

    private final LimitClause limitClause;
    private final FetchClause fetchClause;

    // --------------------------------------------------
    // Sprint 00-15 main constructor
    // --------------------------------------------------

    /**
     * Sprint 00-15 ana constructor.
     *
     * JOIN destekli tam SELECT statement modeli.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems,
            List<JoinClause> joins,
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
                        : new ArrayList<>(selectItems);

        this.joins =
                joins == null
                        ? new ArrayList<>()
                        : new ArrayList<>(joins);

        /*
         * JOIN listesi içerisinde null eleman
         * bulunmasına izin vermiyoruz.
         */
        for (JoinClause join : this.joins) {

            Objects.requireNonNull(
                    join,
                    "join clause cannot be null"
            );
        }

        this.whereExpression =
                whereExpression;

        this.groupByClause =
                groupByClause;

        this.havingClause =
                havingClause;

        this.orderByItems =
                orderByItems == null
                        ? new ArrayList<>()
                        : new ArrayList<>(orderByItems);

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
         * Şimdilik HAVING kullanımını GROUP BY
         * ile sınırlandırıyoruz.
         */
        if (havingClause != null
                && groupByClause == null) {

            throw new IllegalArgumentException(
                    "HAVING requires GROUP BY."
            );
        }
    }

    // --------------------------------------------------
    // Sprint 00-14 compatibility constructor
    // --------------------------------------------------

    /**
     * Sprint 00-14 ana constructor.
     *
     * JOIN içermeyen mevcut kodların ve testlerin
     * geriye dönük uyumluluğunu korur.
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

        this(
                table,
                selectItems,
                List.of(),
                whereExpression,
                groupByClause,
                havingClause,
                orderByItems,
                limitClause,
                fetchClause
        );
    }

    // --------------------------------------------------
    // JOIN convenience constructor
    // --------------------------------------------------

    /**
     * JOIN içeren fakat GROUP BY / HAVING /
     * ORDER BY / LIMIT / FETCH içermeyen
     * temel SELECT sorguları için.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems,
            List<JoinClause> joins,
            Expression whereExpression
    ) {

        this(
                table,
                selectItems,
                joins,
                whereExpression,
                null,
                null,
                List.of(),
                null,
                null
        );
    }

    /**
     * Sadece SELECT + FROM + JOIN kullanan
     * sorgular için.
     */
    public SelectStatement(
            TableReference table,
            List<SelectItem> selectItems,
            List<JoinClause> joins
    ) {

        this(
                table,
                selectItems,
                joins,
                null
        );
    }

    // --------------------------------------------------
    // Existing compatibility constructors
    // --------------------------------------------------

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
                List.of(),
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
                List.of(),
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
                List.of(),
                null,
                null,
                null,
                List.of(),
                null,
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

    // --------------------------------------------------
    // Column conversion
    // --------------------------------------------------

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

    // --------------------------------------------------
    // TABLE
    // --------------------------------------------------

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

    // --------------------------------------------------
    // SELECT ITEMS
    // --------------------------------------------------

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
    // JOIN
    // --------------------------------------------------

    /**
     * SELECT sorgusuna bağlı tüm JOIN ifadelerini
     * değiştirilemez liste olarak döndürür.
     */
    public List<JoinClause> getJoins() {

        return Collections.unmodifiableList(
                joins
        );
    }

    /**
     * Sorguda en az bir JOIN olup olmadığını
     * belirtir.
     */
    public boolean hasJoins() {

        return !joins.isEmpty();
    }

    /**
     * JOIN sayısını döndürür.
     *
     * İleride çoklu JOIN executor işlemlerinde
     * ve testlerde kullanılabilir.
     */
    public int getJoinCount() {

        return joins.size();
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
    // STATEMENT
    // --------------------------------------------------

    @Override
    public StatementType getType() {

        return StatementType.SELECT;
    }

    // --------------------------------------------------
    // DEBUG
    // --------------------------------------------------

    @Override
    public String toString() {

        return "SelectStatement{" +
                "table=" + table +
                ", selectItems=" + selectItems +
                ", joins=" + joins +
                ", whereExpression=" + whereExpression +
                ", groupByClause=" + groupByClause +
                ", havingClause=" + havingClause +
                ", orderByItems=" + orderByItems +
                ", limitClause=" + limitClause +
                ", fetchClause=" + fetchClause +
                '}';
    }
}