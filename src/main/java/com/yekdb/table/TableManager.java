package com.yekdb.table;

import com.yekdb.table.exception.TableAlreadyExistsException;
import com.yekdb.table.exception.TableNotFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB içerisindeki tablo oluşturma, silme ve listeleme
 * işlemlerini yöneten servis sınıfıdır.
 *
 * TableManager:
 * - fiziksel .tbl dosyalarını,
 * - TableCatalog kayıtlarını
 *
 * koordine eder.
 *
 * Tablo ve sütun doğrulamaları ilgili domain sınıflarında
 * gerçekleştirildiği için burada tekrar doğrulama yapılmaz.
 *
 * Sürüm: 1.0
 */
public class TableManager {

    private static final String TABLE_FILE_EXTENSION =
            ".tbl";

    private final Path databaseDirectory;
    private final TableCatalog tableCatalog;

    /**
     * Yeni bir TableManager oluşturur.
     *
     * @param databaseDirectory aktif veritabanı klasörü
     */
    public TableManager(Path databaseDirectory) {
        this(
                databaseDirectory,
                new TableCatalog()
        );
    }

    /**
     * Belirli bir katalog kullanarak TableManager oluşturur.
     *
     * @param databaseDirectory aktif veritabanı klasörü
     * @param tableCatalog      tablo kataloğu
     */
    public TableManager(
            Path databaseDirectory,
            TableCatalog tableCatalog
    ) {

        if (databaseDirectory == null) {
            throw new IllegalArgumentException(
                    "Database directory cannot be null."
            );
        }

        if (tableCatalog == null) {
            throw new IllegalArgumentException(
                    "Table catalog cannot be null."
            );
        }

        this.databaseDirectory =
                databaseDirectory
                        .toAbsolutePath()
                        .normalize();

        this.tableCatalog = tableCatalog;
    }

