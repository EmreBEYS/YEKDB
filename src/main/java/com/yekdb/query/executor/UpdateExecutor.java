package com.yekdb.query.executor;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.ConstraintValidator;
import com.yekdb.constraint.ValidationContext;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.evaluator.WhereEvaluator;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * UPDATE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
 *
 * Sprint 00-13 kapsamında gelişmiş WHERE Expression Engine
 * ile entegre edilmiştir.
 *
 * Sprint 00-24 kapsamında:
 *
 * - NULL storage desteği
 * - Constraint validation desteği
 *
 * eklenmiştir.
 *
 * Desteklenen WHERE yapıları:
 *
 * - Comparison
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 *
 * İşlem akışı:
 *
 * UpdateCommand
 *      ↓
 * Active Records
 *      ↓
 * Record -> Row
 *      ↓
 * WhereEvaluator
 *      ↓
 * ExpressionEvaluator
 *      ↓
 * SET değerlerini Row üzerine uygula
 *      ↓
 * ConstraintValidator
 *      ↓
 * RecordManager.update(...)
 */
public final class UpdateExecutor {

    /**
     * UPDATE komutunu çalıştırır.
     *
     * @param table güncellenecek tablo
     * @param command UPDATE komutu
     * @param recordManager fiziksel kayıt yöneticisi
     * @return güncellenen satır sayısı
     */
    public int execute(
            Table table,
            UpdateCommand command,
            RecordManager recordManager
    ) throws IOException {

        return executeInternal(
                table,
                command,
                recordManager,
                null
        );
    }

    /**
     * Sprint 00-25 Phase 5:
     * FOREIGN KEY alanı güncelleniyorsa referenced table doğrulaması için
     * aktif TableManager bilgisini taşıyan UPDATE overload'udur.
     */
    public int execute(
            Table table,
            UpdateCommand command,
            RecordManager recordManager,
            TableManager tableManager
    ) throws IOException {

        return executeInternal(
                table,
                command,
                recordManager,
                Objects.requireNonNull(
                        tableManager,
                        "TableManager cannot be null."
                )
        );
    }

    private int executeInternal(
            Table table,
            UpdateCommand command,
            RecordManager recordManager,
            TableManager tableManager
    ) throws IOException {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                command,
                "UpdateCommand cannot be null."
        );

        Objects.requireNonNull(
                recordManager,
                "RecordManager cannot be null."
        );

        validateTargetTable(
                table,
                command
        );

        validateUpdatedColumns(
                table,
                command.getUpdatedValues()
        );

        List<Record> activeRecords =
                recordManager.getActiveRecords();

        List<ForeignKeyUpdateReferentialActionValidator.UpdateCandidate> candidates =
                new java.util.ArrayList<>();

        /*
         * Sprint 00-27 Phase 6:
         * Root UPDATE rows are collected and validated before any physical
         * mutation. This allows incoming FK actions to be planned against the
         * statement's old/new key values without leaving a partially updated
         * parent when RESTRICT or SET NULL validation fails.
         */
        for (Record record : activeRecords) {

            long recordId = record.getRecordId();
            Row currentRow = recordManager.getRow(recordId);

            if (!matchesWhere(
                    table,
                    currentRow,
                    command
            )) {
                continue;
            }

            Row updatedRow = createUpdatedRow(
                    table,
                    currentRow,
                    command.getUpdatedValues()
            );

            ConstraintValidator.validate(
                    new ValidationContext(
                            table,
                            updatedRow,
                            recordManager,
                            recordId
                    ),
                    table.getConstraints()
            );

            /*
             * Sprint 00-25 Phase 5:
             * Local FOREIGN KEY changes still validate that the newly
             * referenced parent exists.
             */
            if (updatesForeignKeyColumn(
                    table,
                    command.getUpdatedValues()
            )) {

                if (tableManager == null) {
                    throw new IllegalStateException(
                            "FOREIGN KEY UPDATE validation requires TableManager."
                    );
                }

                ForeignKeyInsertValidator.validate(
                        tableManager,
                        table,
                        updatedRow
                );
            }

            candidates.add(
                    new ForeignKeyUpdateReferentialActionValidator.UpdateCandidate(
                            recordId,
                            currentRow,
                            updatedRow
                    )
            );
        }

        ForeignKeyUpdateReferentialActionValidator.UpdatePlan updatePlan = null;

        if (tableManager != null && !candidates.isEmpty()) {
            updatePlan = ForeignKeyUpdateReferentialActionValidator.plan(
                    tableManager,
                    table,
                    candidates,
                    recordManager
            );
        }

        /*
         * Parent/root rows are changed only after all referential-action
         * planning and generated child-row constraint validation succeeded.
         */
        for (ForeignKeyUpdateReferentialActionValidator.UpdateCandidate candidate
                : candidates) {
            recordManager.update(
                    candidate.recordId(),
                    candidate.updatedRow()
            );
        }

