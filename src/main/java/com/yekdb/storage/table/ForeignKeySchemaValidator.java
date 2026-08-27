package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.exception.InvalidForeignKeyConstraintException;

import java.util.List;

/**
 * CREATE TABLE sırasında FOREIGN KEY şema ilişkilerini doğrular.
 *
 * <p>Sprint 00-25 Phase 3 kapsamında aşağıdaki kurallar uygulanır:</p>
 *
 * <ul>
 *     <li>Referenced table aktif katalogda bulunmalıdır.</li>
 *     <li>Referenced column'ların tamamı parent tabloda bulunmalıdır.</li>
 *     <li>Local ve referenced column veri tipleri birebir uyumlu olmalıdır.</li>
 *     <li>Referenced column grubu PRIMARY KEY veya UNIQUE olmalıdır.</li>
 * </ul>
 *
 * <p>Row-level referential integrity kontrolü bu sınıfın sorumluluğu değildir.
 * INSERT/UPDATE enforcement sonraki phase'lerde uygulanacaktır.</p>
 */
final class ForeignKeySchemaValidator {

    private ForeignKeySchemaValidator() {
        // Utility class.
    }

    static void validate(
            Table table,
            TableCatalog tableCatalog
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null."
            );
        }

        if (tableCatalog == null) {
            throw new IllegalArgumentException(
                    "Table catalog cannot be null."
            );
        }

        for (Constraint constraint :
                table.getConstraints()) {

            if (constraint.type()
                    != ConstraintType.FOREIGN_KEY) {
                continue;
            }

            if (!(constraint
                    instanceof ForeignKeyConstraint foreignKey)) {

                throw new InvalidForeignKeyConstraintException(
                        "FOREIGN_KEY constraint metadata must be represented by ForeignKeyConstraint."
                );
            }

            validateForeignKey(
                    table,
                    foreignKey,
                    tableCatalog
            );
        }
    }

    private static void validateForeignKey(
            Table childTable,
            ForeignKeyConstraint foreignKey,
            TableCatalog tableCatalog
    ) {

        String referencedTableName =
                foreignKey.referencedTableName();

        if (!tableCatalog.containsTable(
                referencedTableName
        )) {

            throw new InvalidForeignKeyConstraintException(
                    "FOREIGN KEY references unknown table: "
                            + referencedTableName
            );
        }

        Table referencedTable =
                tableCatalog.getTable(
                        referencedTableName
                );

        List<String> localColumnNames =
                foreignKey.columns();

        List<String> referencedColumnNames =
                foreignKey.referencedColumnNames();

        for (int index = 0;
             index < localColumnNames.size();
             index++) {

            String localColumnName =
                    localColumnNames.get(index);

            String referencedColumnName =
                    referencedColumnNames.get(index);

            Column localColumn =
                    findColumn(
                            childTable,
                            localColumnName,
                            "FOREIGN KEY references unknown local column: "
                    );

            Column referencedColumn =
                    findColumn(
                            referencedTable,
                            referencedColumnName,
                            "FOREIGN KEY references unknown column '"
                                    + referencedColumnName
                                    + "' in table '"
                                    + referencedTableName
                                    + "'. Column: "
                    );

            if (localColumn.getDataType()
                    != referencedColumn.getDataType()) {

                throw new InvalidForeignKeyConstraintException(
                        "FOREIGN KEY column type mismatch: "
                                + childTable.getTableName()
                                + "."
                                + localColumnName
                                + " ("
                                + localColumn.getDataType()
                                + ") references "
                                + referencedTable.getTableName()
                                + "."
                                + referencedColumnName
                                + " ("
                                + referencedColumn.getDataType()
                                + ")."
                );
            }
        }

        if (!isCandidateKey(
                referencedTable,
                referencedColumnNames
        )) {

            throw new InvalidForeignKeyConstraintException(
                    "FOREIGN KEY referenced columns must match a PRIMARY KEY or UNIQUE constraint: "
                            + referencedTable.getTableName()
                            + "("
                            + String.join(
                                    ",",
                                    referencedColumnNames
                            )
                            + ")"
            );
        }
    }

    private static Column findColumn(
            Table table,
            String columnName,
            String errorPrefix
    ) {

        return table.getColumns()
                .stream()
                .filter(column ->
                        column.getName()
                                .equalsIgnoreCase(
                                        columnName
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new InvalidForeignKeyConstraintException(
                                errorPrefix
                                        + columnName
                        )
                );
    }

    private static boolean isCandidateKey(
            Table referencedTable,
            List<String> referencedColumnNames
    ) {

        return referencedTable.getConstraints()
                .stream()
                .filter(constraint ->
                        constraint.type()
                                == ConstraintType.PRIMARY_KEY
                                || constraint.type()
                                == ConstraintType.UNIQUE
                )
                .anyMatch(constraint ->
                        sameColumns(
                                constraint.columns(),
                                referencedColumnNames
                        )
                );
    }

    private static boolean sameColumns(
            List<String> left,
            List<String> right
    ) {

        if (left.size() != right.size()) {
            return false;
        }

        for (String leftColumn : left) {

            boolean found =
                    right.stream()
                            .anyMatch(rightColumn ->
                                    leftColumn.equalsIgnoreCase(
                                            rightColumn
                                    )
                            );

            if (!found) {
                return false;
            }
        }

        return true;
    }
}
