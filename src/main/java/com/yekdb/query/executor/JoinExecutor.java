package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.TableReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * YEKDB JOIN işlemlerinin temel executor sınıfıdır.
 *
 * Sprint 00-15:
 *
 * - INNER JOIN
 * - Nested Loop Join
 * - Table alias desteği
 * - Qualified column üretimi
 * - ON condition evaluation
 *
 * Sprint 00-16:
 *
 * - LEFT JOIN
 * - RIGHT JOIN
 * - FULL JOIN
 * - Eşleşmeyen satırlarda NULL üretimi
 * - Multiple JOIN için qualified kolon koruması
 *
 * Örnek:
 *
 * employee e
 * INNER JOIN department d
 * ON e.department_id = d.id
 */
public final class JoinExecutor {

    private final ExpressionEvaluator expressionEvaluator;

    public JoinExecutor() {
        this(new ExpressionEvaluator());
    }

    public JoinExecutor(
            ExpressionEvaluator expressionEvaluator
    ) {

        this.expressionEvaluator =
                Objects.requireNonNull(
                        expressionEvaluator,
                        "ExpressionEvaluator null olamaz."
                );
    }

    // --------------------------------------------------
    // PUBLIC API
    // --------------------------------------------------

    /**
     * Tek bir JOIN clause çalıştırır.
     *
     * @param leftTable  sol tablo referansı
     * @param leftRows   sol tablo satırları
     * @param joinClause JOIN bilgisi
     * @param rightRows  sağ tablo satırları
     * @return JOIN sonucu
     */
    public List<Map<String, Object>> execute(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        Objects.requireNonNull(
                leftTable,
                "Sol tablo null olamaz."
        );

        Objects.requireNonNull(
                leftRows,
                "Sol tablo satırları null olamaz."
        );

        Objects.requireNonNull(
                joinClause,
                "JOIN bilgisi null olamaz."
        );

        Objects.requireNonNull(
                rightRows,
                "Sağ tablo satırları null olamaz."
        );

        return switch (joinClause.getJoinType()) {

            case INNER ->
                    executeInnerJoin(
                            leftTable,
                            leftRows,
                            joinClause,
                            rightRows
                    );

            case LEFT ->
                    executeLeftJoin(
                            leftTable,
                            leftRows,
                            joinClause,
                            rightRows
                    );

            case RIGHT ->
                    executeRightJoin(
                            leftTable,
                            leftRows,
                            joinClause,
                            rightRows
                    );

            case FULL ->
                    executeFullJoin(
                            leftTable,
                            leftRows,
                            joinClause,
                            rightRows
                    );
        };
    }

    // --------------------------------------------------
    // INNER JOIN
    // --------------------------------------------------

    /**
     * Nested Loop INNER JOIN uygular.
     *
     * Yalnızca ON koşulunu sağlayan satırlar
     * JOIN sonucuna eklenir.
     */
    private List<Map<String, Object>> executeInnerJoin(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        TableReference rightTable =
                createRightTableReference(
                        joinClause
                );

        for (Map<String, Object> leftRow
                : leftRows) {

            for (Map<String, Object> rightRow
                    : rightRows) {

                Map<String, Object> joinedRow =
                        mergeRows(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRow
                        );

                boolean matches =
                        expressionEvaluator.evaluate(
                                joinClause.getCondition(),
                                joinedRow
                        );

                if (matches) {
                    result.add(joinedRow);
                }
            }
        }

        return result;
    }

    // --------------------------------------------------
    // LEFT JOIN
    // --------------------------------------------------

