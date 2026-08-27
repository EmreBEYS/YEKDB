package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.storage.exception.CorruptedTableFileException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Table schema dosyasında constraint metadata bilgisinin
 * metinsel encode/decode işlemlerini gerçekleştirir.
 *
 * Sprint 00-24 Phase 6.
 *
 * Fiziksel format:
 *
 * constraints=
 * NOT_NULL:email
 * UNIQUE:username
 * PRIMARY_KEY:id
 * UNIQUE:first_name,last_name
 * PRIMARY_KEY:student_id,course_id
 * FOREIGN_KEY:user_id->users:id
 * FOREIGN_KEY:country_code,city_code->cities:country_code,city_code
 *
 * Constraint section eski tablo dosyalarında bulunmayabilir.
 * Bu durumda boş constraint listesi döndürülür ve backward
 * compatibility korunur.
 */
final class ConstraintSchemaCodec {

    static final String CONSTRAINTS_HEADER = "constraints=";

    private ConstraintSchemaCodec() {
        // Utility class.
    }

    static List<String> serialize(
            List<Constraint> constraints
    ) {

        Objects.requireNonNull(
                constraints,
                "constraints cannot be null"
        );

        List<String> lines =
                new ArrayList<>(constraints.size() + 1);

        lines.add(CONSTRAINTS_HEADER);

        for (Constraint constraint : constraints) {

            Objects.requireNonNull(
                    constraint,
                    "constraint cannot be null"
            );

            String columns =
                    constraint.columns()
                            .stream()
                            .collect(
                                    Collectors.joining(",")
                            );

            if (constraint.type() == ConstraintType.FOREIGN_KEY) {

                if (!(constraint instanceof ForeignKeyConstraint foreignKey)) {
                    throw new IllegalArgumentException(
                            "FOREIGN_KEY constraint must be an instance of ForeignKeyConstraint"
                    );
                }

                String referencedColumns =
                        foreignKey.referencedColumnNames()
                                .stream()
                                .collect(
                                        Collectors.joining(",")
                                );

                lines.add(
                        ConstraintType.FOREIGN_KEY.name()
                                + ":"
                                + columns
                                + "->"
                                + foreignKey.referencedTableName()
                                + ":"
                                + referencedColumns
                );

                continue;
            }

            lines.add(
                    constraint.type().name()
                            + ":"
                            + columns
            );
        }

        return List.copyOf(lines);
    }

    static List<Constraint> deserialize(
            List<String> lines,
            Path tableFile
    ) {

        Objects.requireNonNull(
                lines,
                "lines cannot be null"
        );

        int headerIndex =
                findConstraintsHeader(lines);

        if (headerIndex < 0) {
            return List.of();
        }

        List<Constraint> constraints =
                new ArrayList<>();

        for (int index = headerIndex + 1;
             index < lines.size();
             index++) {

            String line =
                    lines.get(index).trim();

            if (line.isEmpty()) {
                continue;
            }

            constraints.add(
                    parseConstraint(
                            line,
                            tableFile
                    )
            );
        }

        return List.copyOf(constraints);
    }

    static int findConstraintsHeader(
            List<String> lines
    ) {

        for (int index = 0;
             index < lines.size();
             index++) {

            if (CONSTRAINTS_HEADER.equals(
                    lines.get(index).trim()
            )) {
                return index;
            }
        }

        return -1;
    }