    /**
     * Yeni tablo oluşturur.
     *
     * İşlem başarılı olduğunda:
     * 1. fiziksel .tbl dosyası oluşturulur,
     * 2. tablo kataloğa kaydedilir.
     *
     * @param table oluşturulacak tablo
     * @return oluşturulan metadata
     */
    public TableMetadata createTable(Table table) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null."
            );
        }

        String tableName =
                table.getTableName();

        Path tableFile =
                resolveTableFile(tableName);

        if (tableCatalog.containsTable(tableName)
                || Files.exists(tableFile)) {

            throw new TableAlreadyExistsException(
                    "Table already exists: "
                            + tableName
            );
        }

        ensureDatabaseDirectoryExists();

        TableMetadata metadata =
                new TableMetadata(
                        tableName,
                        table.getColumnCount()
                );

        try {
            Files.write(
                    tableFile,
                    createInitialTableFileContent(
                            table,
                            metadata
                    ),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            tableCatalog.registerTable(
                    table,
                    metadata
            );

            return metadata;

        } catch (IOException exception) {

            rollbackTableFileCreation(tableFile);

            throw new IllegalStateException(
                    "Table file could not be created: "
                            + tableFile,
                    exception
            );

        } catch (RuntimeException exception) {

            rollbackTableFileCreation(tableFile);

            throw exception;
        }
    }

    /**
     * Tablo adı ve sütun listesinden yeni tablo oluşturur.
     *
     * @param tableName tablo adı
     * @param columns   sütun listesi
     * @return metadata
     */
    public TableMetadata createTable(
            String tableName,
            List<Column> columns
    ) {

        return createTable(
                new Table(
                        tableName,
                        columns
                )
        );
    }

    /**
     * Tabloyu fiziksel dosyası ve katalog kaydıyla birlikte siler.
     *
     * @param tableName tablo adı
     * @return kaldırılan tablo
     */
    public Table dropTable(String tableName) {

        String normalizedName =
                TableNameValidator.validate(tableName);

        Path tableFile =
                resolveTableFile(normalizedName);

        boolean registered =
                tableCatalog.containsTable(normalizedName);

        boolean fileExists =
                Files.exists(tableFile);

        if (!registered && !fileExists) {
            throw new TableNotFoundException(
                    "Table not found: "
                            + normalizedName
            );
        }

        /*
         * Dosya diskte var ancak katalogda yoksa bu,
         * katalog ile fiziksel durumun tutarsız olduğu
         * anlamına gelir.
         *
         * Bu durumda dosyayı silmeden önce hatayı bildiriyoruz.
         */
        if (!registered) {
            throw new TableNotFoundException(
                    "Table exists on disk but is not registered "
                            + "in catalog: "
                            + normalizedName
            );
        }

        try {
            Files.deleteIfExists(tableFile);

            return tableCatalog.unregisterTable(
                    normalizedName
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Table file could not be deleted: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Verilen isimde tablo bulunup bulunmadığını kontrol eder.
     *
     * @param tableName tablo adı
     * @return tablo katalogda veya diskte varsa true
     */
    public boolean exists(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            return false;
        }

        String normalizedName;

        try {
            normalizedName =
                    TableNameValidator.validate(tableName);

        } catch (IllegalArgumentException exception) {
            return false;
        }

        return tableCatalog.containsTable(normalizedName)
                || Files.exists(
                resolveTableFile(normalizedName)
        );
    }

    /**
     * Tabloyu katalogdan döndürür.
     *
     * @param tableName tablo adı
     * @return tablo
     */
    public Table getTable(String tableName) {
        return tableCatalog.getTable(tableName);
    }

    /**
     * Tablo metadata bilgisini döndürür.
     *
     * @param tableName tablo adı
     * @return metadata
     */
    public TableMetadata getMetadata(String tableName) {
        return tableCatalog.getMetadata(tableName);
    }

    /**
     * Katalogdaki tabloları döndürür.
     *
     * @return tablo listesi
     */
    public List<Table> listTables() {
        return tableCatalog.listTables();
    }

    /**
     * Katalogdaki tablo adlarını döndürür.
     *
     * @return tablo adı listesi
     */
    public List<String> listTableNames() {
        return tableCatalog.listTableNames();
    }

    /**
     * Katalogdaki tablo sayısını döndürür.
     *
     * @return tablo sayısı
     */
    public int getTableCount() {
        return tableCatalog.size();
    }

    /**
     * Yönetilen tablo kataloğunu döndürür.
     *
     * @return tablo kataloğu
     */
    public TableCatalog getTableCatalog() {
        return tableCatalog;
    }

    /**
     * Aktif veritabanı dizinini döndürür.
     *
     * @return veritabanı dizini
     */
    public Path getDatabaseDirectory() {
        return databaseDirectory;
    }

    /**
     * Veritabanı klasörü mevcut değilse oluşturur.
     */
    private void ensureDatabaseDirectoryExists() {

        try {
            Files.createDirectories(
                    databaseDirectory
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Database directory could not be created: "
                            + databaseDirectory,
                    exception
            );
        }
    }

    /**
     * Tablo dosyasının güvenli fiziksel yolunu oluşturur.
     *
     * @param tableName tablo adı
     * @return tablo dosyası
     */
    private Path resolveTableFile(String tableName) {

        String normalizedName =
                TableNameValidator.validate(tableName);

        Path tableFile =
                databaseDirectory
                        .resolve(
                                normalizedName
                                        + TABLE_FILE_EXTENSION
                        )
                        .normalize();

        /*
         * Validator zaten path traversal karakterlerine
         * izin vermiyor. Bu kontrol ikinci güvenlik
         * katmanı olarak tutulur.
         */
        if (!tableFile.startsWith(databaseDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid table path: "
                            + tableName
            );
        }

        return tableFile;
    }

    /**
     * Yeni .tbl dosyasının başlangıç içeriğini oluşturur.
     *
     * Şimdilik tablo şeması metinsel olarak yazılır.
     * İleride Storage Engine ile birlikte binary tablo
     * header formatına geçirilebilir.
     *
     * @param table    tablo
     * @param metadata metadata
     * @return fiziksel dosya içeriği
     */
    private byte[] createInitialTableFileContent(
            Table table,
            TableMetadata metadata
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append("YEKDB_TABLE")
                .append(System.lineSeparator());

        builder.append("version=")
                .append(metadata.getVersion())
                .append(System.lineSeparator());

        builder.append("tableName=")
                .append(metadata.getTableName())
                .append(System.lineSeparator());

        builder.append("columnCount=")
                .append(metadata.getColumnCount())
                .append(System.lineSeparator());

        builder.append("createdAt=")
                .append(metadata.getCreatedAt())
                .append(System.lineSeparator());

        builder.append("columns=")
                .append(System.lineSeparator());

        for (Column column : table.getColumns()) {

            builder.append(column.getName())
                    .append(":")
                    .append(column.getDataType())
                    .append(System.lineSeparator());
        }

        return builder
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Tablo oluşturma işlemi başarısız olduğunda
     * oluşturulmuş fiziksel dosyayı temizler.
     *
     * @param tableFile tablo dosyası
     */
    private void rollbackTableFileCreation(
            Path tableFile
    ) {

        try {
            Files.deleteIfExists(tableFile);

        } catch (IOException ignored) {
            /*
             * Asıl işlem hatasının kaybolmaması için
             * rollback hatası burada bastırılır.
             */
        }
    }

    @Override
    public String toString() {
        return "TableManager{" +
                "databaseDirectory=" + databaseDirectory +
                ", tableCount=" + tableCatalog.size() +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                databaseDirectory,
                tableCatalog
        );
    }
}