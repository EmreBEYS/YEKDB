package com.yekdb.query.executor;

import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.TableReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Birden fazla JOIN işlemini sıralı şekilde çalıştırır.
 *
 * Örnek:
 *
 * employee e
 * JOIN department d
 *      ON e.department_id = d.id
 * JOIN company c
 *      ON d.company_id = c.id
 *
 * İlk JOIN sonucu ikinci JOIN işleminin sol tarafı
 * olarak kullanılır.
 */

public final class MultiJoinExecutor {
    private final JoinExecutor joinExecutor;

    public MultiJoinExecutor(){
        this(new JoinExecutor());
    }

    public MultiJoinExecutor(JoinExecutor joinExecutor){
        this.joinExecutor=Objects.requireNonNull(joinExecutor,"JoinExecutor cannot be null.");
    }

    /**
     * Birden fazla JOIN clause'u sırayla çalıştırır.
     *
     * @param baseTable       başlangıç tablosu
     * @param baseRows        başlangıç tablosunun satırları
     * @param joinClauses     uygulanacak JOIN listesi
     * @param rightTableRows  her JOIN için sağ tablo satırları
     * @return tüm JOIN işlemleri tamamlandıktan sonraki sonuç
     */
    public List<Map<String, Object>> execute(
            TableReference baseTable,
            List<Map<String, Object>> baseRows,
            List<JoinClause> joinClauses,
            List<List<Map<String, Object>>> rightTableRows
    ) {

        Objects.requireNonNull(
                baseTable,
                "Başlangıç tablo referansı null olamaz."
        );

        Objects.requireNonNull(
                baseRows,
                "Başlangıç satırları null olamaz."
        );

        Objects.requireNonNull(
                joinClauses,
                "JOIN listesi null olamaz."
        );

        Objects.requireNonNull(
                rightTableRows,
                "Sağ tablo satırları null olamaz."
        );

        if (joinClauses.size() != rightTableRows.size()) {
            throw new IllegalArgumentException(
                    "JOIN sayısı ile sağ tablo veri sayısı eşit olmalıdır."
            );
        }

        /*
         * İlk aşamada mevcut satırlar başlangıç
         * tablosunun satırlarıdır.
         */
        List<Map<String, Object>> currentRows =
                new ArrayList<>(baseRows);

        /*
         * İlk JOIN'in sol tablo referansı başlangıç
         * tablosudur.
         */
        TableReference currentLeftTable =
                baseTable;

        for (int i = 0;
             i < joinClauses.size();
             i++) {

            JoinClause joinClause =
                    joinClauses.get(i);

            List<Map<String, Object>> rightRows =
                    rightTableRows.get(i);

            /*
             * Mevcut JOIN'i çalıştırıyoruz.
             */
            currentRows =
                    joinExecutor.execute(
                            currentLeftTable,
                            currentRows,
                            joinClause,
                            rightRows
                    );

            /*
             * İlk JOIN'den sonra currentRows artık tek bir
             * fiziksel tabloya ait değildir.
             *
             * Bu nedenle sonraki JOIN işlemlerinde mevcut
             * qualified kolonları koruyacak özel bir yaklaşım
             * gerekecek.
             *
             * Bu davranışı bir sonraki adımda
             * JoinExecutor ile entegre edeceğiz.
             */
        }

        return currentRows;
    }
}
