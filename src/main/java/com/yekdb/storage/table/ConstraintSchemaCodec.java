package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
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

        String columnsPart =
                line.substring(
                        separatorIndex + 1
                ).trim();

        List<String> columns =
                parseColumns(
                        columnsPart,
                        line,
                        tableFile
                );

        try {

            ConstraintType type =
                    ConstraintType.valueOf(
                            typeName
                    );

            return switch (type) {

                case NOT_NULL -> {

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
                                columns
                        );

                case PRIMARY_KEY ->
                        new PrimaryKeyConstraint(
                                columns
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