        if (updatePlan != null) {
            ForeignKeyUpdateReferentialActionValidator.applyGeneratedMutations(
                    tableManager,
                    table,
                    recordManager,
                    updatePlan
            );
        }

        return candidates.size();
    }

    /**
     * Command içerisindeki tablo ile gerçek tablo
     * aynı tabloyu göstermelidir.
     */
    private void validateTargetTable(
            Table table,
            UpdateCommand command
    ) {

        if (!table.getTableName()
                .equalsIgnoreCase(
                        command.getTableName()
                )) {

            throw new IllegalArgumentException(
                    "UPDATE target table mismatch. Expected '" +
                            table.getTableName() +
                            "' but received '" +
                            command.getTableName() +
                            "'."
            );
        }
    }

    /**
     * SET bölümündeki bütün kolonların tabloda
     * bulunduğunu ve değer tiplerinin doğru olduğunu
     * kontrol eder.
     */
    private void validateUpdatedColumns(
            Table table,
            Map<String, Object> updatedValues
    ) {

        for (Map.Entry<String, Object> entry :
                updatedValues.entrySet()) {

            String columnName =
                    entry.getKey();

            if (!table.hasColumn(
                    columnName
            )) {

                throw new IllegalArgumentException(
                        "Column not found in table '" +
                                table.getTableName() +
                                "': " +
                                columnName
                );
            }

            Column column =
                    table.getColumn(
                            columnName
                    );

            validateValueType(
                    column,
                    entry.getValue()
            );
        }
    }

    /**
     * UPDATE SET listesinin herhangi bir FOREIGN KEY local kolonuna
     * dokunup dokunmadığını belirler. İlgisiz kolon güncellemelerinde
     * TableManager zorunluluğu oluşturulmaz.
     */
    private boolean updatesForeignKeyColumn(
            Table table,
            Map<String, Object> updatedValues
    ) {

        for (Constraint constraint : table.getConstraints()) {
            if (constraint.type() != ConstraintType.FOREIGN_KEY) {
                continue;
            }

            for (String foreignKeyColumn : constraint.columns()) {
                boolean updated =
                        updatedValues.keySet()
                                .stream()
                                .anyMatch(columnName ->
                                        columnName.equalsIgnoreCase(
                                                foreignKeyColumn
                                        )
                                );

                if (updated) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * WHERE ifadesini değerlendirir.
     *
     * WHERE yoksa tüm aktif satırlar eşleşir.
     *
     * WHERE varsa Sprint 00-13
     * Expression Engine kullanılır.
     */
    private boolean matchesWhere(
            Table table,
            Row row,
            UpdateCommand command
    ) {

        if (!command.hasWhereExpression()) {
            return true;
        }

        return WhereEvaluator.evaluate(
                command.getWhereExpression(),
                row,
                table
        );
    }

    /**
     * Mevcut Row'un kopyasını oluşturur ve
     * SET bölümündeki değerleri ilgili kolonlara uygular.
     */
    private Row createUpdatedRow(
            Table table,
            Row currentRow,
            Map<String, Object> updatedValues
    ) {

        Row updatedRow =
                new Row(
                        currentRow.getValues()
                );

        List<Column> columns =
                table.getColumns();

        for (Map.Entry<String, Object> entry :
                updatedValues.entrySet()) {

            String targetColumn =
                    entry.getKey();

            int columnIndex =
                    findColumnIndex(
                            columns,
                            targetColumn
                    );

            updatedRow.setValue(
                    columnIndex,
                    entry.getValue()
            );
        }

        return updatedRow;
    }

    /**
     * Fiziksel Row sırasındaki kolon indeksini bulur.
     *
     * Column isimleri case-insensitive karşılaştırılır.
     */
    private int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {

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
                "Column index could not be resolved: " +
                        columnName
        );
    }

    /**
     * UPDATE ile verilen yeni değerin kolon tipiyle
     * uyumlu olup olmadığını kontrol eder.
     *
     * NULL değer burada geçerli kabul edilir.
     * NOT NULL kontrolü ConstraintValidator
     * katmanında gerçekleştirilir.
     */
    private void validateValueType(
            Column column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        DataType dataType =
                column.getDataType();

        boolean valid =
                switch (dataType) {

                    case INT ->
                            value instanceof Integer;

                    case LONG ->
                            value instanceof Long;

                    case DOUBLE ->
                            value instanceof Double;

                    case BOOLEAN ->
                            value instanceof Boolean;

                    case STRING ->
                            value instanceof String;
                };

        if (!valid) {

            throw new IllegalArgumentException(
                    "Invalid value type for column '" +
                            column.getName() +
                            "'. Expected " +
                            dataType +
                            " but received " +
                            value.getClass()
                                    .getSimpleName() +
                            "."
            );
        }
    }
}