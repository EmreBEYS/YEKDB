package com.yekdb.query.command;

import com.yekdb.constraint.Constraint;
import com.yekdb.storage.table.Column;

import java.util.List;
import java.util.Objects;

/**
 * CREATE TABLE SQL komutunu temsil eder.
 *
 * Sprint 00-24 Phase 5 kapsamında CREATE TABLE constraint
 * tanımlarını da taşımaktadır.
 */
public final class CreateTableCommand implements Command {

    private final String tableName;
    private final List<Column> columns;
    private final List<Constraint> constraints;

    /**
     * Constraint içermeyen eski CREATE TABLE kullanımları için
     * backward-compatible constructor.
     */
    public CreateTableCommand(
            String tableName,
            List<Column> columns
    ) {
        this(tableName, columns, List.of());
    }

    /**
     * Constraint tanımlarıyla birlikte CREATE TABLE komutu oluşturur.
     */
    public CreateTableCommand(
            String tableName,
            List<Column> columns,
            List<Constraint> constraints
    ) {

        this.tableName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (this.tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        Objects.requireNonNull(
                columns,
                "Column list cannot be null."
        );

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Table must contain at least one column."
            );
        }

        Objects.requireNonNull(
                constraints,
                "Constraint list cannot be null."
        );

        if (columns.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Column list cannot contain null values."
            );
        }

        if (constraints.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Constraint list cannot contain null values."
            );
        }

        this.columns = List.copyOf(columns);
        this.constraints = List.copyOf(constraints);
    }

    public String getTableName() {
        return tableName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public int getConstraintCount() {
        return constraints.size();
    }

    public boolean hasConstraints() {
        return !constraints.isEmpty();
    }

    @Override
    public String toString() {
        return "CreateTableCommand{" +
                "tableName='" + tableName + '\'' +
                ", columnCount=" + columns.size() +
                ", constraintCount=" + constraints.size() +
                '}';
    }
}
