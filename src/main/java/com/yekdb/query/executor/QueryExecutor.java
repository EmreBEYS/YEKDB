package com.yekdb.query.executor;

import com.yekdb.database.Database;
import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.query.mapper.StatementCommandMapper;
import com.yekdb.query.parser.SqlParser;
import com.yekdb.query.statement.Statement;
import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.page.PageManager;
import com.yekdb.storage.page.PageType;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.RowSerializer;
import com.yekdb.table.Table;
import com.yekdb.table.TableManager;
import com.yekdb.table.TableMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL komutlarını ilgili YEKDB yönetici ve depolama
 * katmanlarına yönlendirerek çalıştırır.
 *
 * <p>Desteklenen temel işlemler:</p>
 *
 * <ul>
 *     <li>CREATE DATABASE</li>
 *     <li>USE DATABASE</li>
 *     <li>DROP DATABASE</li>
 *     <li>CREATE TABLE</li>
 *     <li>DROP TABLE</li>
 *     <li>INSERT INTO</li>
 *     <li>SELECT *</li>
 *     <li>DELETE ... WHERE record_id = value</li>
 * </ul>
 */
public final class QueryExecutor implements AutoCloseable {

    /**
     * Tablo kayıtlarının tutulduğu fiziksel veri dosyası uzantısı.
     */
    private static final String TABLE_DATA_FILE_EXTENSION = ".data";

    /**
     * Sprint 00-10 kapsamında desteklenen DELETE koşulu.
     *
     * Örnekler:
     *
     * record_id = 5
     * recordId = 5
     * _record_id = 5
     */
    private static final Pattern RECORD_ID_CONDITION_PATTERN =
            Pattern.compile(
                    "(?i)^(record_id|recordId|_record_id)\\s*=\\s*(\\d+)$"
            );

    private final DatabaseManager databaseManager;
    private final SqlParser sqlParser;

    /**
     * Açılmış tablo veri dosyaları.
     */
    private final Map<String, DataFile> openDataFiles;

    /**
     * Tablo adına göre oluşturulmuş RecordManager nesneleri.
     */
    private final Map<String, RecordManager> recordManagers;

    /**
     * Aktif veritabanına bağlı tablo yöneticisi.
     */
    private TableManager tableManager;

    /**
     * Yeni QueryExecutor oluşturur.
     *
     * @param databaseManager veritabanı yöneticisi
     */
    public QueryExecutor(DatabaseManager databaseManager) {
        this(
                databaseManager,
                new SqlParser()
        );
    }

