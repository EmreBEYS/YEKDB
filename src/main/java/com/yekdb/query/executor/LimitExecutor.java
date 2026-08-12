package com.yekdb.query.executor;

import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.storage.record.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SELECT sonuçlarına LIMIT ve FETCH uygular.
 *
 * Sprint 00-14
 *
 * LIMIT ve FETCH execution seviyesinde aynı
 * davranışı gerçekleştirir:
 *
 * Sonuç listesinden ilk N satırı döndürür.
 */
public final class LimitExecutor {

    /**
     * LIMIT uygular.
     */
    public List<Row> execute(
            List<Row> rows,
            LimitClause limitClause
    ) {

        Objects.requireNonNull(
                limitClause,
                "LimitClause cannot be null."
        );

        return applyLimit(
                rows,
                limitClause.getRowCount()
        );
    }

    /**
     * FETCH uygular.
     */
    public List<Row> execute(
            List<Row> rows,
            FetchClause fetchClause
    ) {

        Objects.requireNonNull(
                fetchClause,
                "FetchClause cannot be null."
        );

        return applyLimit(
                rows,
                fetchClause.getRowCount()
        );
    }

    /**
     * Ortak LIMIT / FETCH algoritması.
     */
    private List<Row> applyLimit(
            List<Row> rows,
            int rowCount
    ) {

        Objects.requireNonNull(
                rows,
                "Rows cannot be null."
        );

        if (rowCount < 0) {

            throw new IllegalArgumentException(
                    "Row count cannot be negative."
            );
        }

        /*
         * LIMIT 0
         *
         * FETCH FIRST 0 ROWS ONLY
         */
        if (rowCount == 0) {

            return List.of();
        }

        /*
         * İstenen limit mevcut satır
         * sayısına eşit veya daha büyükse
         * tüm satırların yeni bir kopyasını döndür.
         */
        if (rowCount >= rows.size()) {

            return new ArrayList<>(
                    rows
            );
        }

        /*
         * İlk N satır.
         */
        return new ArrayList<>(
                rows.subList(
                        0,
                        rowCount
                )
        );
    }
}