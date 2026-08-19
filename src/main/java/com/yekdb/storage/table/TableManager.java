package com.yekdb.storage.table;

import com.yekdb.storage.exception.TableAlreadyExistsException;
import com.yekdb.storage.exception.TableNotFoundException;
import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderConstants;
import com.yekdb.storage.table.header.TableHeaderIO;
import com.yekdb.storage.table.header.TableHeaderSerializer;
import com.yekdb.storage.table.header.TableIdAllocator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB içerisindeki tablo oluşturma, silme, recovery ve listeleme
 * işlemlerini yöneten servis sınıfıdır.
 *
 * <p>TableManager fiziksel {@code .tbl} dosyalarını, 512 byte Binary
 * Table Header yapısını, UTF-8 şema bölümünü ve {@link TableCatalog}
 * kayıtlarını koordine eder.</p>
 *
 * Tablo ve sütun doğrulamaları ilgili domain sınıflarında
 * gerçekleştirildiği için burada tekrar doğrulama yapılmaz.
 *
 * Sürüm: 1.1
 */
public class TableManager {

    private static final String TABLE_FILE_EXTENSION = ".tbl";

    private final TableFileMetadataReader metadataReader;
    private final Path databaseDirectory;
    private final TableCatalog tableCatalog;
    private final TableIdAllocator tableIdAllocator;

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
        this.metadataReader = new TableFileMetadataReader();
        this.tableIdAllocator = new TableIdAllocator();
    }

    /**
     * Veritabanı dizinindeki fiziksel .tbl dosyalarını
     * okuyarak tablo kataloğunu yeniden oluşturur.
     *
     * Bu metod YEKDB yeniden başlatıldığında disk üzerinde
     * bulunan tablo şemalarının bellekteki TableCatalog
     * yapısına geri yüklenmesini sağlar.
     */
    public void loadCatalog() {

        ensureDatabaseDirectoryExists();

        TableCatalog recoveredCatalog =
                new TableCatalog();

        try (var tableFiles =
                     Files.list(databaseDirectory)) {

            tableFiles
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .endsWith(
                                            TABLE_FILE_EXTENSION
                                    )
                    )
                    .sorted()
                    .forEach(path ->
                            recoverTable(
                                    path,
                                    recoveredCatalog
                            )
                    );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Table catalog could not be loaded from: "
                            + databaseDirectory,
                    exception
            );
        }

        /*
         * Tüm dosyalar başarılı şekilde okunduktan sonra
         * gerçek katalog güncellenir.
         */
        tableCatalog.clear();

        for (Table table :
                recoveredCatalog.listTables()) {

            tableCatalog.registerTable(
                    table,
                    recoveredCatalog.getMetadata(
                            table.getTableName()
                    )
            );
        }

        /*
         * Recovery tamamlandıktan sonra yeni oluşturulacak
         * tabloların ID değerinin disk üzerindeki en yüksek
         * tableId değerinden devam etmesini sağlarız.
         */
        synchronizeTableIdAllocator();
    }

    /**
     * Tek bir fiziksel tablo dosyasını okuyarak
     * kataloğa geri yükler.
     *
     * @param tableFile fiziksel tablo dosyası
     */
    private void recoverTable(
            Path tableFile,
            TableCatalog targetCatalog
    ) {

        TableRecoveryEntry recoveryEntry =
                metadataReader.read(tableFile);

        targetCatalog.registerTable(
                recoveryEntry.table(),
                recoveryEntry.metadata()
        );
    }

    /**
     * Yeni tablo oluşturur.
     *
     * İşlem başarılı olduğunda:
     * 1. fiziksel .tbl dosyası oluşturulur,
     * 2. {@link TableHeaderConstants#HEADER_SIZE} byte Binary Table Header yazılır,
     * 3. header sonrasında UTF-8 tablo şeması yazılır,
     * 4. tablo kataloğa kaydedilir.
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
     * Fiziksel dosya düzeni:
     *
     * 0 - HEADER_SIZE-1 : Binary Table Header
     * HEADER_SIZE - ...  : UTF-8 tablo şeması
     *
     * @param table    tablo
     * @param metadata metadata
     * @return fiziksel dosya içeriği
     */
    private byte[] createInitialTableFileContent(
            Table table,
            TableMetadata metadata
    ) {

        byte[] schemaBytes =
                createSchemaContent(
                        table,
                        metadata
                );

        TableHeader header =
                new TableHeader(
                        tableIdAllocator.nextId(),
                        table.getTableName(),
                        table.getColumnCount(),
                        0L,
                        -1L,
                        -1L,
                        TableHeaderConstants.HEADER_SIZE,
                        TableHeaderConstants.FLAG_NONE
                );

        byte[] headerBytes =
                TableHeaderSerializer.serialize(header);

        ByteBuffer fileBuffer =
                ByteBuffer.allocate(
                        headerBytes.length
                                + schemaBytes.length
                );

        fileBuffer.put(headerBytes);
        fileBuffer.put(schemaBytes);

        return fileBuffer.array();
    }

    /**
     * Tablo şemasının fiziksel dosyada saklanacak
     * UTF-8 metinsel temsilini oluşturur.
     *
     * Binary Table Header {@link TableHeaderConstants#HEADER_SIZE}
     * byte uzunluğundadır. Şema bilgisi header'dan hemen sonra başlar.
     *
     * @param table    tablo
     * @param metadata tablo metadata bilgisi
     * @return UTF-8 schema verisi
     */
    private byte[] createSchemaContent(
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
     * Disk üzerindeki tüm .tbl dosyalarının Binary Table Header
     * alanlarını okuyarak TableIdAllocator değerini senkronize eder.
     *
     * Böylece YEKDB yeniden başlatıldığında yeni tablo ID değerleri
     * 1'den başlamaz; disk üzerindeki en yüksek tableId + 1
     * değerinden devam eder.
     */
    private void synchronizeTableIdAllocator() {

        long maxTableId = 0L;

        try (var tableFiles =
                     Files.list(databaseDirectory)) {

            List<Path> files =
                    tableFiles
                            .filter(Files::isRegularFile)
                            .filter(path ->
                                    path.getFileName()
                                            .toString()
                                            .endsWith(
                                                    TABLE_FILE_EXTENSION
                                            )
                            )
                            .toList();

            for (Path tableFile : files) {

                TableHeader header =
                        TableHeaderIO.read(tableFile);

                maxTableId =
                        Math.max(
                                maxTableId,
                                header.getTableId()
                        );
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Table ID allocator could not be synchronized from: "
                            + databaseDirectory,
                    exception
            );
        }

        tableIdAllocator.ensureNextIdAtLeast(
                maxTableId + 1L
        );
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