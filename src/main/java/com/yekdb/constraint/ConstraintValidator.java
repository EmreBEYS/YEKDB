package com.yekdb.constraint;

import com.yekdb.constraint.exception.NotNullConstraintViolationException;
import com.yekdb.constraint.exception.PrimaryKeyConstraintViolationException;
import com.yekdb.constraint.exception.UniqueConstraintViolationException;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB constraint validation merkezi.
 *
 * Sprint 00-24 kapsamında:
 *
 * - NOT NULL
 * - UNIQUE
 *
 * constraint enforcement işlemleri burada gerçekleştirilir.
 *
 * İlerleyen phase'de PRIMARY KEY desteği de aynı merkezi
 * validator üzerinden çalışacaktır.
 */
public final class ConstraintValidator {

    private ConstraintValidator() {
        // Utility class.
    }

    /**
     * Verilen Row'u tablo constraint'lerine göre doğrular.
     *
     * @param context validation context
     * @param constraints tablo constraint listesi
     */
    public static void validate(
            ValidationContext context,
            List<Constraint> constraints
    ) {

        Objects.requireNonNull(
                context,
                "context cannot be null"
        );

        Objects.requireNonNull(
                constraints,
                "constraints cannot be null"
        );

        for (Constraint constraint :
                constraints) {

            Objects.requireNonNull(
                    constraint,
                    "constraint cannot be null"
            );

            switch (constraint.type()) {

                case NOT_NULL ->
                        validateNotNull(
                                context,
                                constraint
                        );

                case UNIQUE ->
                        validateUnique(
                                context,
                                constraint
                        );

                case PRIMARY_KEY ->
                        validatePrimaryKey(
                                context,
                                constraint
                        );
            }
        }
    }
    private static void validatePrimaryKey(
            ValidationContext context,
            Constraint constraint
    ) {

        if (!context.hasRecordManager()) {

            throw new IllegalStateException(
                    "PRIMARY KEY validation requires RecordManager."
            );
        }

        Table table =
                context.table();

        Row candidateRow =
                context.row();

        List<String> constraintColumns =
                constraint.columns();

        List<Integer> columnIndexes =
                resolveColumnIndexes(
                        table,
                        constraintColumns
                );

        /*
         * PRIMARY KEY hiçbir bileşeninde NULL
         * değer kabul etmez.
         *
         * Composite PK için de aynı kural geçerlidir:
         *
         * (student_id, course_id)
         *
         * student_id = NULL  -> invalid
         * course_id  = NULL  -> invalid
         */
        if (containsNull(
                candidateRow,
                columnIndexes
        )) {

            throw new PrimaryKeyConstraintViolationException(
                    constraintColumns,
                    "PRIMARY KEY cannot contain NULL values."
            );
        }

        RecordManager recordManager =
                context.recordManager();

        final List<Record> activeRecords;

        try {

            activeRecords =
                    recordManager.getActiveRecords();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to read active records during PRIMARY KEY validation.",
                    exception
            );
        }

