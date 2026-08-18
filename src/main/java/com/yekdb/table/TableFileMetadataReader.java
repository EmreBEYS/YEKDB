package com.yekdb.table;

import com.yekdb.table.exception.CorruptedTableFileException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fiziksel .tbl dosyalarından tablo şeması ve metadata
 * bilgilerini okuyarak YEKDB tablo nesnelerini yeniden oluşturur.
 *
 * Bu sınıf tablo recovery işleminin temel bileşenidir.
 *
 * Sürüm: 1.0
 */
public class TableFileMetadataReader {

    private static final String TABLE_MAGIC = "YEKDB_TABLE";

    /**
     * Verilen .tbl dosyasını okuyarak tablo ve metadata
     * bilgilerini yeniden oluşturur.
     *
     * @param tableFile okunacak fiziksel tablo dosyası
     * @return recovery sonucu
     */
    public TableRecoveryEntry read(Path tableFile) {

        validateTableFile(tableFile);

        List<String> lines = readLines(tableFile);

        validateMinimumLineCount(lines, tableFile);

        validateMagicHeader(lines.get(0), tableFile);

        int version = parseIntegerField(
                lines.get(1),
                "version",
                tableFile
        );

        String tableName = parseStringField(
                lines.get(2),
                "tableName",
                tableFile
        );

        int columnCount = parseIntegerField(
                lines.get(3),
                "columnCount",
                tableFile
        );

        LocalDateTime createdAt =
                parseCreatedAt(
                        lines.get(4),
                        tableFile
                );

        validateColumnsHeader(
                lines.get(5),
                tableFile
        );

        List<Column> columns =
                parseColumns(
                        lines,
                        tableFile
                );

        validateColumnCount(
                columnCount,
                columns,
                tableFile
        );

        validateFileName(
                tableFile,
                tableName
        );

        Table table =
                createTable(
                        tableName,
                        columns,
                        tableFile
                );

        TableMetadata metadata =
                createMetadata(
                        tableName,
                        columnCount,
                        createdAt,
                        tableFile,
                        version
                );

        return new TableRecoveryEntry(
                table,
                metadata
        );
    }

    /**
     * Dosya yolunun geçerli olduğunu kontrol eder.
     */
    private void validateTableFile(Path tableFile) {

        if (tableFile == null) {
            throw new IllegalArgumentException(
                    "Table file cannot be null."
            );
        }

        if (!Files.exists(tableFile)) {
            throw new CorruptedTableFileException(
                    "Table file does not exist: "
                            + tableFile
            );
        }

        if (!Files.isRegularFile(tableFile)) {
            throw new CorruptedTableFileException(
                    "Table path is not a regular file: "
                            + tableFile
            );
        }
    }