    private static Constraint parseConstraint(
            String line,
            Path tableFile
    ) {

        int separatorIndex =
                line.indexOf(':');

        if (separatorIndex <= 0
                || separatorIndex
                == line.length() - 1) {

            throw corrupted(
                    "Invalid constraint definition: "
                            + line,
                    tableFile,
                    null
            );
        }

        String typeName =
                line.substring(
                        0,
                        separatorIndex
                ).trim();

        String definitionPart =
                line.substring(
                        separatorIndex + 1
                ).trim();

        try {

            ConstraintType type =
                    ConstraintType.valueOf(
                            typeName
                    );

            return switch (type) {

                case NOT_NULL -> {

                    List<String> columns =
                            parseColumns(
                                    definitionPart,
                                    line,
                                    tableFile
                            );

                    if (columns.size() != 1) {
                        throw corrupted(
                                "NOT NULL constraint must reference exactly one column: "
                                        + line,
                                tableFile,
                                null
                        );
                    }

                    yield new NotNullConstraint(
                            columns.getFirst()
                    );
                }

                case UNIQUE ->
                        new UniqueConstraint(
                                parseColumns(
                                        definitionPart,
                                        line,
                                        tableFile
                                )
                        );

                case PRIMARY_KEY ->
                        new PrimaryKeyConstraint(
                                parseColumns(
                                        definitionPart,
                                        line,
                                        tableFile
                                )
                        );

                case FOREIGN_KEY ->
                        parseForeignKeyConstraint(
                                definitionPart,
                                line,
                                tableFile
                        );
            };

        } catch (CorruptedTableFileException exception) {
            throw exception;

        } catch (IllegalArgumentException exception) {

            throw corrupted(
                    "Invalid constraint definition: "
                            + line,
                    tableFile,
                    exception
            );
        }
    }

    private static ForeignKeyConstraint parseForeignKeyConstraint(
            String definitionPart,
            String sourceLine,
            Path tableFile
    ) {

        int arrowIndex =
                definitionPart.indexOf("->");

        if (arrowIndex <= 0
                || arrowIndex == definitionPart.length() - 2
                || definitionPart.indexOf("->", arrowIndex + 2) >= 0) {

            throw corrupted(
                    "Invalid FOREIGN KEY definition: "
                            + sourceLine,
                    tableFile,
                    null
            );
        }

        String localColumnsPart =
                definitionPart.substring(
                        0,
                        arrowIndex
                ).trim();

        String referencePart =
                definitionPart.substring(
                        arrowIndex + 2
                ).trim();

        int referenceSeparatorIndex =
                referencePart.indexOf(':');

        if (referenceSeparatorIndex <= 0
                || referenceSeparatorIndex
                == referencePart.length() - 1) {

            throw corrupted(
                    "Invalid FOREIGN KEY reference definition: "
                            + sourceLine,
                    tableFile,
                    null
            );
        }

        String referencedTableName =
                referencePart.substring(
                        0,
                        referenceSeparatorIndex
                ).trim();

        String referencedColumnsPart =
                referencePart.substring(
                        referenceSeparatorIndex + 1
                ).trim();

        if (referencedTableName.isBlank()) {
            throw corrupted(
                    "FOREIGN KEY referenced table name cannot be empty: "
                            + sourceLine,
                    tableFile,
                    null
            );
        }

        List<String> localColumns =
                parseColumns(
                        localColumnsPart,
                        sourceLine,
                        tableFile
                );

        List<String> referencedColumns =
                parseColumns(
                        referencedColumnsPart,
                        sourceLine,
                        tableFile
                );

        if (localColumns.size() != referencedColumns.size()) {
            throw corrupted(
                    "FOREIGN KEY local/referenced column count mismatch: "
                            + sourceLine,
                    tableFile,
                    null
            );
        }

        return new ForeignKeyConstraint(
                localColumns,
                referencedTableName,
                referencedColumns
        );
    }

    private static List<String> parseColumns(
            String columnsPart,
            String sourceLine,
            Path tableFile
    ) {

        if (columnsPart.isBlank()) {
            throw corrupted(
                    "Constraint column list cannot be empty: "
                            + sourceLine,
                    tableFile,
                    null
            );
        }

        String[] tokens =
                columnsPart.split(",", -1);

        List<String> columns =
                new ArrayList<>(tokens.length);

        for (String token : tokens) {

            String column =
                    token.trim();

            if (column.isEmpty()) {
                throw corrupted(
                        "Constraint contains an empty column name: "
                                + sourceLine,
                        tableFile,
                        null
                );
            }

            columns.add(column);
        }

        return List.copyOf(columns);
    }

    private static CorruptedTableFileException corrupted(
            String message,
            Path tableFile,
            Throwable cause
    ) {

        String fullMessage =
                tableFile == null
                        ? message
                        : message + " in " + tableFile;

        if (cause == null) {
            return new CorruptedTableFileException(
                    fullMessage
            );
        }

        return new CorruptedTableFileException(
                fullMessage,
                cause
        );
    }
}
