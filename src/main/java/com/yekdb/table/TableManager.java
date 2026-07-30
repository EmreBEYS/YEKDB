package com.yekdb.table;

import com.yekdb.table.exception.DuplicateColumnException;
import com.yekdb.table.exception.InvalidColumnException;
import com.yekdb.table.exception.TableAlreadyExistsException;
import com.yekdb.table.exception.TableNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * YEKDB içerisindeki tablo oluşturma, silme ve listeleme
 * işlemlerini yöneten servis sınıfıdır.
 *
 * TableManager hem fiziksel .tbl dosyalarını hem de
 * bellekte bulunan TableCatalog nesnesini yönetir.
 *
 * Sürüm: 1.0
 */
public class TableManager {

    private static final String TABLE_FILE_EXTENSION = ".tbl";

    private final Path databaseDirectory;
    private final TableCatalog tableCatalog;

    /**
     * Yeni bir TableManager oluşturur.
     *
     * @param databaseDirectory aktif veritabanının klasör yolu
     */
    public TableManager(Path databaseDirectory) {
        this(databaseDirectory, new TableCatalog());
    }

    /**
     * Belirli bir katalog ile yeni TableManager oluşturur.
     *
     * Bu constructor özellikle testlerde mevcut bir katalog
     * nesnesi kullanmak için faydalıdır.
     *
     * @param databaseDirectory aktif veritabanının klasör yolu
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
                databaseDirectory.toAbsolutePath().normalize();

        this.tableCatalog = tableCatalog;
    }

    /**
     * Yeni bir tablo oluşturur.
     *
     * İşlem başarılı olursa fiziksel .tbl dosyası oluşturulur
     * ve tablo kataloğa kaydedilir.
     *
     * @param table oluşturulacak tablo
     * @return oluşturulan tablo metadata bilgisi
     * @throws IllegalArgumentException    tablo null ise
     * @throws TableAlreadyExistsException tablo zaten varsa
     * @throws DuplicateColumnException    aynı isimde sütun varsa
     * @throws InvalidColumnException      geçersiz sütun varsa
     * @throws IllegalStateException       dosya işlemi başarısız olursa
     */
    public TableMetadata createTable(Table table) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null."
            );
        }

        validateTable(table);

        String tableName = normalizeTableName(table.getTableName());
        Path tableFile = resolveTableFile(tableName);

        if (tableCatalog.containsTable(tableName)
                || Files.exists(tableFile)) {

            throw new TableAlreadyExistsException(
                    "Table already exists: " + tableName
            );
        }

        ensureDatabaseDirectoryExists();

        TableMetadata metadata = new TableMetadata(
                tableName,
                table.getColumnCount()
        );

        try {
            Files.write(
                    tableFile,
                    createInitialTableFileContent(table, metadata),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            tableCatalog.registerTable(table, metadata);

            return metadata;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Table file could not be created: " + tableFile,
                    exception
            );
        } catch (RuntimeException exception) {
            rollbackTableFileCreation(tableFile);
            throw exception;
        }
    }

    /**
     * Yeni bir tablo oluşturur.
     *
     * Bu yardımcı metot tablo adı ve sütun listesinden
     * otomatik olarak Table nesnesi oluşturur.
     *
     * @param tableName tablo adı
     * @param columns   tablo sütunları
     * @return oluşturulan metadata
     */
    public TableMetadata createTable(
            String tableName,
            List<Column> columns
    ) {
        return createTable(new Table(tableName, columns));
    }

    /**
     * Bir tabloyu siler.
     *
     * Fiziksel .tbl dosyasını ve katalog kaydını kaldırır.
     *
     * @param tableName silinecek tablo adı
     * @return silinen tablo
     * @throws TableNotFoundException tablo bulunamazsa
     * @throws IllegalStateException  dosya silinemezse
     */
    public Table dropTable(String tableName) {

        String normalizedName = normalizeTableName(tableName);
        Path tableFile = resolveTableFile(normalizedName);

        if (!tableCatalog.containsTable(normalizedName)
                && Files.notExists(tableFile)) {

            throw new TableNotFoundException(
                    "Table not found: " + normalizedName
            );
        }

        try {
            boolean fileDeleted = Files.deleteIfExists(tableFile);

            if (!fileDeleted && Files.exists(tableFile)) {
                throw new IllegalStateException(
                        "Table file could not be deleted: " + tableFile
                );
            }

            if (tableCatalog.containsTable(normalizedName)) {
                return tableCatalog.unregisterTable(normalizedName);
            }

            throw new TableNotFoundException(
                    "Table exists on disk but is not registered " +
                            "in catalog: " + normalizedName
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Table file could not be deleted: " + tableFile,
                    exception
            );
        }
    }

    /**
     * Verilen isimde tablo olup olmadığını kontrol eder.
     *
     * @param tableName tablo adı
     * @return tablo katalogda veya diskte varsa true
     */
    public boolean exists(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            return false;
        }

        String normalizedName = normalizeTableName(tableName);

        return tableCatalog.containsTable(normalizedName)
                || Files.exists(resolveTableFile(normalizedName));
    }

    /**
     * Verilen tabloyu katalogdan döndürür.
     *
     * @param tableName tablo adı
     * @return tablo
     */
    public Table getTable(String tableName) {
        return tableCatalog.getTable(tableName);
    }

    /**
     * Verilen tabloya ait metadata bilgisini döndürür.
     *
     * @param tableName tablo adı
     * @return metadata
     */
    public TableMetadata getMetadata(String tableName) {
        return tableCatalog.getMetadata(tableName);
    }

    /**
     * Kayıtlı tabloları döndürür.
     *
     * @return değiştirilemez tablo listesi
     */
    public List<Table> listTables() {
        return tableCatalog.listTables();
    }

    /**
     * Kayıtlı tablo adlarını döndürür.
     *
     * @return değiştirilemez tablo adı listesi
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
     * Aktif veritabanı klasörünü döndürür.
     *
     * @return veritabanı klasörü
     */
    public Path getDatabaseDirectory() {
        return databaseDirectory;
    }

    /**
     * Tablo ve sütun tanımlarını doğrular.
     *
     * @param table doğrulanacak tablo
     */
    private void validateTable(Table table) {

        List<Column> columns = table.getColumns();

        if (columns.isEmpty()) {
            throw new InvalidColumnException(
                    "Table must contain at least one column."
            );
        }

        for (Column column : columns) {
            validateColumn(column);
        }

        long distinctColumnCount = columns.stream()
                .map(Column::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        if (distinctColumnCount != columns.size()) {
            throw new DuplicateColumnException(
                    "Duplicate column names are not allowed " +
                            "in table: " + table.getTableName()
            );
        }
    }

    /**
     * Tek bir sütunun geçerli olup olmadığını kontrol eder.
     *
     * @param column doğrulanacak sütun
     */
    private void validateColumn(Column column) {

        if (column == null) {
            throw new InvalidColumnException(
                    "Column cannot be null."
            );
        }

        String columnName = column.getName();

        if (columnName == null || columnName.isBlank()) {
            throw new InvalidColumnException(
                    "Column name cannot be null or blank."
            );
        }

        if (!columnName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new InvalidColumnException(
                    "Invalid column name: " + columnName
            );
        }

        if (column.getDataType() == null) {
            throw new InvalidColumnException(
                    "Column data type cannot be null: " + columnName
            );
        }
    }

    /**
     * Veritabanı klasörü yoksa oluşturur.
     */
    private void ensureDatabaseDirectoryExists() {
        try {
            Files.createDirectories(databaseDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Database directory could not be created: " +
                            databaseDirectory,
                    exception
            );
        }
    }

    /**
     * Tablo dosyasının tam yolunu oluşturur.
     *
     * @param tableName tablo adı
     * @return tablo dosya yolu
     */
    private Path resolveTableFile(String tableName) {
        return databaseDirectory.resolve(
                tableName + TABLE_FILE_EXTENSION
        );
    }

    /**
     * Tablo adını normalize eder.
     *
     * @param tableName tablo adı
     * @return normalize edilmiş tablo adı
     */
    private String normalizeTableName(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        return tableName
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Yeni oluşturulan .tbl dosyasının başlangıç içeriğini üretir.
     *
     * Şimdilik tablo şeması metinsel olarak yazılır.
     * İleride bu bölüm binary tablo header formatına dönüştürülecek.
     *
     * @param table    tablo
     * @param metadata metadata
     * @return dosya içeriği
     */
    private byte[] createInitialTableFileContent(
            Table table,
            TableMetadata metadata
    ) {
        StringBuilder builder = new StringBuilder();

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

        return builder.toString().getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        );
    }

    /**
     * Katalog kaydı başarısız olursa oluşturulan dosyayı geri alır.
     *
     * @param tableFile tablo dosyası
     */
    private void rollbackTableFileCreation(Path tableFile) {
        try {
            Files.deleteIfExists(tableFile);
        } catch (IOException ignored) {
            // Asıl hata korunur. Rollback hatası burada bastırılır.
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
        return Objects.hash(databaseDirectory, tableCatalog);
    }
}