    /**
     * Nested Loop LEFT JOIN uygular.
     *
     * Sol tablodaki bütün satırlar korunur.
     *
     * Eğer sağ tabloda ON koşulunu sağlayan bir satır
     * bulunursa eşleşen JOIN sonucu eklenir.
     *
     * Eğer hiçbir sağ satır eşleşmezse sol satır korunur
     * ve sağ tablo kolonları NULL değerlerle oluşturulur.
     */
    private List<Map<String, Object>> executeLeftJoin(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        TableReference rightTable =
                createRightTableReference(
                        joinClause
                );

        for (Map<String, Object> leftRow
                : leftRows) {

            /*
             * Mevcut sol satırın sağ tabloda en az
             * bir eşleşme bulup bulmadığını tutar.
             */
            boolean matched = false;

            for (Map<String, Object> rightRow
                    : rightRows) {

                Map<String, Object> joinedRow =
                        mergeRows(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRow
                        );

                boolean matches =
                        expressionEvaluator.evaluate(
                                joinClause.getCondition(),
                                joinedRow
                        );

                if (matches) {

                    result.add(joinedRow);

                    matched = true;
                }
            }

            /*
             * Sağ tarafta hiçbir eşleşme bulunamadıysa
             * LEFT JOIN gereği sol satır korunur.
             */
            if (!matched) {

                Map<String, Object> joinedRow =
                        createLeftUnmatchedRow(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRows
                        );

                result.add(joinedRow);
            }
        }

        return result;
    }

    // --------------------------------------------------
    // RIGHT JOIN
    // --------------------------------------------------

    /**
     * Nested Loop RIGHT JOIN uygular.
     *
     * Sağ tablodaki bütün satırlar korunur.
     *
     * Eğer sol tabloda ON koşulunu sağlayan bir satır
     * bulunursa eşleşen JOIN sonucu eklenir.
     *
     * Eğer hiçbir sol satır eşleşmezse sağ satır korunur
     * ve sol tablo kolonları NULL değerlerle oluşturulur.
     */
    private List<Map<String, Object>> executeRightJoin(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        TableReference rightTable =
                createRightTableReference(
                        joinClause
                );

        /*
         * RIGHT JOIN'de dış döngü sağ tablo üzerinden
         * ilerler.
         *
         * Böylece sağ taraftaki bütün satırların
         * korunması garanti edilir.
         */
        for (Map<String, Object> rightRow
                : rightRows) {

            boolean matched = false;

            for (Map<String, Object> leftRow
                    : leftRows) {

                Map<String, Object> joinedRow =
                        mergeRows(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRow
                        );

                boolean matches =
                        expressionEvaluator.evaluate(
                                joinClause.getCondition(),
                                joinedRow
                        );

                if (matches) {

                    result.add(joinedRow);

                    matched = true;
                }
            }

            /*
             * Sol tarafta hiçbir eşleşme bulunamadıysa
             * RIGHT JOIN gereği sağ satır korunur.
             */
            if (!matched) {

                Map<String, Object> joinedRow =
                        createRightUnmatchedRow(
                                leftTable,
                                leftRows,
                                rightTable,
                                rightRow
                        );

                result.add(joinedRow);
            }
        }

        return result;
    }

    // --------------------------------------------------
    // FULL JOIN
    // --------------------------------------------------

    /**
     * Nested Loop FULL JOIN uygular.
     *
     * Her iki tablodaki bütün satırlar korunur.
     *
     * Eşleşen satırlar normal şekilde birleştirilir.
     *
     * Sol tarafta eşleşmeyen satırlar için
     * sağ tablo kolonları NULL olur.
     *
     * Sağ tarafta eşleşmeyen satırlar için
     * sol tablo kolonları NULL olur.
     */
    private List<Map<String, Object>> executeFullJoin(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            JoinClause joinClause,
            List<Map<String, Object>> rightRows
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        TableReference rightTable =
                createRightTableReference(
                        joinClause
                );

        /*
         * Sağ tablodaki hangi satırların eşleştiğini
         * indeks bazında takip eder.
         *
         * Bu yapı eşleşmiş sağ satırların işlem sonunda
         * tekrar eklenmesini engeller.
         */
        boolean[] rightMatched =
                new boolean[rightRows.size()];

        for (Map<String, Object> leftRow
                : leftRows) {

            boolean leftMatched = false;

            for (int rightIndex = 0;
                 rightIndex < rightRows.size();
                 rightIndex++) {

                Map<String, Object> rightRow =
                        rightRows.get(rightIndex);

                Map<String, Object> joinedRow =
                        mergeRows(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRow
                        );

                boolean matches =
                        expressionEvaluator.evaluate(
                                joinClause.getCondition(),
                                joinedRow
                        );

                if (matches) {

                    result.add(joinedRow);

                    leftMatched = true;
                    rightMatched[rightIndex] = true;
                }
            }

            /*
             * Mevcut sol satır hiçbir sağ satırla
             * eşleşmediyse FULL JOIN gereği korunur.
             */
            if (!leftMatched) {

                Map<String, Object> joinedRow =
                        createLeftUnmatchedRow(
                                leftTable,
                                leftRow,
                                rightTable,
                                rightRows
                        );

                result.add(joinedRow);
            }
        }

        /*
         * Sol taraf tamamlandıktan sonra hiçbir sol
         * satırla eşleşmemiş sağ satırlar eklenir.
         */
        for (int rightIndex = 0;
             rightIndex < rightRows.size();
             rightIndex++) {

            if (!rightMatched[rightIndex]) {

                Map<String, Object> joinedRow =
                        createRightUnmatchedRow(
                                leftTable,
                                leftRows,
                                rightTable,
                                rightRows.get(rightIndex)
                        );

                result.add(joinedRow);
            }
        }

        return result;
    }

