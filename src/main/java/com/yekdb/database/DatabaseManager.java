package com.yekdb.database;

import com.yekdb.database.exception.DatabaseAlreadyExistsException;
import com.yekdb.database.exception.DatabaseNotFoundException;
import com.yekdb.database.exception.DatabaseOperationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB içerisindeki veritabanlarının oluşturulması,
 * seçilmesi, listelenmesi ve silinmesinden sorumludur.
 */
public class DatabaseManager {

    private static final String METADATA_FILE_NAME =
            "database.meta";

    private static final String METADATA_HEADER =
            "YEKDB DATABASE";

    /**
     * Tüm veritabanlarının saklandığı ana dizin.
     */
    private final Path dataDirectory;

    /**
     * Aktif olarak seçilmiş veritabanı.
     */
    private Database currentDatabase;

    /**
     * Yeni bir DatabaseManager oluşturur.
     *
     * @param dataDirectory veritabanlarının saklanacağı ana dizin
     */
    public DatabaseManager(Path dataDirectory) {

        this.dataDirectory = Objects.requireNonNull(
                dataDirectory,
                "Data directory cannot be null."
        ).normalize();
    }

    /**
     * Ana veri dizinini döndürür.
     *
     * @return ana veri dizini
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Aktif veritabanını döndürür.
     *
     * @return aktif veritabanı veya null
     */
    public Database getCurrentDatabase() {
        return currentDatabase;
    }

    /**
     * Veritabanının mevcut olup olmadığını kontrol eder.
     *
     * @param databaseName veritabanı adı
     * @return veritabanı dizini ve metadata dosyası varsa true
     */
    public boolean exists(String databaseName) {

        String normalizedName =
                DatabaseNameValidator.validate(databaseName);

        Path databasePath =
                resolveDatabasePath(normalizedName);

        return Files.isDirectory(databasePath)
                && Files.isRegularFile(
                databasePath.resolve(METADATA_FILE_NAME)
        );
    }

    /**
     * Yeni bir veritabanı oluşturur.
     *
     * @param databaseName oluşturulacak veritabanının adı
     * @return oluşturulan veritabanı
     */
    public Database createDatabase(String databaseName) {

        String normalizedName =
                DatabaseNameValidator.validate(databaseName);

        if (exists(normalizedName)) {
            throw new DatabaseAlreadyExistsException(
                    normalizedName
            );
        }

        Path databasePath =
                resolveDatabasePath(normalizedName);

        boolean databaseDirectoryCreated = false;

        try {
            Files.createDirectories(dataDirectory);

            Files.createDirectory(databasePath);

            databaseDirectoryCreated = true;

            DatabaseMetadata metadata =
                    new DatabaseMetadata(normalizedName);

            Database database =
                    new Database(
                            normalizedName,
                            databasePath,
                            metadata
                    );

            writeMetadataFile(database);

            return database;

        } catch (IOException exception) {

            if (databaseDirectoryCreated) {
                deleteIncompleteDatabaseDirectory(
                        databasePath
                );
            }

            throw new DatabaseOperationException(
                    "Failed to create database: "
                            + normalizedName,
                    exception
            );

        } catch (RuntimeException exception) {

            if (databaseDirectoryCreated) {
                deleteIncompleteDatabaseDirectory(
                        databasePath
                );
            }

            throw exception;
        }
    }

    /**
     * Mevcut bir veritabanını aktif veritabanı olarak seçer.
     *
     * @param databaseName seçilecek veritabanının adı
     * @return aktif hâle getirilen veritabanı
     */
    public Database useDatabase(String databaseName) {

        String normalizedName =
                DatabaseNameValidator.validate(databaseName);

        if (!exists(normalizedName)) {
            throw new DatabaseNotFoundException(
                    normalizedName
            );
        }

        Path databasePath =
                resolveDatabasePath(normalizedName);

        DatabaseMetadata metadata =
                readMetadataFile(databasePath);

        /*
         * Fiziksel klasör adı ile metadata içerisindeki
         * veritabanı adı aynı olmalıdır.
         */
        if (!normalizedName.equals(
                metadata.getDatabaseName()
        )) {
            throw new DatabaseOperationException(
                    "Database metadata name does not match "
                            + "database directory name. "
                            + "Directory: "
                            + normalizedName
                            + ", Metadata: "
                            + metadata.getDatabaseName()
            );
        }

        Database database =
                new Database(
                        normalizedName,
                        databasePath,
                        metadata
                );

        currentDatabase = database;

        return database;
    }

