package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.NotNullConstraint;

import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.exception.TableAlreadyExistsException;
import com.yekdb.storage.exception.TableNotFoundException;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderConstants;
import com.yekdb.storage.table.header.TableHeaderIO;
import com.yekdb.storage.table.header.TableHeaderSerializer;
import com.yekdb.storage.table.header.TableHeaderUpdater;
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
 * Sürüm: 1.2
 */
public class TableManager {

    private static final String TABLE_FILE_EXTENSION = ".tbl";
    private static final String DATA_FILE_EXTENSION = ".data";

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

        ForeignKeySchemaValidator.validate(
                table,
                tableCatalog
        );

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
     * Tablo adı, sütunları ve constraint listesiyle yeni tablo oluşturur.
     *
     * Sprint 00-24 Phase 5 CREATE TABLE parser entegrasyonu için
     * eklenmiştir.
     *
     * @param tableName tablo adı
     * @param columns sütun listesi
     * @param constraints constraint listesi
     * @return metadata
     */
    public TableMetadata createTable(
            String tableName,
            List<Column> columns,
            List<Constraint> constraints
    ) {

        return createTable(
                new Table(
                        tableName,
                        columns,
                        constraints
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
     * Boş bir tabloya yeni sütun ekler.
     *
     * Mevcut satırların fiziksel row rewrite işlemi sonraki phase'e
     * bırakıldığı için kayıt içeren tablolarda işlem reddedilir.
     */
    public TableMetadata addColumn(
            String tableName,
            String columnName,
            DataType dataType
    ) {
        Table current = getTable(tableName);
        requireEmptyTableForStructuralAlter(tableName);

        List<Column> columns =
                new java.util.ArrayList<>(current.getColumns());
        columns.add(new Column(columnName, dataType));

        Table updated = new Table(
                current.getTableName(),
                columns,
                current.getConstraints()
        );

        return persistAlteredSchema(current, updated);
    }

    /**
     * Boş bir tablodan sütun kaldırır.
     */
    public TableMetadata dropColumn(
            String tableName,
            String columnName
    ) {
        Table current = getTable(tableName);
        requireEmptyTableForStructuralAlter(tableName);

        if (current.getColumnCount() <= 1) {
            throw new IllegalStateException(
                    "Table must contain at least one column."
            );
        }

        Column target = current.getColumn(columnName);
        requireColumnNotConstrained(current, target.getName());
        requireColumnNotReferencedByForeignKey(
                current.getTableName(),
                target.getName()
        );

        List<Column> columns =
                current.getColumns().stream()
                        .filter(column ->
                                !column.getName().equalsIgnoreCase(target.getName()))
                        .toList();

        Table updated = new Table(
                current.getTableName(),
                columns,
                current.getConstraints()
        );

        return persistAlteredSchema(current, updated);
    }

    /**
     * Sütunun yalnızca şema adını değiştirir. Row verisi positional
     * tutulduğu için fiziksel kayıt rewrite gerekmez. Constraint/FK
     * bağı bulunan sütunlarda metadata tutarsızlığı oluşmaması için
     * işlem bu phase'te reddedilir.
     */
    public TableMetadata renameColumn(
            String tableName,
            String oldColumnName,
            String newColumnName
    ) {
        Table current = getTable(tableName);
        Column target = current.getColumn(oldColumnName);

        requireColumnNotConstrained(current, target.getName());
        requireColumnNotReferencedByForeignKey(
                current.getTableName(),
                target.getName()
        );

        List<Column> columns =
                new java.util.ArrayList<>();

        for (Column column : current.getColumns()) {
            if (column.getName().equalsIgnoreCase(target.getName())) {
                columns.add(new Column(newColumnName, column.getDataType()));
            } else {
                columns.add(column);
            }
        }

        Table updated = new Table(
                current.getTableName(),
                columns,
                current.getConstraints()
        );

        return persistAlteredSchema(current, updated);
    }

    /**
     * Tablo adını fiziksel dosya, binary header, schema metadata ve
     * catalog kaydıyla birlikte değiştirir.
     *
     * Başka bir tablonun FOREIGN KEY metadata'sı bu tablo adına
     * bağlıysa referential metadata bozulmaması için işlem reddedilir.
     */
    public TableMetadata renameTable(
            String tableName,
            String newTableName
    ) {
        Table current = getTable(tableName);
        String normalizedNewName =
                TableNameValidator.validate(newTableName);

        if (current.getTableName().equals(normalizedNewName)) {
            return getMetadata(current.getTableName());
        }

        Path previousFile =
                requireManagedTableFile(current.getTableName());
        Path targetFile =
                resolveTableFile(normalizedNewName);

        Path previousDataFile =
                resolveTableDataFile(current.getTableName());
        Path targetDataFile =
                resolveTableDataFile(normalizedNewName);

        if (tableCatalog.containsTable(normalizedNewName)
                || Files.exists(targetFile)
                || Files.exists(targetDataFile)) {
            throw new TableAlreadyExistsException(
                    "Table already exists: " + normalizedNewName
            );
        }

        requireTableNotReferencedByForeignKey(
                current.getTableName()
        );

        TableMetadata previousMetadata =
                tableCatalog.getMetadata(current.getTableName());
        TableHeader previousHeader =
                getTableHeader(current.getTableName());

        Table renamedTable = new Table(
                normalizedNewName,
                current.getColumns(),
                current.getConstraints()
        );

        TableMetadata renamedMetadata = new TableMetadata(
                normalizedNewName,
                current.getColumnCount(),
                previousMetadata.getCreatedAt(),
                normalizedNewName + TABLE_FILE_EXTENSION,
                previousMetadata.getVersion()
        );

        TableHeader renamedHeader = new TableHeader(
                previousHeader.getTableId(),
                normalizedNewName,
                previousHeader.getColumnCount(),
                previousHeader.getRowCount(),
                previousHeader.getFirstDataPageId(),
                previousHeader.getLastDataPageId(),
                previousHeader.getSchemaOffset(),
                previousHeader.getFlags()
        );

        byte[] headerBytes =
                TableHeaderSerializer.serialize(renamedHeader);
        byte[] schemaBytes =
                createSchemaContent(renamedTable, renamedMetadata);
        ByteBuffer buffer =
                ByteBuffer.allocate(headerBytes.length + schemaBytes.length);
        buffer.put(headerBytes);
        buffer.put(schemaBytes);

        boolean dataFileMoved = false;

        try {
            Files.write(
                    targetFile,
                    buffer.array(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            if (Files.exists(previousDataFile)) {
                Files.move(previousDataFile, targetDataFile);
                dataFileMoved = true;
            }

            Files.delete(previousFile);

            tableCatalog.unregisterTable(current.getTableName());
            tableCatalog.registerTable(renamedTable, renamedMetadata);

            return renamedMetadata;

        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(targetFile);
            } catch (IOException ignored) {
                // Best-effort rollback cleanup.
            }

            if (dataFileMoved
                    && Files.exists(targetDataFile)
                    && !Files.exists(previousDataFile)) {
                try {
                    Files.move(targetDataFile, previousDataFile);
                } catch (IOException ignored) {
                    // Original exception is more relevant.
                }
            }

            if (!Files.exists(previousFile)) {
                try {
                    Files.write(
                            previousFile,
                            createCurrentTableFileContent(
                                    current,
                                    previousMetadata,
                                    previousHeader
                            ),
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE
                    );
                } catch (IOException ignored) {
                    // Original exception is more relevant.
                }
            }

            throw new IllegalStateException(
                    "Table could not be renamed: "
                            + current.getTableName()
                            + " -> "
                            + normalizedNewName,
                    exception
            );
        }
    }

    /**
     * Kolona explicit NOT NULL constraint ekler.
     * Mevcut satır validation hattı sonraki phase'e bırakıldığı için
     * bu işlem şimdilik yalnızca boş tablolarda desteklenir.
     */
    public TableMetadata setColumnNotNull(
            String tableName,
            String columnName
    ) {
        Table current = getTable(tableName);
        Column target = current.getColumn(columnName);

        requireEmptyTableForSetNotNull(tableName);

        boolean alreadyNotNull =
                current.getConstraints().stream()
                        .anyMatch(constraint ->
                                constraint.type() == ConstraintType.NOT_NULL
                                        && constraint.columns().stream()
                                        .anyMatch(column ->
                                                column.equalsIgnoreCase(target.getName())));

        if (alreadyNotNull) {
            throw new IllegalStateException(
                    "Column is already NOT NULL: " + target.getName()
            );
        }

        List<Constraint> constraints =
                new java.util.ArrayList<>(current.getConstraints());
        constraints.add(new NotNullConstraint(target.getName()));

        Table updated = new Table(
                current.getTableName(),
                current.getColumns(),
                constraints
        );

        return persistAlteredSchema(current, updated);
    }

    /**
     * Kolondaki explicit NOT NULL constraint'i kaldırır.
     * PRIMARY KEY kolonlarında nullability SQL semantiğinin parçası
     * olduğu için işlem reddedilir.
     */
    public TableMetadata dropColumnNotNull(
            String tableName,
            String columnName
    ) {
        Table current = getTable(tableName);
        Column target = current.getColumn(columnName);

        boolean primaryKeyColumn =
                current.getConstraints().stream()
                        .anyMatch(constraint ->
                                constraint.type() == ConstraintType.PRIMARY_KEY
                                        && constraint.columns().stream()
                                        .anyMatch(column ->
                                                column.equalsIgnoreCase(target.getName())));

        if (primaryKeyColumn) {
            throw new IllegalStateException(
                    "NOT NULL cannot be dropped from PRIMARY KEY column: "
                            + target.getName()
            );
        }

        boolean found = false;
        List<Constraint> constraints =
                new java.util.ArrayList<>();

        for (Constraint constraint : current.getConstraints()) {
            if (constraint.type() == ConstraintType.NOT_NULL
                    && constraint.columns().size() == 1
                    && constraint.columns().getFirst()
                    .equalsIgnoreCase(target.getName())) {
                found = true;
                continue;
            }
            constraints.add(constraint);
        }

        if (!found) {
            throw new IllegalStateException(
                    "Column does not have an explicit NOT NULL constraint: "
                            + target.getName()
            );
        }

        Table updated = new Table(
                current.getTableName(),
                current.getColumns(),
                constraints
        );

        return persistAlteredSchema(current, updated);
    }

    private void requireEmptyTableForSetNotNull(
            String tableName
    ) {
        if (hasActiveRows(tableName)) {
            throw new IllegalStateException(
                    "ALTER COLUMN SET NOT NULL currently requires an empty table. "
                            + "Existing row validation will be supported in a later phase."
            );
        }
    }

    private void requireTableNotReferencedByForeignKey(
            String tableName
    ) {
        for (Table candidate : tableCatalog.listTables()) {
            for (Constraint constraint : candidate.getConstraints()) {
                if (constraint instanceof com.yekdb.constraint.ForeignKeyConstraint foreignKey
                        && foreignKey.referencedTableName()
                        .equalsIgnoreCase(tableName)) {
                    throw new IllegalStateException(
                            "Table is referenced by FOREIGN KEY from table "
                                    + candidate.getTableName()
                                    + " and cannot be renamed yet: "
                                    + tableName
                    );
                }
            }
        }
    }

    private byte[] createCurrentTableFileContent(
            Table table,
            TableMetadata metadata,
            TableHeader header
    ) {
        byte[] headerBytes = TableHeaderSerializer.serialize(header);
        byte[] schemaBytes = createSchemaContent(table, metadata);
        ByteBuffer buffer = ByteBuffer.allocate(
                headerBytes.length + schemaBytes.length
        );
        buffer.put(headerBytes);
        buffer.put(schemaBytes);
        return buffer.array();
    }

    private void requireEmptyTableForStructuralAlter(
            String tableName
    ) {
        if (hasActiveRows(tableName)) {
            throw new IllegalStateException(
                    "ALTER TABLE ADD/DROP COLUMN currently requires an empty table. "
                            + "Existing rows will be supported by row rewrite in a later phase."
            );
        }
    }

    /**
     * ALTER güvenlik kontrolünde gerçek row storage dosyasını kullanır.
     * .tbl header rowCount değeri DML kayıt sayısını temsil etmez.
     */
    private boolean hasActiveRows(String tableName) {
        Path dataFile = resolveTableDataFile(tableName);

        if (!Files.exists(dataFile)) {
            return false;
        }

        StorageEngine storageEngine = new StorageEngine(dataFile);

        try {
            storageEngine.initialize();

            RecordManager recordManager = new RecordManager(
                    storageEngine.getPageManager(),
                    PageType.DATA
            );

            return !recordManager.getActiveRecords().isEmpty();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Table row storage could not be inspected: " + tableName,
                    exception
            );

        } finally {
            if (storageEngine.isInitialized()) {
                try {
                    storageEngine.shutdown();
                } catch (IOException ignored) {
                    // Read-only inspection cleanup; primary result/exception is preserved.
                }
            }
        }
    }

    private Path resolveTableDataFile(String tableName) {
        String normalizedName = TableNameValidator.validate(tableName);

        Path dataFile = databaseDirectory
                .resolve(normalizedName + DATA_FILE_EXTENSION)
                .normalize();

        if (!dataFile.startsWith(databaseDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid table data path: " + tableName
            );
        }

        return dataFile;
    }

    private void requireColumnNotConstrained(
            Table table,
            String columnName
    ) {
        boolean constrained =
                table.getConstraints().stream()
                        .anyMatch(constraint ->
                                constraint.columns().stream()
                                        .anyMatch(column ->
                                                column.equalsIgnoreCase(columnName)));

        if (constrained) {
            throw new IllegalStateException(
                    "Column is used by a constraint and cannot be altered yet: "
                            + columnName
            );
        }
    }

    private void requireColumnNotReferencedByForeignKey(
            String tableName,
            String columnName
    ) {
        for (Table candidate : tableCatalog.listTables()) {
            for (Constraint constraint : candidate.getConstraints()) {
                if (constraint instanceof com.yekdb.constraint.ForeignKeyConstraint foreignKey
                        && foreignKey.referencedTableName().equalsIgnoreCase(tableName)
                        && foreignKey.referencedColumnNames().stream()
                        .anyMatch(column -> column.equalsIgnoreCase(columnName))) {
                    throw new IllegalStateException(
                            "Column is referenced by FOREIGN KEY from table "
                                    + candidate.getTableName()
                                    + ": "
                                    + columnName
                    );
                }
            }
        }
    }

    private TableMetadata persistAlteredSchema(
            Table previousTable,
            Table updatedTable
    ) {
        String tableName = previousTable.getTableName();
        Path tableFile = requireManagedTableFile(tableName);
        TableMetadata previousMetadata = tableCatalog.getMetadata(tableName);
        TableHeader previousHeader = getTableHeader(tableName);

        TableMetadata updatedMetadata = new TableMetadata(
                tableName,
                updatedTable.getColumnCount(),
                previousMetadata.getCreatedAt(),
                previousMetadata.getFileName(),
                previousMetadata.getVersion()
        );

        TableHeader updatedHeader = new TableHeader(
                previousHeader.getTableId(),
                previousHeader.getTableName(),
                updatedTable.getColumnCount(),
                previousHeader.getRowCount(),
                previousHeader.getFirstDataPageId(),
                previousHeader.getLastDataPageId(),
                previousHeader.getSchemaOffset(),
                previousHeader.getFlags()
        );

        byte[] headerBytes = TableHeaderSerializer.serialize(updatedHeader);
        byte[] schemaBytes = createSchemaContent(updatedTable, updatedMetadata);
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + schemaBytes.length);
        buffer.put(headerBytes);
        buffer.put(schemaBytes);

        try {
            Files.write(
                    tableFile,
                    buffer.array(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            tableCatalog.unregisterTable(tableName);
            tableCatalog.registerTable(updatedTable, updatedMetadata);
            return updatedMetadata;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Altered table schema could not be persisted: " + tableName,
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
     * Verilen tablonun disk üzerindeki Binary Table Header
     * bilgisini döndürür.
     *
     * @param tableName tablo adı
     * @return persistent table header
     */
    public TableHeader getTableHeader(
            String tableName
    ) {

        Path tableFile =
                requireManagedTableFile(
                        tableName
                );

        try {
            return TableHeaderIO.read(
                    tableFile
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Table header could not be read: "
                            + tableName,
                    exception
            );
        }
    }



    /**
     * Mevcut tabloya yeni constraint ekler.
     *
     * <p>Yeni Table örneği oluşturularak kolon/PRIMARY KEY gibi domain
     * kuralları yeniden doğrulanır. FOREIGN KEY şema ilişkisi catalog
     * üzerinden kontrol edilir. Tablo kayıt içeriyorsa yeni constraint'in
     * mevcut satırlar tarafından da sağlandığı doğrulanmadan metadata
     * persist edilmez.</p>
     *
     * Sprint 00-26 Phase 5.
     *
     * @param tableName tablo adı
     * @param constraint eklenecek constraint
     * @return güncellenmiş metadata
     */
    public TableMetadata addConstraint(
            String tableName,
            Constraint constraint
    ) {
        Objects.requireNonNull(
                constraint,
                "Constraint cannot be null."
        );

        Table current = getTable(tableName);

        requireConstraintNotAlreadyPresent(
                current,
                constraint
        );

        List<Constraint> constraints =
                new java.util.ArrayList<>(
                        current.getConstraints()
                );

        constraints.add(constraint);

        Table updated = new Table(
                current.getTableName(),
                current.getColumns(),
                constraints
        );

        ForeignKeySchemaValidator.validate(
                updated,
                tableCatalog
        );

        AlterTableConstraintValidator.validateExistingRows(
                databaseDirectory,
                tableCatalog,
                updated,
                constraint
        );

        return persistAlteredSchema(
                current,
                updated
        );
    }

    /**
     * İsmi verilen constraint'i tablodan kaldırır.
     *
     * <p>PRIMARY KEY / UNIQUE constraint başka bir FOREIGN KEY tarafından
     * candidate key olarak kullanılıyorsa referential integrity korunması için
     * DROP işlemi reddedilir.</p>
     *
     * Sprint 00-26 Phase 6.
     *
     * @param tableName tablo adı
     * @param constraintName kaldırılacak explicit constraint adı
     * @return güncellenmiş metadata
     */
    public TableMetadata dropConstraint(
            String tableName,
            String constraintName
    ) {
        if (constraintName == null || constraintName.isBlank()) {
            throw new IllegalArgumentException(
                    "Constraint name cannot be null or blank."
            );
        }

        String normalizedName = constraintName.trim();
        Table current = getTable(tableName);

        Constraint target = current.getConstraints()
                .stream()
                .filter(constraint ->
                        constraint.name() != null
                                && constraint.name().equalsIgnoreCase(normalizedName)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Named constraint not found on table '"
                                        + current.getTableName()
                                        + "': "
                                        + normalizedName
                        )
                );

        if (target.type() == ConstraintType.PRIMARY_KEY
                || target.type() == ConstraintType.UNIQUE) {
            requireCandidateKeyNotReferenced(
                    current.getTableName(),
                    target
            );
        }

        List<Constraint> constraints =
                new java.util.ArrayList<>(current.getConstraints());

        constraints.remove(target);

        Table updated = new Table(
                current.getTableName(),
                current.getColumns(),
                constraints
        );

        ForeignKeySchemaValidator.validate(
                updated,
                tableCatalog
        );

        return persistAlteredSchema(
                current,
                updated
        );
    }

    private void requireCandidateKeyNotReferenced(
            String referencedTableName,
            Constraint candidateKey
    ) {
        for (Table candidateTable : tableCatalog.listTables()) {
            for (Constraint constraint : candidateTable.getConstraints()) {
                if (!(constraint instanceof ForeignKeyConstraint foreignKey)) {
                    continue;
                }

                if (!foreignKey.referencedTableName()
                        .equalsIgnoreCase(referencedTableName)) {
                    continue;
                }

                if (sameCandidateKeyColumns(
                        candidateKey.columns(),
                        foreignKey.referencedColumnNames()
                )) {
                    throw new IllegalStateException(
                            "Constraint '"
                                    + candidateKey.name()
                                    + "' cannot be dropped because it is referenced by FOREIGN KEY"
                                    + (constraint.name() == null
                                    ? ""
                                    : " '" + constraint.name() + "'")
                                    + " on table '"
                                    + candidateTable.getTableName()
                                    + "'."
                    );
                }
            }
        }
    }

    private boolean sameCandidateKeyColumns(
            List<String> left,
            List<String> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }

        for (String leftColumn : left) {
            boolean found = right.stream()
                    .anyMatch(rightColumn ->
                            leftColumn.equalsIgnoreCase(rightColumn)
                    );

            if (!found) {
                return false;
            }
        }

        return true;
    }

    private void requireConstraintNotAlreadyPresent(
            Table table,
            Constraint candidate
    ) {
        boolean duplicate = table.getConstraints()
                .stream()
                .anyMatch(existing ->
                        sameConstraintDefinition(
                                existing,
                                candidate
                        )
                );

        if (duplicate) {
            throw new IllegalStateException(
                    "Equivalent constraint already exists on table '"
                            + table.getTableName()
                            + "': "
                            + candidate.type()
                            + " "
                            + candidate.columns()
            );
        }
    }

    private boolean sameConstraintDefinition(
            Constraint left,
            Constraint right
    ) {
        if (left.type() != right.type()) {
            return false;
        }

        if (!sameConstraintColumns(
                left.columns(),
                right.columns()
        )) {
            return false;
        }

        if (left instanceof ForeignKeyConstraint leftForeignKey
                && right instanceof ForeignKeyConstraint rightForeignKey) {
            return leftForeignKey.referencedTableName()
                    .equalsIgnoreCase(
                            rightForeignKey.referencedTableName()
                    )
                    && sameConstraintColumns(
                            leftForeignKey.referencedColumnNames(),
                            rightForeignKey.referencedColumnNames()
                    );
        }

        return true;
    }

    private boolean sameConstraintColumns(
            List<String> left,
            List<String> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            if (!left.get(index)
                    .equalsIgnoreCase(right.get(index))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tablonun persistent rowCount metadata değerini
     * belirtilen değer ile değiştirir.
     *
     * @param tableName tablo adı
     * @param rowCount yeni row count
     * @return güncellenmiş persistent header
     */
    public TableHeader updateTableRowCount(
            String tableName,
            long rowCount
    ) {

        Path tableFile =
                requireManagedTableFile(
                        tableName
                );

        return TableHeaderUpdater.persistRowCount(
                tableFile,
                rowCount
        );
    }

    /**
     * Tablonun persistent rowCount metadata değerini
     * bir artırır.
     *
     * @param tableName tablo adı
     * @return güncellenmiş persistent header
     */
    public TableHeader incrementTableRowCount(
            String tableName
    ) {

        Path tableFile =
                requireManagedTableFile(
                        tableName
                );

        return TableHeaderUpdater.persistIncrementRowCount(
                tableFile
        );
    }

    /**
     * Tablonun persistent rowCount metadata değerini
     * bir azaltır.
     *
     * @param tableName tablo adı
     * @return güncellenmiş persistent header
     */
    public TableHeader decrementTableRowCount(
            String tableName
    ) {

        Path tableFile =
                requireManagedTableFile(
                        tableName
                );

        return TableHeaderUpdater.persistDecrementRowCount(
                tableFile
        );
    }

    /**
     * Tablonun first/last physical data page metadata
     * değerlerini atomik olarak günceller.
     *
     * @param tableName       tablo adı
     * @param firstDataPageId ilk data page ID
     * @param lastDataPageId  son data page ID
     * @return güncellenmiş persistent header
     */
    public TableHeader updateTableDataPageRange(
            String tableName,
            long firstDataPageId,
            long lastDataPageId
    ) {

        Path tableFile =
                requireManagedTableFile(
                        tableName
                );

        return TableHeaderUpdater.persistDataPageRange(
                tableFile,
                firstDataPageId,
                lastDataPageId
        );
    }

    /**
     * Tablonun physical data page range metadata değerini
     * boş duruma getirir.
     *
     * @param tableName tablo adı
     * @return güncellenmiş persistent header
     */
    public TableHeader clearTableDataPageRange(
            String tableName
    ) {
        return updateTableDataPageRange(
                tableName,
                -1L,
                -1L
        );
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
     * Tablo adını doğrular ve tablonun hem katalog hem de
     * fiziksel storage tarafından yönetildiğini garanti eder.
     *
     * @param tableName tablo adı
     * @return doğrulanmış fiziksel .tbl yolu
     */
    private Path requireManagedTableFile(
            String tableName
    ) {

        String normalizedName =
                TableNameValidator.validate(
                        tableName
                );

        Path tableFile =
                resolveTableFile(
                        normalizedName
                );

        boolean registered =
                tableCatalog.containsTable(
                        normalizedName
                );

        boolean fileExists =
                Files.isRegularFile(
                        tableFile
                );

        if (!registered && !fileExists) {
            throw new TableNotFoundException(
                    "Table not found: "
                            + normalizedName
            );
        }

        if (!registered) {
            throw new TableNotFoundException(
                    "Table exists on disk but is not registered "
                            + "in catalog: "
                            + normalizedName
            );
        }

        if (!fileExists) {
            throw new IllegalStateException(
                    "Table is registered in catalog but physical file is missing: "
                            + normalizedName
            );
        }

        return tableFile;
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

        /*
         * Sprint 00-24 Phase 6:
         *
         * Constraint metadata fiziksel tablo şemasının sonuna
         * ayrı bir section olarak yazılır.
         *
         * Eski constraint-free tabloların recovery desteği
         * TableFileMetadataReader tarafında korunmaktadır.
         */
        for (String constraintLine :
                ConstraintSchemaCodec.serialize(
                        table.getConstraints()
                )) {

            builder.append(constraintLine)
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