    // --------------------------------------------------
    // EŞLEŞMEYEN SATIRLAR
    // --------------------------------------------------

    /**
     * LEFT JOIN veya FULL JOIN sırasında sağ tarafta
     * eşleşme bulunmayan bir sol satır oluşturur.
     *
     * Sol tablo değerleri korunur.
     * Sağ tablo kolonları NULL olarak eklenir.
     */
    private Map<String, Object> createLeftUnmatchedRow(
            TableReference leftTable,
            Map<String, Object> leftRow,
            TableReference rightTable,
            List<Map<String, Object>> rightRows
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        /*
         * Sol tablo değerleri normal şekilde eklenir.
         */
        addQualifiedColumns(
                joinedRow,
                leftTable,
                leftRow
        );

        /*
         * Sağ tabloda en az bir satır varsa kolon yapısı
         * ilk satır üzerinden belirlenir.
         *
         * İleride bu bilgi TableMetadata üzerinden
         * alınacaktır.
         */
        if (!rightRows.isEmpty()) {

            Map<String, Object> nullRightRow =
                    new LinkedHashMap<>();

            for (String columnName
                    : rightRows.get(0).keySet()) {

                nullRightRow.put(
                        normalizeSourceColumnName(
                                columnName
                        ),
                        null
                );
            }

            addQualifiedColumns(
                    joinedRow,
                    rightTable,
                    nullRightRow
            );
        }

        return joinedRow;
    }

    /**
     * RIGHT JOIN veya FULL JOIN sırasında sol tarafta
     * eşleşme bulunmayan bir sağ satır oluşturur.
     *
     * Sağ tablo değerleri korunur.
     * Sol tablo kolonları NULL olarak eklenir.
     */
    private Map<String, Object> createRightUnmatchedRow(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            TableReference rightTable,
            Map<String, Object> rightRow
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        /*
         * Sol tabloda en az bir satır varsa kolon yapısı
         * ilk satır üzerinden belirlenir.
         *
         * İleride bu bilgi TableMetadata üzerinden
         * alınacaktır.
         */
        if (!leftRows.isEmpty()) {

            Map<String, Object> nullLeftRow =
                    new LinkedHashMap<>();

            for (String columnName
                    : leftRows.get(0).keySet()) {

                /*
                 * Kaynak satır bir önceki JOIN sonucundan
                 * gelmiş olabilir.
                 *
                 * Qualified kolon adı varsa aynen korunur.
                 */
                if (isQualifiedColumnName(columnName)) {

                    nullLeftRow.put(
                            columnName,
                            null
                    );

                } else {

                    nullLeftRow.put(
                            normalizeSourceColumnName(
                                    columnName
                            ),
                            null
                    );
                }
            }

            addQualifiedColumns(
                    joinedRow,
                    leftTable,
                    nullLeftRow
            );
        }

        /*
         * Sağ tablo değerleri normal şekilde eklenir.
         */
        addQualifiedColumns(
                joinedRow,
                rightTable,
                rightRow
        );

        return joinedRow;
    }

    // --------------------------------------------------
    // ROW MERGE
    // --------------------------------------------------

