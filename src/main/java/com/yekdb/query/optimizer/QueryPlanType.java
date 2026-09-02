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
    INDEX_SCAN,

    /**
     * WHERE koşulunun sabit olarak false olduğu bilindiği için
     * storage taraması yapılmadan boş sonuç döndürülür.
     */
    EMPTY_RESULT
}