    /**
     * Dosyanın tüm satırlarını UTF-8 olarak okur.
     */
    private List<String> readLines(Path tableFile) {

        try {

            return Files.readAllLines(
                    tableFile,
                    StandardCharsets.UTF_8
            );

        } catch (IOException exception) {

            throw new CorruptedTableFileException(
                    "Table file could not be read: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Dosyada gerekli minimum satır sayısının
     * bulunduğunu kontrol eder.
     */
    private void validateMinimumLineCount(
            List<String> lines,
            Path tableFile
    ) {

        if (lines.size() < 6) {
            throw new CorruptedTableFileException(
                    "Table file is incomplete: "
                            + tableFile
            );
        }
    }

    /**
     * YEKDB tablo magic header bilgisini kontrol eder.
     */
    private void validateMagicHeader(
            String header,
            Path tableFile
    ) {

        if (!TABLE_MAGIC.equals(header)) {
            throw new CorruptedTableFileException(
                    "Invalid table file header: "
                            + tableFile
            );
        }
    }

    /**
     * key=value biçimindeki String alanı okur.
     */
    private String parseStringField(
            String line,
            String expectedKey,
            Path tableFile
    ) {

        String prefix =
                expectedKey + "=";

        if (line == null
                || !line.startsWith(prefix)) {

            throw new CorruptedTableFileException(
                    "Expected field '"
                            + expectedKey
                            + "' in table file: "
                            + tableFile
            );
        }

        String value =
                line.substring(
                        prefix.length()
                ).trim();

        if (value.isEmpty()) {
            throw new CorruptedTableFileException(
                    "Field '"
                            + expectedKey
                            + "' cannot be empty: "
                            + tableFile
            );
        }

        return value;
    }

    /**
     * key=value biçimindeki integer alanı okur.
     */
    private int parseIntegerField(
            String line,
            String expectedKey,
            Path tableFile
    ) {

        String value =
                parseStringField(
                        line,
                        expectedKey,
                        tableFile
                );

        try {

            int parsedValue =
                    Integer.parseInt(value);

            if (parsedValue <= 0) {
                throw new CorruptedTableFileException(
                        "Field '"
                                + expectedKey
                                + "' must be greater than zero: "
                                + tableFile
                );
            }

            return parsedValue;

        } catch (NumberFormatException exception) {

            throw new CorruptedTableFileException(
                    "Invalid integer value for field '"
                            + expectedKey
                            + "': "
                            + value,
                    exception
            );
        }
    }

    /**
     * createdAt metadata alanını okur.
     */
    private LocalDateTime parseCreatedAt(
            String line,
            Path tableFile
    ) {

        String value =
                parseStringField(
                        line,
                        "createdAt",
                        tableFile
                );

        try {

            return LocalDateTime.parse(value);

        } catch (DateTimeParseException exception) {

            throw new CorruptedTableFileException(
                    "Invalid createdAt value: "
                            + value,
                    exception
            );
        }
    }

    /**
     * columns= bölüm başlığını doğrular.
     */
    private void validateColumnsHeader(
            String line,
            Path tableFile
    ) {

        if (!"columns=".equals(line)) {
            throw new CorruptedTableFileException(
                    "Invalid columns section header: "
                            + tableFile
            );
        }
    }

    /**
     * columns= satırından sonraki sütunları
     * name:type biçiminde parse eder.
     */
    private List<Column> parseColumns(
            List<String> lines,
            Path tableFile
    ) {

        List<Column> columns =
                new ArrayList<>();

        for (int index = 6;
             index < lines.size();
             index++) {

            String line =
                    lines.get(index).trim();

            if (line.isEmpty()) {
                continue;
            }

            columns.add(
                    parseColumn(
                            line,
                            tableFile
                    )
            );
        }

        if (columns.isEmpty()) {
            throw new CorruptedTableFileException(
                    "Table file contains no columns: "
                            + tableFile
            );
        }

        return columns;
    }

    /**
     * Tek bir sütun satırını parse eder.
     */
    private Column parseColumn(
            String line,
            Path tableFile
    ) {

        int separatorIndex =
                line.indexOf(':');

        if (separatorIndex <= 0
                || separatorIndex
                == line.length() - 1) {

            throw new CorruptedTableFileException(
                    "Invalid column definition: "
                            + line
                            + " in "
                            + tableFile
            );
        }

        String columnName =
                line.substring(
                        0,
                        separatorIndex
                ).trim();

        String dataTypeName =
                line.substring(
                        separatorIndex + 1
                ).trim();

        try {

            DataType dataType =
                    DataType.valueOf(
                            dataTypeName
                    );

            return new Column(
                    columnName,
                    dataType
            );

        } catch (IllegalArgumentException exception) {

            throw new CorruptedTableFileException(
                    "Invalid column definition: "
                            + line
                            + " in "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Metadata sütun sayısı ile gerçekten okunan
     * sütun sayısının eşleştiğini doğrular.
     */
    private void validateColumnCount(
            int expectedColumnCount,
            List<Column> columns,
            Path tableFile
    ) {

        if (expectedColumnCount
                != columns.size()) {

            throw new CorruptedTableFileException(
                    "Column count mismatch in table file: "
                            + tableFile
                            + ". Expected: "
                            + expectedColumnCount
                            + ", actual: "
                            + columns.size()
            );
        }
    }

    /**
     * Fiziksel dosya adının metadata tablo adıyla
     * uyumlu olduğunu doğrular.
     */
    private void validateFileName(
            Path tableFile,
            String tableName
    ) {

        String expectedFileName =
                tableName + ".tbl";

        String actualFileName =
                tableFile
                        .getFileName()
                        .toString();

        if (!expectedFileName.equals(actualFileName)) {

            throw new CorruptedTableFileException(
                    "Table file name does not match table name. "
                            + "Expected: "
                            + expectedFileName
                            + ", actual: "
                            + actualFileName
            );
        }
    }

    /**
     * Parse edilen schema bilgisinden Table oluşturur.
     */
    private Table createTable(
            String tableName,
            List<Column> columns,
            Path tableFile
    ) {

        try {

            return new Table(
                    tableName,
                    columns
            );

        } catch (RuntimeException exception) {

            throw new CorruptedTableFileException(
                    "Invalid table schema in file: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Parse edilen fiziksel metadata bilgisinden
     * TableMetadata oluşturur.
     */
    private TableMetadata createMetadata(
            String tableName,
            int columnCount,
            LocalDateTime createdAt,
            Path tableFile,
            int version
    ) {

        try {

            return new TableMetadata(
                    tableName,
                    columnCount,
                    createdAt,
                    tableFile
                            .getFileName()
                            .toString(),
                    version
            );

        } catch (RuntimeException exception) {

            throw new CorruptedTableFileException(
                    "Invalid table metadata in file: "
                            + tableFile,
                    exception
            );
        }
    }
}