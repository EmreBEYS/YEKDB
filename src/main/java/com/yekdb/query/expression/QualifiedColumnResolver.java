package com.yekdb.query.expression;

import com.yekdb.query.exception.AmbiguousColumnException;
import com.yekdb.query.exception.UnknownColumnException;
import com.yekdb.query.executor.JoinedRow;

import java.util.Map;
import java.util.Objects;

public final class QualifiedColumnResolver {

    public Object resolve(
            JoinedRow row,
            String qualifier,
            String columnName
    ) {
        Objects.requireNonNull(row, "row cannot be null");
        Objects.requireNonNull(columnName, "columnName cannot be null");

        if (columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "columnName cannot be blank"
            );
        }

        /*
         * Qualified column:
         *
         * e.id
         * d.name
         */
        if (qualifier != null && !qualifier.isBlank()) {

            String qualifiedName =
                    qualifier + "." + columnName;

            if (!row.contains(qualifiedName)) {
                throw new UnknownColumnException(
                        qualifiedName
                );
            }

            return row.get(qualifiedName);
        }

        /*
         * Unqualified column:
         *
         * id
         * name
         *
         * We search:
         *
         * e.id
         * d.id
         *
         * If more than one exists -> ambiguous.
         */

        Object resolvedValue = null;
        int matchCount = 0;

        String suffix = "." + columnName;

        for (Map.Entry<String, Object> entry
                : row.getValues().entrySet()) {

            if (entry.getKey().endsWith(suffix)) {

                resolvedValue = entry.getValue();
                matchCount++;

                if (matchCount > 1) {
                    throw new AmbiguousColumnException(
                            columnName
                    );
                }
            }
        }

        if (matchCount == 0) {
            throw new UnknownColumnException(
                    columnName
            );
        }

        return resolvedValue;
    }
}