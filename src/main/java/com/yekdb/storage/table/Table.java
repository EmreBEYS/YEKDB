package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.storage.exception.DuplicateColumnException;
import com.yekdb.storage.exception.InvalidColumnException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * YEKDB içerisinde bir tablo şemasını temsil eder.
 *
 * Bir tablo:
 *
 * - tablo adı,
 * - sütun tanımları,
 * - tablo constraint tanımları
 *
 * bilgilerinden oluşur.
 *
 * Bu sınıf fiziksel kayıtları değil yalnızca tablo şemasını
 * temsil eder.
 *
 * Table nesnesi oluşturulduğunda tüm temel şema kurallarının
 * geçerli olduğu garanti edilir.
 *
 * Sprint 00-24 kapsamında constraint metadata desteği
 * eklenmiştir.
 *
 * Desteklenen constraint tipleri:
 *
 * - NOT NULL
 * - UNIQUE
 * - PRIMARY KEY
 */
public class Table {

    private final String tableName;

    private final List<Column> columns;

    private final List<Constraint> constraints;

    /**
     * Constraint içermeyen yeni bir tablo oluşturur.
     *
     * Bu constructor eski Table API'si ile backward-compatible
     * davranış sağlamak için korunmaktadır.
     *
     * @param tableName tablo adı
     * @param columns tablo sütunları
     */
    public Table(
            String tableName,
            List<Column> columns
    ) {

        this(
                tableName,
                columns,
                List.of()
        );
    }

    /**
     * Constraint tanımlarıyla birlikte yeni bir tablo oluşturur.
     *
     * @param tableName tablo adı
     * @param columns tablo sütunları
     * @param constraints tablo constraint listesi
     */
    public Table(
            String tableName,
            List<Column> columns,
            List<Constraint> constraints
    ) {

        this.tableName =
                TableNameValidator.validate(
                        tableName
                );

        validateColumns(
                columns
        );

        validateConstraints(
                columns,
                constraints
        );

        /*
         * List.copyOf sayesinde dışarıdan verilen listelerin
         * sonradan değiştirilmesi Table nesnesini etkileyemez.
         *
         * Column ve Constraint listelerinin kendisinde null
         * eleman bulunmasına validation aşamasında izin verilmez.
         */
        this.columns =
                List.copyOf(columns);

        this.constraints =
                List.copyOf(constraints);
    }

    /**
     * Sütun listesinin tablo kurallarına uygun olduğunu doğrular.
     *
     * @param columns sütun listesi
     */
    private void validateColumns(
            List<Column> columns
    ) {

        if (columns == null ||
                columns.isEmpty()) {

            throw new InvalidColumnException(
                    "Table must contain at least one column."
            );
        }

        if (columns.stream()
                .anyMatch(Objects::isNull)) {

            throw new InvalidColumnException(
                    "Column list cannot contain null values."
            );
        }

        Set<String> columnNames =
                new HashSet<>();

        for (Column column : columns) {

            String normalizedColumnName =
                    column.getName()
                            .toLowerCase();

            if (!columnNames.add(
                    normalizedColumnName
            )) {

                throw new DuplicateColumnException(
                        "Duplicate column names are not allowed."
                );
            }
        }
    }

    /**
     * Constraint listesinin geçerli olduğunu doğrular.
     *
     * Doğrulanan temel kurallar:
     *
     * - Constraint listesi null olamaz.
     * - Constraint listesi null eleman içeremez.
     * - Constraint içerisinde en az bir column bulunmalıdır.
     * - Constraint yalnızca tabloda bulunan column'ları
     *   referanslayabilir.
     * - Bir tabloda birden fazla PRIMARY KEY bulunamaz.
     *
     * NOT NULL tek kolonlu olmak zorundadır.
     *
     * UNIQUE ve PRIMARY KEY ise composite column listesi
     * taşıyabilir.
     *
     * @param columns tablo sütunları
     * @param constraints tablo constraint listesi
     */
    private void validateConstraints(
            List<Column> columns,
            List<Constraint> constraints
    ) {

        if (constraints == null) {

            throw new IllegalArgumentException(
                    "Constraint list cannot be null."
            );
        }

        if (constraints.stream()
                .anyMatch(Objects::isNull)) {

            throw new IllegalArgumentException(
                    "Constraint list cannot contain null values."
            );
        }

        int primaryKeyCount = 0;

        for (Constraint constraint :
                constraints) {

            validateConstraintColumns(
                    columns,
                    constraint
            );

            if (constraint.type()
                    == ConstraintType.PRIMARY_KEY) {

                primaryKeyCount++;

                if (primaryKeyCount > 1) {

                    throw new IllegalArgumentException(
                            "Table cannot contain more than one PRIMARY KEY constraint."
                    );
                }
            }

            if (constraint.type()
                    == ConstraintType.NOT_NULL &&
                    constraint.columns().size() != 1) {

                throw new IllegalArgumentException(
                        "NOT NULL constraint must reference exactly one column."
                );
            }
        }
    }

