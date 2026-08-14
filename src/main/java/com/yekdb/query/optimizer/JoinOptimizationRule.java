package com.yekdb.query.optimizer;

/**
 * Sprint 00-16 rule-based JOIN optimizer kurallarını temsil eder.
 *
 * Bu enum yalnızca optimizer tarafından gerçekten uygulanabilen
 * veya güvenli biçimde analiz edilebilen kuralları içerir.
 */
public enum JoinOptimizationRule {

    /**
     * JOIN ON koşullarının geçerli column-to-column
     * karşılaştırmaları olduğunu doğrular.
     */
    CONDITION_VALIDATION,

    /**
     * Koşulsuz / geçersiz JOIN nedeniyle oluşabilecek
     * istemsiz Cartesian Product durumlarını engeller.
     */
    CARTESIAN_PREVENTION,

    /**
     * Yalnızca tek tabloya bağlı WHERE predicate'lerini
     * ilgili tabloya push-down adayı olarak işaretler.
     */
    PREDICATE_PUSHDOWN,

    /**
     * SELECT, WHERE ve JOIN condition tarafından gerçekten
     * ihtiyaç duyulan kolonları hesaplar.
     */
    PROJECTION_PRUNING,

    /**
     * Yalnızca semantik olarak güvenli INNER JOIN
     * zincirlerinde JOIN sırasını değiştirebilir.
     */
    INNER_JOIN_REORDER,

    /**
     * Güvenli reorder mümkün olduğunda daha küçük
     * tablonun önce değerlendirilmesini tercih eder.
     */
    SMALL_TABLE_FIRST
}
