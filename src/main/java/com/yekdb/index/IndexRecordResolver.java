package com.yekdb.index;

import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordId;
import com.yekdb.storage.record.RecordManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Index ve physical RecordManager katmanları arasındaki entegrasyon köprüsüdür.
 *
 * <p>Index aramasından dönen fiziksel RecordId değerlerini RecordManager
 * üzerinden gerçek Record nesnelerine çözümler. RecordManager'ın index
 * paketine bağımlı olmasını önleyerek katman yönünü index -> storage olarak
 * korur.</p>
 */
public final class IndexRecordResolver {

    private final IndexManager indexManager;
    private final RecordManager recordManager;

    public IndexRecordResolver(
            IndexManager indexManager,
            RecordManager recordManager
    ) {
        this.indexManager = Objects.requireNonNull(
                indexManager,
                "IndexManager cannot be null."
        );
        this.recordManager = Objects.requireNonNull(
                recordManager,
                "RecordManager cannot be null."
        );
    }

    /**
     * Index anahtarına bağlı aktif kayıtları fiziksel RecordId üzerinden çözer.
     */
    public <K extends Comparable<K>> List<Record> resolve(
            String indexName,
            K key
    ) throws IOException {

        List<RecordId> recordIds =
                indexManager.searchRecordIds(
                        indexName,
                        key
                );

        List<Record> records =
                new ArrayList<>(recordIds.size());

        for (RecordId recordId : recordIds) {
            records.add(
                    recordManager.readRecord(recordId)
            );
        }

        return List.copyOf(records);
    }
}