    /**
     * Sol ve sağ satırı JOIN evaluator tarafından
     * kullanılabilecek qualified kolon isimleriyle
     * birleştirir.
     *
     * Örnek:
     *
     * employee e:
     *
     * id = 1
     * name = Yunus
     * department_id = 10
     *
     * department d:
     *
     * id = 10
     * name = IT
     *
     * Sonuç:
     *
     * employee.id = 1
     * e.id = 1
     *
     * employee.name = Yunus
     * e.name = Yunus
     *
     * department.id = 10
     * d.id = 10
     */
    private Map<String, Object> mergeRows(
            TableReference leftTable,
            Map<String, Object> leftRow,
            TableReference rightTable,
            Map<String, Object> rightRow
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        addQualifiedColumns(
                joinedRow,
                leftTable,
                leftRow
        );

        addQualifiedColumns(
                joinedRow,
                rightTable,
                rightRow
        );

        return joinedRow;
    }

    // --------------------------------------------------
    // QUALIFIED COLUMNS
    // --------------------------------------------------

    /**
     * Bir satırdaki kolonları JOIN sonucuna ekler.
     *
     * Normal fiziksel tablo satırlarında:
     *
     * table.column
     *
     * ve alias varsa:
     *
     * alias.column
     *
     * biçiminde qualified kolonlar oluşturulur.
     *
     * Eğer kaynak satır daha önceki bir JOIN işleminden
     * geliyorsa ve kolon zaten qualified ise mevcut
     * kolon adı aynen korunur.
     */
    private void addQualifiedColumns(
            Map<String, Object> target,
            TableReference table,
            Map<String, Object> row
    ) {

        Objects.requireNonNull(
                target,
                "Hedef satır null olamaz."
        );

        Objects.requireNonNull(
                table,
                "Tablo referansı null olamaz."
        );

        Objects.requireNonNull(
                row,
                "Kaynak satır null olamaz."
        );

        String tableName =
                table.getTableName();

        String alias =
                table.getAlias();

        for (Map.Entry<String, Object> entry
                : row.entrySet()) {

            String sourceColumnName =
                    entry.getKey();

            Object value =
                    entry.getValue();

            /*
             * Kaynak kolon zaten qualified ise önceki
             * JOIN sonucundan gelmiş olabilir.
             *
             * Örnek:
             *
             * e.id
             * d.id
             * d.company_id
             *
             * Bu durumda qualifier kesinlikle
             * kaybedilmemelidir.
             */
            if (isQualifiedColumnName(
                    sourceColumnName
            )) {

                target.put(
                        sourceColumnName,
                        value
                );

                continue;
            }

            /*
             * Fiziksel tablo satırından gelen normal
             * kolon adı düzenlenir.
             */
            String columnName =
                    normalizeSourceColumnName(
                            sourceColumnName
                    );

            /*
             * Gerçek tablo adı.
             *
             * employee.id
             */
            target.put(
                    tableName
                            + "."
                            + columnName,
                    value
            );

            /*
             * Alias mevcutsa ayrıca alias.column
             * biçimi oluşturulur.
             *
             * e.id
             */
            if (table.hasAlias()) {

                target.put(
                        alias
                                + "."
                                + columnName,
                        value
                );
            }
        }
    }

    /**
     * Kaynak satır zaten qualified key içeriyorsa
     * gerçek kolon adını ayırır.
     *
     * Örnek:
     *
     * e.id
     *
     * ->
     *
     * id
     */
    private String normalizeSourceColumnName(
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "Kolon adı null olamaz."
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "Kolon adı boş olamaz."
            );
        }

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0
                && dotIndex < normalized.length() - 1) {

            return normalized.substring(
                    dotIndex + 1
            );
        }

        return normalized;
    }

    /**
     * Kolon adının qualified olup olmadığını kontrol eder.
     *
     * Örnek:
     *
     * e.id             -> true
     * employee.id      -> true
     * d.company_id     -> true
     *
     * id               -> false
     * name             -> false
     */
    private boolean isQualifiedColumnName(
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "Kolon adı null olamaz."
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "Kolon adı boş olamaz."
            );
        }

        int dotIndex =
                normalized.indexOf('.');

        return dotIndex > 0
                && dotIndex < normalized.length() - 1;
    }

    // --------------------------------------------------
    // RIGHT TABLE
    // --------------------------------------------------

    /**
     * JoinClause bilgisinden sağ tablo referansı üretir.
     */
    private TableReference createRightTableReference(
            JoinClause joinClause
    ) {

        return new TableReference(
                joinClause.getTableName(),
                joinClause.getAlias()
        );
    }
}