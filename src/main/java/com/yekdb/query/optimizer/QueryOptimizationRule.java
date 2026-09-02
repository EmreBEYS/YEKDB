package com.yekdb.query.optimizer;

/**
 * Query Optimization V2 pipeline'ında uygulanabilen kuralları temsil eder.
 *
 * Phase 1 kapsamında kurallar yalnızca plan metadata'sı olarak taşınır.
 * Davranış değiştiren expression rewrite kuralları ilerleyen phase'lerde
 * aktif hale getirilecektir.
 */
public enum QueryOptimizationRule {

    /**
     * WHERE expression ağacı optimizer pipeline'ından geçirilmiştir.
     */
    EXPRESSION_OPTIMIZATION,

    /**
     * Sabit predicate'ler TRUE/FALSE sonucuna indirgenmiştir.
     */
    CONSTANT_PREDICATE_SIMPLIFICATION,

    /**
     * Uygun B+ Tree index erişim yolu seçilmiştir.
     */
    INDEX_SELECTION
}