    /**
     * Belirtilen parser ile yeni QueryExecutor oluşturur.
     *
     * @param databaseManager veritabanı yöneticisi
     * @param sqlParser       SQL parser
     */
    public QueryExecutor(
            DatabaseManager databaseManager,
            SqlParser sqlParser
    ) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager,
                "DatabaseManager cannot be null."
        );

        this.sqlParser = Objects.requireNonNull(
                sqlParser,
                "SqlParser cannot be null."
        );

        this.openDataFiles = new HashMap<>();
        this.recordManagers = new HashMap<>();

        initializeTableManager();
    }

    /**
     * SQL metnini parse eder, Command nesnesine dönüştürür
     * ve çalıştırır.
     *
     * @param sql çalıştırılacak SQL
     * @return yürütme sonucu
     */
    public ExecuteResult execute(String sql) {
        Statement statement = sqlParser.parse(sql);

        return execute(statement);
    }

    /**
     * Statement nesnesini Command nesnesine dönüştürerek
     * çalıştırır.
     *
     * @param statement çalıştırılacak statement
     * @return yürütme sonucu
     */
    public ExecuteResult execute(Statement statement) {
        Command command =
                StatementCommandMapper.map(statement);

        return execute(command);
    }

    /**
     * Verilen Command nesnesini çalıştırır.
     *
     * @param command çalıştırılacak komut
     * @return yürütme sonucu
     */
    public ExecuteResult execute(Command command) {
        if (command == null) {
            throw new QueryExecutionException(
                    "Command cannot be null."
            );
        }

        try {
            if (command instanceof CreateDatabaseCommand value) {
                return executeCreateDatabase(value);
            }

            if (command instanceof UseDatabaseCommand value) {
                return executeUseDatabase(value);
            }

            if (command instanceof DropDatabaseCommand value) {
                return executeDropDatabase(value);
            }

            if (command instanceof CreateTableCommand value) {
                return executeCreateTable(value);
            }

            if (command instanceof DropTableCommand value) {
                return executeDropTable(value);
            }

            if (command instanceof InsertCommand value) {
                return executeInsert(value);
            }

            if (command instanceof SelectCommand value) {
                return executeSelect(value);
            }

            if (command instanceof DeleteCommand value) {
                return executeDelete(value);
            }

            throw new QueryExecutionException(
                    "Unsupported command type: "
                            + command.getClass().getSimpleName()
            );

        } catch (QueryExecutionException exception) {
            throw exception;

        } catch (IOException exception) {
            throw new QueryExecutionException(
                    "Physical query execution failed for command: "
                            + command.getClass().getSimpleName(),
                    exception
            );

        } catch (RuntimeException exception) {
            throw new QueryExecutionException(
                    "Query execution failed for command: "
                            + command.getClass().getSimpleName(),
                    exception
            );
        }
    }

    /**
     * CREATE DATABASE komutunu çalıştırır.
     */
    private ExecuteResult executeCreateDatabase(
            CreateDatabaseCommand command
    ) {
        Database database = databaseManager.createDatabase(
                command.getDatabaseName()
        );

        return ExecuteResult.success(
                "Database created successfully: "
                        + database.getName()
        );
    }

    /**
     * USE DATABASE komutunu çalıştırır.
     */
    private ExecuteResult executeUseDatabase(
            UseDatabaseCommand command
    ) throws IOException {

        closeRecordResources();

        Database database = databaseManager.useDatabase(
                command.getDatabaseName()
        );

        tableManager = new TableManager(
                database.getDatabasePath()
        );

        return ExecuteResult.success(
                "Database selected successfully: "
                        + database.getName()
        );
    }

    /**
     * DROP DATABASE komutunu çalıştırır.
     */
    private ExecuteResult executeDropDatabase(
            DropDatabaseCommand command
    ) throws IOException {

        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        boolean droppingCurrentDatabase =
                currentDatabase != null
                        && currentDatabase.getName()
                        .equalsIgnoreCase(
                                command.getDatabaseName()
                        );

        if (droppingCurrentDatabase) {
            closeRecordResources();
        }

        databaseManager.dropDatabase(
                command.getDatabaseName()
        );

        if (droppingCurrentDatabase) {
            tableManager = null;
        }

        return ExecuteResult.success(
                "Database dropped successfully: "
                        + command.getDatabaseName()
        );
    }

    /**
     * CREATE TABLE komutunu çalıştırır.
     */
    private ExecuteResult executeCreateTable(
            CreateTableCommand command
    ) {
        TableManager activeTableManager =
                requireTableManager();

        TableMetadata metadata =
                activeTableManager.createTable(
                        command.getTableName(),
                        command.getColumns()
                );

        return ExecuteResult.success(
                "Table created successfully: "
                        + metadata.getTableName()
        );
    }

    /**
     * DROP TABLE komutunu çalıştırır.
     */
    private ExecuteResult executeDropTable(
            DropTableCommand command
    ) throws IOException {

        TableManager activeTableManager =
                requireTableManager();

        closeTableRecordResources(
                command.getTableName()
        );

        activeTableManager.dropTable(
                command.getTableName()
        );

        deleteTableDataFile(
                command.getTableName()
        );

        return ExecuteResult.success(
                "Table dropped successfully: "
                        + command.getTableName()
        );
    }

    /**
     * INSERT komutunu çalıştırır.
     */
    private ExecuteResult executeInsert(
            InsertCommand command
    ) throws IOException {

        TableManager activeTableManager =
                requireTableManager();

        requireTableExists(
                activeTableManager,
                command.getTableName()
        );

        Table table = activeTableManager.getTable(
                command.getTableName()
        );

        validateInsertValueCount(
                table,
                command
        );

        /*
         * Row sınıfı mevcut sürümde null değer kabul etmez.
         */
        validateNoNullValues(
                command.getValues()
        );

        Row row = new Row(
                command.getValues()
        );

        RecordManager recordManager =
                requireRecordManager(
                        command.getTableName()
                );

        Record record =
                recordManager.insert(row);

        return ExecuteResult.success(
                "Record inserted successfully. "
                        + "Table: "
                        + command.getTableName()
                        + ", record ID: "
                        + record.getRecordId(),
                1
        );
    }

    /**
     * SELECT komutunu çalıştırır.
     *
     * Sprint 00-10 kapsamında SELECT * desteklenir.
     */
    private ExecuteResult executeSelect(
            SelectCommand command
    ) throws IOException {

        TableManager activeTableManager =
                requireTableManager();

        requireTableExists(
                activeTableManager,
                command.getTableName()
        );

        if (!command.isSelectAll()) {
            throw new QueryExecutionException(
                    "Selecting specific columns is not supported "
                            + "in Sprint 00-10. Use SELECT *."
            );
        }

        RecordManager recordManager =
                requireRecordManager(
                        command.getTableName()
                );

        List<Record> records =
                recordManager.getActiveRecords();

        List<Row> rows =
                new ArrayList<>(records.size());

        for (Record record : records) {
            Row row = RowSerializer.deserialize(
                    record.getData()
            );

            rows.add(row);
        }

        return ExecuteResult.success(
                rows.size()
                        + " row(s) selected from table: "
                        + command.getTableName(),
                rows
        );
    }

    /**
     * DELETE komutunu çalıştırır.
     *
     * Sprint 00-10 kapsamında kayıt silme işlemi
     * fiziksel Record ID üzerinden yapılır.
     */
    private ExecuteResult executeDelete(
            DeleteCommand command
    ) throws IOException {

        TableManager activeTableManager =
                requireTableManager();

        requireTableExists(
                activeTableManager,
                command.getTableName()
        );

        if (!command.hasWhereClause()) {
            throw new QueryExecutionException(
                    "DELETE without WHERE is not supported."
            );
        }

        long recordId = parseRecordIdCondition(
                command.getWhereClause()
        );

        RecordManager recordManager =
                requireRecordManager(
                        command.getTableName()
                );

        recordManager.delete(recordId);

        return ExecuteResult.success(
                "Record deleted successfully. "
                        + "Table: "
                        + command.getTableName()
                        + ", record ID: "
                        + recordId,
                1
        );
    }

    /**
     * INSERT değer sayısı ile tablo sütun sayısını karşılaştırır.
     */
    private void validateInsertValueCount(
            Table table,
            InsertCommand command
    ) {
        int expectedColumnCount =
                table.getColumnCount();

        int actualValueCount =
                command.getValueCount();

        if (expectedColumnCount != actualValueCount) {
            throw new QueryExecutionException(
                    "INSERT value count does not match table column count. "
                            + "Expected: "
                            + expectedColumnCount
                            + ", received: "
                            + actualValueCount
                            + "."
            );
        }
    }

    /**
     * Row mevcut sürümde null desteklemediği için
     * null değerleri yürütme öncesinde engeller.
     */
    private void validateNoNullValues(
            List<Object> values
    ) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index) == null) {
                throw new QueryExecutionException(
                        "NULL values are not supported by Row yet. "
                                + "Value index: "
                                + index
                                + "."
                );
            }
        }
    }

    /**
     * WHERE koşulundan Record ID değerini çıkarır.
     */
    private long parseRecordIdCondition(
            String whereClause
    ) {
        Matcher matcher =
                RECORD_ID_CONDITION_PATTERN.matcher(
                        whereClause.trim()
                );

        if (!matcher.matches()) {
            throw new QueryExecutionException(
                    "Sprint 00-10 DELETE condition must use "
                            + "record_id = <number>. Received: "
                            + whereClause
            );
        }

        try {
            return Long.parseLong(
                    matcher.group(2)
            );

        } catch (NumberFormatException exception) {
            throw new QueryExecutionException(
                    "Invalid record ID in DELETE condition: "
                            + matcher.group(2),
                    exception
            );
        }
    }

    /**
     * Belirtilen tabloya bağlı RecordManager nesnesini
     * döndürür veya oluşturur.
     */
    private RecordManager requireRecordManager(
            String tableName
    ) throws IOException {

        Database currentDatabase =
                requireCurrentDatabase();

        String normalizedTableName =
                normalizeTableName(tableName);

        RecordManager existingManager =
                recordManagers.get(normalizedTableName);

        if (existingManager != null) {
            return existingManager;
        }

        Path dataFilePath =
                currentDatabase
                        .getDatabasePath()
                        .resolve(
                                normalizedTableName
                                        + TABLE_DATA_FILE_EXTENSION
                        );

        DataFile dataFile =
                new DataFile(dataFilePath);

        dataFile.open();

        try {
            initializeDataFileHeader(dataFile);

            PageManager pageManager =
                    new PageManager(dataFile);

            RecordManager recordManager =
                    new RecordManager(
                            pageManager,
                            PageType.DATA
                    );

            openDataFiles.put(
                    normalizedTableName,
                    dataFile
            );

            recordManagers.put(
                    normalizedTableName,
                    recordManager
            );

            return recordManager;

        } catch (IOException | RuntimeException exception) {
            dataFile.close();
            throw exception;
        }
    }

    /**
     * Yeni oluşturulan boş veri dosyasına YEKDB header yazar.
     */
    private void initializeDataFileHeader(
            DataFile dataFile
    ) throws IOException {

        long fileSize = dataFile.size();

        if (fileSize == 0) {
            DatabaseHeader databaseHeader =
                    new DatabaseHeader();

            dataFile.write(
                    0,
                    databaseHeader.toBytes()
            );

            dataFile.sync();
            return;
        }

        if (fileSize < DatabaseHeader.HEADER_SIZE) {
            throw new QueryExecutionException(
                    "Table data file is smaller than the YEKDB header: "
                            + dataFile.getFilePath()
            );
        }

        byte[] headerBytes =
                dataFile.read(
                        0,
                        DatabaseHeader.HEADER_SIZE
                );

        DatabaseHeader.fromBytes(headerBytes);
    }

    /**
     * Tablo işlemlerinden önce aktif veritabanının
     * bulunmasını zorunlu kılar.
     */
    private TableManager requireTableManager() {
        Database currentDatabase =
                requireCurrentDatabase();

        if (tableManager == null) {
            tableManager = new TableManager(
                    currentDatabase.getDatabasePath()
            );
        }

        return tableManager;
    }

    /**
     * Aktif veritabanını döndürür.
     */
    private Database requireCurrentDatabase() {
        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase == null) {
            throw new QueryExecutionException(
                    "No database selected. "
                            + "Execute USE DATABASE first."
            );
        }

        return currentDatabase;
    }

    /**
     * Tablo mevcut değilse yürütmeyi durdurur.
     */
    private void requireTableExists(
            TableManager activeTableManager,
            String tableName
    ) {
        if (!activeTableManager.exists(tableName)) {
            throw new QueryExecutionException(
                    "Table not found: " + tableName
            );
        }
    }

    /**
     * QueryExecutor oluşturulurken aktif bir veritabanı
     * varsa TableManager bağlantısını hazırlar.
     */
    private void initializeTableManager() {
        Database currentDatabase =
                databaseManager.getCurrentDatabase();

        if (currentDatabase == null) {
            tableManager = null;
            return;
        }

        tableManager = new TableManager(
                currentDatabase.getDatabasePath()
        );
    }

    /**
     * Belirli tabloya ait açık veri kaynağını kapatır.
     */
    private void closeTableRecordResources(
            String tableName
    ) throws IOException {

        String normalizedTableName =
                normalizeTableName(tableName);

        recordManagers.remove(
                normalizedTableName
        );

        DataFile dataFile =
                openDataFiles.remove(
                        normalizedTableName
                );

        if (dataFile != null) {
            dataFile.close();
        }
    }

    /**
     * Bütün açık tablo veri dosyalarını kapatır.
     */
    private void closeRecordResources()
            throws IOException {

        IOException firstException = null;

        for (DataFile dataFile : openDataFiles.values()) {
            try {
                dataFile.close();

            } catch (IOException exception) {
                if (firstException == null) {
                    firstException = exception;
                } else {
                    firstException.addSuppressed(
                            exception
                    );
                }
            }
        }

        openDataFiles.clear();
        recordManagers.clear();

        if (firstException != null) {
            throw firstException;
        }
    }

    /**
     * DROP TABLE işleminde tabloya ait binary veri
     * dosyasını siler.
     */
    private void deleteTableDataFile(
            String tableName
    ) throws IOException {

        Database currentDatabase =
                requireCurrentDatabase();

        Path dataFilePath =
                currentDatabase
                        .getDatabasePath()
                        .resolve(
                                normalizeTableName(tableName)
                                        + TABLE_DATA_FILE_EXTENSION
                        );

        java.nio.file.Files.deleteIfExists(
                dataFilePath
        );
    }

    private String normalizeTableName(
            String tableName
    ) {
        if (tableName == null || tableName.isBlank()) {
            throw new QueryExecutionException(
                    "Table name cannot be null or blank."
            );
        }

        return tableName
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Executor tarafından açılan fiziksel veri dosyalarını kapatır.
     */
    @Override
    public void close() throws IOException {
        closeRecordResources();
    }
}