    /**
     * Constraint içerisindeki bütün sütunların tabloda
     * mevcut olduğunu doğrular.
     *
     * @param columns tablo sütunları
     * @param constraint doğrulanacak constraint
     */
    private void validateConstraintColumns(
            List<Column> columns,
            Constraint constraint
    ) {

        List<String> constraintColumns =
                constraint.columns();

        if (constraintColumns == null ||
                constraintColumns.isEmpty()) {

            throw new IllegalArgumentException(
                    "Constraint must reference at least one column."
            );
        }

        for (String constraintColumn :
                constraintColumns) {

            boolean exists =
                    columns.stream()
                            .anyMatch(column ->
                                    column.getName()
                                            .equalsIgnoreCase(
                                                    constraintColumn
                                            )
                            );

            if (!exists) {

                throw new IllegalArgumentException(
                        "Constraint references unknown column: "
                                + constraintColumn
                );
            }
        }
    }

    /**
     * Tablo adını döndürür.
     *
     * @return tablo adı
     */
    public String getTableName() {

        return tableName;
    }

    /**
     * Değiştirilemez sütun listesini döndürür.
     *
     * @return sütun listesi
     */
    public List<Column> getColumns() {

        return columns;
    }

    /**
     * Tablo constraint listesini döndürür.
     *
     * Liste immutable olarak saklanmaktadır.
     *
     * @return constraint listesi
     */
    public List<Constraint> getConstraints() {

        return constraints;
    }

    /**
     * Belirtilen tipteki constraint'leri döndürür.
     *
     * Örneğin:
     *
     * table.getConstraints(ConstraintType.NOT_NULL)
     *
     * @param type constraint tipi
     * @return eşleşen constraint listesi
     */
    public List<Constraint> getConstraints(
            ConstraintType type
    ) {

        Objects.requireNonNull(
                type,
                "Constraint type cannot be null."
        );

        return constraints.stream()
                .filter(constraint ->
                        constraint.type() == type
                )
                .toList();
    }

    /**
     * Tabloda verilen tipte constraint olup olmadığını
     * kontrol eder.
     *
     * @param type constraint tipi
     * @return constraint varsa true
     */
    public boolean hasConstraint(
            ConstraintType type
    ) {

        Objects.requireNonNull(
                type,
                "Constraint type cannot be null."
        );

        return constraints.stream()
                .anyMatch(constraint ->
                        constraint.type() == type
                );
    }

    /**
     * Tablodaki sütun sayısını döndürür.
     *
     * @return sütun sayısı
     */
    public int getColumnCount() {

        return columns.size();
    }

    /**
     * Verilen isimde bir sütunun tabloda bulunup bulunmadığını
     * kontrol eder.
     *
     * @param columnName sütun adı
     * @return sütun varsa true
     */
    public boolean hasColumn(
            String columnName
    ) {

        if (columnName == null ||
                columnName.isBlank()) {

            return false;
        }

        String normalizedColumnName;

        try {

            normalizedColumnName =
                    ColumnNameValidator.validate(
                            columnName
                    );

        } catch (InvalidColumnException exception) {

            return false;
        }

        return columns.stream()
                .anyMatch(column ->
                        column.getName()
                                .equalsIgnoreCase(
                                        normalizedColumnName
                                )
                );
    }

    /**
     * Verilen isimdeki sütunu döndürür.
     *
     * @param columnName sütun adı
     * @return sütun
     */
    public Column getColumn(
            String columnName
    ) {

        String normalizedColumnName =
                ColumnNameValidator.validate(
                        columnName
                );

        return columns.stream()
                .filter(column ->
                        column.getName()
                                .equalsIgnoreCase(
                                        normalizedColumnName
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Column not found: "
                                        + normalizedColumnName
                        )
                );
    }

    @Override
    public String toString() {

        return "Table{" +
                "tableName='" +
                tableName +
                '\'' +
                ", columns=" +
                columns +
                ", constraints=" +
                constraints +
                '}';
    }

    @Override
    public boolean equals(
            Object object
    ) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof Table table)) {
            return false;
        }

        return tableName.equals(
                table.tableName
        )
                && columns.equals(
                table.columns
        )
                && constraints.equals(
                table.constraints
        );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                tableName,
                columns,
                constraints
        );
    }
}