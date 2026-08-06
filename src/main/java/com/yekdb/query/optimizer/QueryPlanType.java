package com.yekdb.query.optimizer;

/**
 * Sorgunun hangi yöntemle yürütüleceğini temsil eder.
 */
public enum QueryPlanType {

    /**
     * Tablodaki bütün satırlar sırayla taranır.
     */
    FULL_TABLE_SCAN,

    /**
     * Uygun bir index kullanılarak satırlara erişilir.
     */
    INDEX_SCAN
}