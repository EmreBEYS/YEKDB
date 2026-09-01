package com.yekdb.trigger;

import com.yekdb.trigger.exception.DuplicateTriggerException;
import com.yekdb.trigger.exception.TriggerNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aktif veritabanındaki trigger tanımlarını ve metadata kayıtlarını yönetir.
 */
public class TriggerCatalog {

    private final Map<String, TriggerDefinition> triggers;
    private final Map<String, TriggerMetadata> metadataEntries;

    public TriggerCatalog() {
        this.triggers = new LinkedHashMap<>();
        this.metadataEntries = new LinkedHashMap<>();
    }

    public void registerTrigger(
            TriggerDefinition definition,
            TriggerMetadata metadata
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Trigger definition cannot be null."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Trigger metadata cannot be null."
            );
        }

        String triggerName =
                TriggerNameValidator.validate(
                        definition.getTriggerName()
                );

        String metadataTriggerName =
                TriggerNameValidator.validate(
                        metadata.getTriggerName()
                );

        if (!triggerName.equals(metadataTriggerName)) {
            throw new IllegalArgumentException(
                    "Trigger definition name and metadata trigger name must match."
            );
        }

        if (!definition.getTableName()
                .equals(metadata.getTableName())) {
            throw new IllegalArgumentException(
                    "Trigger definition table and metadata table must match."
            );
        }

        if (triggers.containsKey(triggerName)) {
            throw new DuplicateTriggerException(
                    "Trigger already exists: " + triggerName
            );
        }

        triggers.put(triggerName, definition);
        metadataEntries.put(triggerName, metadata);
    }

    public TriggerDefinition unregisterTrigger(String triggerName) {
        String normalizedName =
                TriggerNameValidator.validate(triggerName);

        TriggerDefinition removedTrigger =
                triggers.remove(normalizedName);

        if (removedTrigger == null) {
            throw new TriggerNotFoundException(
                    "Trigger not found: " + normalizedName
            );
        }

        metadataEntries.remove(normalizedName);

        return removedTrigger;
    }

    public TriggerDefinition getTrigger(String triggerName) {
        String normalizedName =
                TriggerNameValidator.validate(triggerName);

        TriggerDefinition definition =
                triggers.get(normalizedName);

        if (definition == null) {
            throw new TriggerNotFoundException(
                    "Trigger not found: " + normalizedName
            );
        }

        return definition;
    }

    public TriggerMetadata getMetadata(String triggerName) {
        String normalizedName =
                TriggerNameValidator.validate(triggerName);

        TriggerMetadata metadata =
                metadataEntries.get(normalizedName);

        if (metadata == null) {
            throw new TriggerNotFoundException(
                    "Trigger metadata not found: " + normalizedName
            );
        }

        return metadata;
    }

    public boolean containsTrigger(String triggerName) {
        if (triggerName == null || triggerName.isBlank()) {
            return false;
        }

        try {
            return triggers.containsKey(
                    TriggerNameValidator.validate(triggerName)
            );

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public List<TriggerDefinition> listTriggers() {
        return List.copyOf(
                triggers.values()
        );
    }

    public List<String> listTriggerNames() {
        return List.copyOf(
                triggers.keySet()
        );
    }

    public List<TriggerMetadata> listMetadata() {
        return List.copyOf(
                metadataEntries.values()
        );
    }

    public List<TriggerDefinition> findTriggers(
            String tableName,
            TriggerTiming timing,
            TriggerEvent event
    ) {
        if (timing == null) {
            throw new IllegalArgumentException(
                    "Trigger timing cannot be null."
            );
        }

        if (event == null) {
            throw new IllegalArgumentException(
                    "Trigger event cannot be null."
            );
        }

        return triggers.values()
                .stream()
                .filter(trigger ->
                        trigger.matches(
                                tableName,
                                timing,
                                event
                        )
                )
                .toList();
    }

    public int size() {
        return triggers.size();
    }

    public boolean isEmpty() {
        return triggers.isEmpty();
    }

    public void clear() {
        triggers.clear();
        metadataEntries.clear();
    }

    @Override
    public String toString() {
        return "TriggerCatalog{" +
                "triggerNames=" + triggers.keySet() +
                '}';
    }
}