    /**
     * Belirtilen veritabanını fiziksel olarak siler.
     *
     * <p>Veritabanı aktif veritabanıysa silme işleminden sonra
     * currentDatabase null yapılır.</p>
     *
     * @param databaseName silinecek veritabanının adı
     */
    public void dropDatabase(String databaseName) {

        String normalizedName =
                DatabaseNameValidator.validate(databaseName);

        if (!exists(normalizedName)) {
            throw new DatabaseNotFoundException(
                    normalizedName
            );
        }

        Path databasePath =
                resolveDatabasePath(normalizedName);

        try (var paths = Files.walk(databasePath)) {

            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);

                        } catch (IOException exception) {
                            throw new DatabaseDeletionRuntimeException(
                                    exception
                            );
                        }
                    });

        } catch (DatabaseDeletionRuntimeException exception) {

            throw new DatabaseOperationException(
                    "Failed to drop database: "
                            + normalizedName,
                    exception.getCause()
            );

        } catch (IOException exception) {

            throw new DatabaseOperationException(
                    "Failed to read database directory: "
                            + normalizedName,
                    exception
            );
        }

        if (currentDatabase != null
                && currentDatabase
                .getName()
                .equalsIgnoreCase(normalizedName)) {

            currentDatabase = null;
        }
    }

    /**
     * Tüm geçerli veritabanlarını alfabetik olarak listeler.
     *
     * @return veritabanı adları
     */
    public List<String> listDatabases() {

        if (!Files.isDirectory(dataDirectory)) {
            return List.of();
        }

        try (var paths = Files.list(dataDirectory)) {

            return paths
                    .filter(Files::isDirectory)
                    .filter(path ->
                            Files.isRegularFile(
                                    path.resolve(
                                            METADATA_FILE_NAME
                                    )
                            )
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .sorted(
                            String.CASE_INSENSITIVE_ORDER
                    )
                    .toList();

        } catch (IOException exception) {

            throw new DatabaseOperationException(
                    "Failed to list databases in directory: "
                            + dataDirectory,
                    exception
            );
        }
    }

    /**
     * database.meta dosyasını diske yazar.
     *
     * @param database veritabanı
     * @throws IOException metadata yazma işlemi başarısız olursa
     */
    private void writeMetadataFile(
            Database database
    ) throws IOException {

        Path metadataFile =
                database
                        .getDatabasePath()
                        .resolve(METADATA_FILE_NAME);

        DatabaseMetadata metadata =
                database.getMetadata();

        List<String> lines = List.of(
                METADATA_HEADER,
                "Version=" + metadata.getVersion(),
                "Database=" + metadata.getDatabaseName(),
                "Created=" + metadata.getCreatedAt(),
                "LastModified="
                        + metadata.getLastModifiedAt(),
                "Encoding=" + metadata.getEncoding(),
                "PageSize=" + metadata.getPageSize()
        );

        Files.write(
                metadataFile,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    /**
     * database.meta dosyasını okuyarak metadata nesnesi oluşturur.
     *
     * @param databasePath veritabanı dizini
     * @return metadata
     */
    private DatabaseMetadata readMetadataFile(
            Path databasePath
    ) {

        Path metadataFile =
                databasePath.resolve(METADATA_FILE_NAME);

        try {
            List<String> lines =
                    Files.readAllLines(
                            metadataFile,
                            StandardCharsets.UTF_8
                    );

            if (lines.isEmpty()
                    || !METADATA_HEADER.equals(
                    lines.get(0)
            )) {

                throw new DatabaseOperationException(
                        "Invalid database metadata file: "
                                + metadataFile
                );
            }

            String version =
                    readMetadataValue(
                            lines,
                            "Version"
                    );

            String databaseName =
                    readMetadataValue(
                            lines,
                            "Database"
                    );

            String created =
                    readMetadataValue(
                            lines,
                            "Created"
                    );

            String lastModified =
                    readMetadataValue(
                            lines,
                            "LastModified"
                    );

            String encoding =
                    readMetadataValue(
                            lines,
                            "Encoding"
                    );

            String pageSize =
                    readMetadataValue(
                            lines,
                            "PageSize"
                    );

            return new DatabaseMetadata(
                    databaseName,
                    version,
                    LocalDateTime.parse(created),
                    LocalDateTime.parse(lastModified),
                    encoding,
                    Integer.parseInt(pageSize)
            );

        } catch (
                IOException
                | DateTimeParseException
                | NumberFormatException exception
        ) {

            throw new DatabaseOperationException(
                    "Failed to read database metadata: "
                            + metadataFile,
                    exception
            );
        }
    }

    /**
     * Metadata satırları içinden belirtilen anahtarın
     * değerini döndürür.
     *
     * @param lines metadata satırları
     * @param key metadata anahtarı
     * @return metadata değeri
     */
    private String readMetadataValue(
            List<String> lines,
            String key
    ) {

        String prefix = key + "=";

        return lines.stream()
                .filter(line ->
                        line.startsWith(prefix)
                )
                .map(line ->
                        line.substring(
                                prefix.length()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new DatabaseOperationException(
                                "Missing metadata field: "
                                        + key
                        )
                );
    }

    /**
     * Veritabanının fiziksel yolunu oluşturur.
     *
     * @param databaseName veritabanı adı
     * @return veritabanı dizini
     */
    private Path resolveDatabasePath(
            String databaseName
    ) {

        Path databasePath =
                dataDirectory
                        .resolve(databaseName)
                        .normalize();

        if (!databasePath.startsWith(
                dataDirectory
        )) {
            throw new IllegalArgumentException(
                    "Invalid database path: "
                            + databaseName
            );
        }

        return databasePath;
    }

    /**
     * Başarısız oluşturma işleminden kalan dizini temizler.
     *
     * @param databasePath temizlenecek veritabanı dizini
     */
    private void deleteIncompleteDatabaseDirectory(
            Path databasePath
    ) {

        try {
            Path metadataFile =
                    databasePath.resolve(
                            METADATA_FILE_NAME
                    );

            Files.deleteIfExists(metadataFile);
            Files.deleteIfExists(databasePath);

        } catch (IOException ignored) {
            /*
             * Asıl createDatabase hatası korunur.
             */
        }
    }

    /**
     * Stream içerisinde oluşan IOException'ı
     * dropDatabase metodunun dışına taşıyabilmek için
     * kullanılan dahili exception sınıfıdır.
     */
    private static class DatabaseDeletionRuntimeException
            extends RuntimeException {

        DatabaseDeletionRuntimeException(
                IOException cause
        ) {
            super(cause);
        }

        @Override
        public synchronized IOException getCause() {
            return (IOException) super.getCause();
        }
    }
}