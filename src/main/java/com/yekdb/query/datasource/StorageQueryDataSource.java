package com.yekdb.query.datasource;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseManager;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.RowSerializer;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * QueryExecutor ile fiziksel YEKDB storage katmanı
 * arasındaki persistent veri kaynağıdır.
 *
 * SELECT sorguları:
 *
 * - tablo şemasını .tbl dosyasından,
 * - aktif kayıtları .data dosyasından
 *
 * bu sınıf üzerinden okur.
 */
public final class StorageQueryDataSource
        implements QueryDataSource {

    /**
     * Fiziksel tablo kayıt dosyası uzantısı.
     *
     * INSERT / UPDATE / DELETE mutation pipeline'ı
     * ile aynı dosya düzeni kullanılır.
     */
    private static final String DATA_FILE_EXTENSION =
            ".data";

    private final DatabaseManager databaseManager;

    public StorageQueryDataSource(
            DatabaseManager databaseManager
    ) {

        this.databaseManager =
                Objects.requireNonNull(
                        databaseManager,
                        "DatabaseManager cannot be null."
                );
    }

    /**
     * Tablo şemasını aktif veritabanının
     * persistent .tbl dosyasından yükler.
     *
     * Yeni oluşturulan TableManager'ın katalogu
     * boş olduğu için önce loadCatalog() çağrılır.
     *
     * @param tableName tablo adı
     * @return tablo şeması
     */
    @Override
    public Table getTable(
            String tableName
    ) {

        validateTableName(
                tableName
        );

        TableManager tableManager =
                createTableManager();

        /*
         * StorageQueryDataSource kendi TableManager
         * instance'ını oluşturduğu için disk üzerindeki
         * .tbl dosyalarından katalog yeniden yüklenir.
         */
        tableManager.loadCatalog();

        return tableManager.getTable(
                tableName
        );
    }

    /**
     * Tabloya ait aktif fiziksel kayıtları
     * Row nesneleri olarak döndürür.
     *
     * Tombstone ile silinmiş kayıtlar SELECT
     * sonucuna dahil edilmez.
     *
     * @param tableName tablo adı
     * @return aktif satırlar
     */
    @Override
    public List<Row> getRows(
            String tableName
    ) {

        validateTableName(
                tableName
        );

        TableManager tableManager =
                createTableManager();

        /*
         * Tablo şemasının gerçekten mevcut
         * olduğunu doğrulamak için katalog yüklenir.
         */
        tableManager.loadCatalog();

        Table table =
                tableManager.getTable(
                        tableName
                );

        Path dataFile =
                resolveTableDataFile(
                        tableManager,
                        table
                );

        StorageEngine storageEngine =
                new StorageEngine(
                        dataFile
                );

        try {

            /*
             * Mutation pipeline ile aynı storage
             * initialization yolu kullanılır.
             */
            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            /*
             * Logical DELETE uygulanmış kayıtlar
             * SELECT sonucuna girmemelidir.
             */
            List<Record> activeRecords =
                    recordManager
                            .getActiveRecords();

            List<Row> rows =
                    new ArrayList<>(
                            activeRecords.size()
                    );

            for (Record record
                    : activeRecords) {

                Row row =
                        RowSerializer.deserialize(
                                record.getData()
                        );

                rows.add(
                        row
                );
            }

            /*
             * Query execution katmanının datasource
             * tarafından döndürülen listeyi değiştirmesini
             * engellemek için immutable görünüm döndürülür.
             */
            return Collections.unmodifiableList(
                    rows
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to read rows from table: "
                            + table.getTableName(),
                    exception
            );

        } finally {

            shutdownStorageEngine(
                    storageEngine,
                    table
            );
        }
    }

    /**
     * Aktif veritabanı için yeni TableManager oluşturur.
     */
    private TableManager createTableManager() {

        Database database =
                requireCurrentDatabase();

        return new TableManager(
                database.getDatabasePath()
        );
    }

    /**
     * Tabloya ait fiziksel .data dosyasının
     * yolunu üretir.
     *
     * Bu isimlendirme TableMutationExecutionSupport
     * ile birebir aynıdır.
     */
    private Path resolveTableDataFile(
            TableManager tableManager,
            Table table
    ) {

        Objects.requireNonNull(
                tableManager,
                "TableManager cannot be null."
        );

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        return tableManager
                .getDatabaseDirectory()
                .resolve(
                        table
                                .getTableName()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                                + DATA_FILE_EXTENSION
                );
    }

    /**
     * SELECT işleminden önce aktif bir
     * veritabanı bulunmasını zorunlu kılar.
     */
    private Database requireCurrentDatabase() {

        Database database =
                databaseManager
                        .getCurrentDatabase();

        if (database == null) {

            throw new IllegalStateException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        return database;
    }

    /**
     * StorageEngine yaşam döngüsünü güvenli
     * biçimde sonlandırır.
     */
    private void shutdownStorageEngine(
            StorageEngine storageEngine,
            Table table
    ) {

        if (!storageEngine.isInitialized()) {
            return;
        }

        try {

            storageEngine.shutdown();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to close storage engine for table: "
                            + table.getTableName(),
                    exception
            );
        }
    }

    /**
     * Datasource girişindeki tablo adını
     * temel seviyede doğrular.
     */
    private void validateTableName(
            String tableName
    ) {

        if (tableName == null
                || tableName.isBlank()) {

            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}