package com.yekdb.query.datasource;

import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.List;

/**
 * QueryExecutor ile veri saklama katmanı arasındaki bağlantıyı temsil eder.
 *
 * QueryExecutor tablo şemasını ve kayıtları bu arayüz üzerinden alır.
 */
public interface QueryDataSource {

    /**
     * Tablo adına göre tablo şemasını döndürür.
     *
     * @param tableName tablo adı
     * @return tablo şeması
     */
    Table getTable(String tableName);

    /**
     * Tabloya ait aktif satırları döndürür.
     *
     * @param tableName tablo adı
     * @return tablo satırları
     */
    List<Row> getRows(String tableName);
}