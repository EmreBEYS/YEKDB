package com.yekdb.query.executor;

import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.evaluator.WhereEvaluator;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * DELETE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
 *
 * Sprint 00-13 kapsamında gelişmiş WHERE Expression Engine
 * ile entegre edilmiştir.
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
 * DELETE işlemi logical delete yaklaşımıyla uygulanır.
 * Record fiziksel olarak sayfadan kaldırılmaz;
 * RecordManager.delete(recordId) üzerinden deleted flag işaretlenir.
 */
public final class DeleteExecutor {

    /**
     * DELETE komutunu çalıştırır.
     *
     * @param table         hedef tablo
     * @param command       DELETE komutu
     * @param recordManager fiziksel kayıt yöneticisi
     * @return silinen aktif satır sayısı
     */
    public int execute(
            Table table,
            DeleteCommand command,
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
     * Sprint 00-25 Phase 6:
     * Parent row FOREIGN KEY ile referanslanıyorsa DELETE RESTRICT kontrolü
     * için aktif TableManager bilgisini taşıyan overload.
     */
    public int execute(
            Table table,
            DeleteCommand command,
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
            DeleteCommand command,
            RecordManager recordManager,
            TableManager tableManager
    ) throws IOException {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                command,
                "DeleteCommand cannot be null."
        );

        Objects.requireNonNull(
                recordManager,
                "RecordManager cannot be null."
        );

        validateTargetTable(
                table,
                command
        );

        List<Record> activeRecords =
                recordManager.getActiveRecords();

        List<ForeignKeyDeleteRestrictValidator.DeleteCandidate> candidates =
                new java.util.ArrayList<>();

        for (Record record : activeRecords) {

            long recordId = record.getRecordId();
            Row row = recordManager.getRow(recordId);

            if (!matchesWhere(
                    table,
                    row,
                    command
            )) {
                continue;
            }

            candidates.add(
                    new ForeignKeyDeleteRestrictValidator.DeleteCandidate(
                            recordId,
                            row
                    )
            );
        }

        /*
         * Validation tüm fiziksel delete işlemlerinden önce yapılır.
         * Böylece çok satırlı DELETE sırasında bir satır FK tarafından
         * referanslanıyorsa statement kısmi silme yapmadan reddedilir.
         */
        if (tableManager != null) {
            ForeignKeyDeleteRestrictValidator.validate(
                    tableManager,
                    table,
                    candidates,
                    recordManager
            );
        }

        for (ForeignKeyDeleteRestrictValidator.DeleteCandidate candidate
                : candidates) {
            recordManager.delete(candidate.recordId());
        }

        return candidates.size();
    }

    /**
     * Command içerisindeki tablo adı ile gerçek tablo
     * adının eşleştiğini doğrular.
     */
    private void validateTargetTable(
            Table table,
            DeleteCommand command
    ) {

        if (!table.getTableName()
                .equalsIgnoreCase(
                        command.getTableName()
                )) {

            throw new IllegalArgumentException(
                    "DELETE target table mismatch. Expected '"
                            + table.getTableName()
                            + "' but received '"
                            + command.getTableName()
                            + "'."
            );
        }
    }

    /**
     * WHERE yoksa bütün aktif kayıtlar eşleşir.
     *
     * WHERE varsa Sprint 00-13 Expression Engine
     * doğrudan WhereEvaluator üzerinden kullanılır.
     */
    private boolean matchesWhere(
            Table table,
            Row row,
            DeleteCommand command
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
}