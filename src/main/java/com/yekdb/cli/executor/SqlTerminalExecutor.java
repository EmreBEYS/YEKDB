package com.yekdb.cli.executor;

import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.query.executor.QueryExecutor;

import java.util.Objects;

/**
 * Interactive SQL terminal ile YEKDB query engine
 * arasındaki adaptör katmanıdır.
 *
 * Bu sınıf SQL parse etmez veya doğrudan storage
 * katmanına erişmez.
 *
 * SQL yürütme sorumluluğunu mevcut QueryExecutor
 * katmanına devreder.
 *
 * Akış:
 *
 * InteractiveSqlTerminal
 *      ->
 * SqlTerminalExecutor
 *      ->
 * QueryExecutor
 *      ->
 * Parser / Mapper / Executor / Storage
 */
public final class SqlTerminalExecutor {

    private QueryExecutor queryExecutor;

    /**
     * Geçici boş constructor.
     *
     * Phase 5-B içerisinde TerminalLauncher gerçek
     * QueryExecutor instance'ını sağlayacak.
     *
     * Phase 5-A sırasında mevcut launcher'ın compile
     * almaya devam etmesi için korunmaktadır.
     */
    public SqlTerminalExecutor() {
        this.queryExecutor = null;
    }

    /**
     * Gerçek QueryExecutor bağımlılığı ile
     * terminal executor oluşturur.
     *
     * @param queryExecutor YEKDB query engine
     */
    public SqlTerminalExecutor(
            QueryExecutor queryExecutor
    ) {
        this.queryExecutor =
                Objects.requireNonNull(
                        queryExecutor,
                        "QueryExecutor cannot be null."
                );
    }

    /**
     * QueryExecutor bağımlılığını sonradan bağlar.
     *
     * Phase 5-B launcher entegrasyonunda
     * kullanılabilir.
     *
     * @param queryExecutor YEKDB query engine
     */
    public void setQueryExecutor(
            QueryExecutor queryExecutor
    ) {
        this.queryExecutor =
                Objects.requireNonNull(
                        queryExecutor,
                        "QueryExecutor cannot be null."
                );
    }

    /**
     * SQL statement'ını mevcut YEKDB query
     * pipeline'ına gönderir.
     *
     * @param sql çalıştırılacak SQL
     * @return YEKDB yürütme sonucu
     */
    public ExecuteResult execute(
            String sql
    ) {

        if (sql == null
                || sql.isBlank()) {

            throw new IllegalArgumentException(
                    "SQL statement cannot be null or blank."
            );
        }

        QueryExecutor executor =
                requireQueryExecutor();

        return executor.execute(sql);
    }

    /**
     * QueryExecutor'ın terminal katmanına
     * bağlanmış olup olmadığını döndürür.
     */
    public boolean isReady() {
        return queryExecutor != null;
    }

    /**
     * Bağlı QueryExecutor instance'ını döndürür.
     */
    public QueryExecutor getQueryExecutor() {
        return queryExecutor;
    }

    /**
     * SQL engine henüz bağlanmamışsa anlaşılır
     * bir hata üretir.
     */
    private QueryExecutor requireQueryExecutor() {

        if (queryExecutor == null) {

            throw new IllegalStateException(
                    "QueryExecutor is not configured for the SQL terminal."
            );
        }

        return queryExecutor;
    }
}