        for (Record record :
                activeRecords) {

            long recordId =
                    record.getRecordId();

            /*
             * UPDATE sırasında mevcut kayıt
             * kendi PRIMARY KEY değeriyle
             * karşılaştırılmamalıdır.
             */
            if (context.shouldIgnoreRecord(
                    recordId
            )) {

                continue;
            }

            final Row existingRow;

            try {

                existingRow =
                        recordManager.getRow(
                                recordId
                        );

            } catch (IOException exception) {

                throw new IllegalStateException(
                        "Failed to read record "
                                + recordId
                                + " during PRIMARY KEY validation.",
                        exception
                );
            }

            /*
             * Normal şartlarda mevcut PRIMARY KEY
             * kayıtlarında NULL bulunmamalıdır.
             *
             * Yine de corrupted/legacy veri varsa
             * duplicate comparison açısından atlıyoruz.
             */
            if (containsNull(
                    existingRow,
                    columnIndexes
            )) {

                continue;
            }

            if (matchesUniqueValues(
                    candidateRow,
                    existingRow,
                    columnIndexes
            )) {

                throw new PrimaryKeyConstraintViolationException(
                        constraintColumns,
                        "Duplicate PRIMARY KEY value."
                );
            }
        }
    }

    /**
     * NOT NULL constraint doğrulaması.
     */
    private static void validateNotNull(
            ValidationContext context,
            Constraint constraint
    ) {

        Table table =
                context.table();

        Row row =
                context.row();

        String columnName =
                constraint.columns()
                        .getFirst();

        int columnIndex =
                findColumnIndex(
                        table,
                        columnName
                );

        Object value =
                row.getValue(
                        columnIndex
                );

        if (value == null) {

            throw new NotNullConstraintViolationException(
                    columnName
            );
        }
    }

    /**
     * UNIQUE constraint doğrulaması.
     *
     * Kurallar:
     *
     * - Tek kolonlu UNIQUE desteklenir.
     * - Composite UNIQUE desteklenir.
     * - Constraint kolonlarından herhangi biri NULL ise
     *   kontrol atlanır.
     * - UPDATE sırasında güncellenmekte olan mevcut
     *   record kendisiyle karşılaştırılmaz.
     */
    private static void validateUnique(
            ValidationContext context,
            Constraint constraint
    ) {

        if (!context.hasRecordManager()) {

            throw new IllegalStateException(
                    "UNIQUE validation requires RecordManager."
            );
        }

        Table table =
                context.table();

        Row candidateRow =
                context.row();

        List<String> constraintColumns =
                constraint.columns();

        List<Integer> columnIndexes =
                resolveColumnIndexes(
                        table,
                        constraintColumns
                );

        /*
         * SQL UNIQUE semantiğine yakın davranış:
         *
         * UNIQUE constraint kolonlarından en az biri NULL ise
         * duplicate comparison yapılmaz.
         *
         * Örn:
         *
         * UNIQUE(email)
         *
         * NULL
         * NULL
         *
         * kabul edilir.
         */
        if (containsNull(
                candidateRow,
                columnIndexes
        )) {

            return;
        }

        RecordManager recordManager =
                context.recordManager();

        final List<Record> activeRecords;

        try {

            activeRecords =
                    recordManager
                            .getActiveRecords();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to read active records during UNIQUE validation.",
                    exception
            );
        }

        for (Record record :
                activeRecords) {

            long recordId =
                    record.getRecordId();

            /*
             * UPDATE sırasında row kendi fiziksel record'u
             * ile duplicate kabul edilmemelidir.
             */
            if (context.shouldIgnoreRecord(
                    recordId
            )) {

                continue;
            }

            final Row existingRow;

            try {

                existingRow =
                        recordManager.getRow(
                                recordId
                        );

            } catch (IOException exception) {

                throw new IllegalStateException(
                        "Failed to read record " +
                                recordId +
                                " during UNIQUE validation.",
                        exception
                );
            }

            if (containsNull(
                    existingRow,
                    columnIndexes
            )) {

                continue;
            }

            if (matchesUniqueValues(
                    candidateRow,
                    existingRow,
                    columnIndexes
            )) {

                throw new UniqueConstraintViolationException(
                        constraintColumns
                );
            }
        }
    }

    /**
     * Constraint kolon isimlerini fiziksel Row indekslerine
     * dönüştürür.
     */
    private static List<Integer> resolveColumnIndexes(
            Table table,
            List<String> columnNames
    ) {

        List<Integer> indexes =
                new ArrayList<>(
                        columnNames.size()
                );

        for (String columnName :
                columnNames) {

            indexes.add(
                    findColumnIndex(
                            table,
                            columnName
                    )
            );
        }

        return indexes;
    }

    /**
     * Constraint kapsamındaki değerlerden herhangi birinin
     * NULL olup olmadığını kontrol eder.
     */
    private static boolean containsNull(
            Row row,
            List<Integer> columnIndexes
    ) {

        for (int columnIndex :
                columnIndexes) {

            if (row.getValue(
                    columnIndex
            ) == null) {

                return true;
            }
        }

        return false;
    }

    /**
     * Candidate Row ile mevcut Row'un UNIQUE constraint
     * kapsamındaki bütün değerlerinin aynı olup olmadığını
     * kontrol eder.
     */
    private static boolean matchesUniqueValues(
            Row candidateRow,
            Row existingRow,
            List<Integer> columnIndexes
    ) {

        for (int columnIndex :
                columnIndexes) {

            Object candidateValue =
                    candidateRow.getValue(
                            columnIndex
                    );

            Object existingValue =
                    existingRow.getValue(
                            columnIndex
                    );

            if (!Objects.equals(
                    candidateValue,
                    existingValue
            )) {

                return false;
            }
        }

        return true;
    }

    /**
     * Tablo içerisindeki fiziksel column indeksini bulur.
     */
    private static int findColumnIndex(
            Table table,
            String columnName
    ) {

        List<Column> columns =
                table.getColumns();

        for (int index = 0;
             index < columns.size();
             index++) {

            if (columns.get(index)
                    .getName()
                    .equalsIgnoreCase(
                            columnName
                    )) {

                return index;
            }
        }

        throw new IllegalArgumentException(
                "Constraint references unknown column: "
                        + columnName
        );
    }
}