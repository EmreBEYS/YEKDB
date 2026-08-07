package com.yekdb.query.executor;

import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * DELETE komutlarını fiziksel kayıtlar üzerinde çalıştırır.
 *
 * Sprint 00-12 kapsamında DELETE işlemi logical delete
 * yaklaşımıyla uygulanır. Record fiziksel olarak sayfadan
 * kaldırılmaz; RecordManager.delete(recordId) üzerinden
 * deleted flag değeri işaretlenir.
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

        int deletedRowCount = 0;

        for (Record record : activeRecords) {

            long recordId =
                    record.getRecordId();

            Row row =
                    recordManager.getRow(
                            recordId
                    );

            if (!matchesWhere(
                    table,
                    row,
                    command
            )) {
                continue;
            }

            recordManager.delete(
                    recordId
            );

            deletedRowCount++;
        }

        return deletedRowCount;
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
     * WHERE varsa mevcut TableScanExecutor altyapısı
     * tek satırlık liste üzerinde kullanılır.
     */
    private boolean matchesWhere(
            Table table,
            Row row,
            DeleteCommand command
    ) {

        if (!command.hasWhereExpression()) {
            return true;
        }

        QueryResult result =
                TableScanExecutor.execute(
                        table,
                        List.of(row),
                        command.getWhereExpression()
                );

        return !result.getRows().isEmpty();
    }
}