package com.yekdb.procedure;

import com.yekdb.procedure.exception.DuplicateProcedureException;
import com.yekdb.procedure.exception.ProcedureNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aktif veritabanındaki stored procedure tanımlarını ve metadata kayıtlarını yönetir.
 */
public class ProcedureCatalog {

    private final Map<String, ProcedureDefinition> procedures;
    private final Map<String, ProcedureMetadata> metadataEntries;

    public ProcedureCatalog() {
        this.procedures = new LinkedHashMap<>();
        this.metadataEntries = new LinkedHashMap<>();
    }

    public void registerProcedure(
            ProcedureDefinition definition,
            ProcedureMetadata metadata
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Procedure definition cannot be null."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Procedure metadata cannot be null."
            );
        }

        String procedureName =
                ProcedureNameValidator.validate(
                        definition.getProcedureName()
                );

        String metadataProcedureName =
                ProcedureNameValidator.validate(
                        metadata.getProcedureName()
                );

        if (!procedureName.equals(metadataProcedureName)) {
            throw new IllegalArgumentException(
                    "Procedure definition name and metadata procedure name must match."
            );
        }

        if (definition.getParameterCount()
                != metadata.getParameterCount()) {
            throw new IllegalArgumentException(
                    "Procedure definition and metadata parameter counts must match."
            );
        }

        if (procedures.containsKey(procedureName)) {
            throw new DuplicateProcedureException(
                    "Procedure already exists: " + procedureName
            );
        }

        procedures.put(procedureName, definition);
        metadataEntries.put(procedureName, metadata);
    }

    public void replaceProcedure(
            ProcedureDefinition definition,
            ProcedureMetadata metadata
    ) {
        validateDefinitionAndMetadata(
                definition,
                metadata
        );

        String procedureName =
                ProcedureNameValidator.validate(
                        definition.getProcedureName()
                );

        if (!procedures.containsKey(procedureName)) {
            throw new ProcedureNotFoundException(
                    "Procedure not found: " + procedureName
            );
        }

        procedures.put(procedureName, definition);
        metadataEntries.put(procedureName, metadata);
    }

    private void validateDefinitionAndMetadata(
            ProcedureDefinition definition,
            ProcedureMetadata metadata
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Procedure definition cannot be null."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Procedure metadata cannot be null."
            );
        }

        String procedureName =
                ProcedureNameValidator.validate(
                        definition.getProcedureName()
                );

        String metadataProcedureName =
                ProcedureNameValidator.validate(
                        metadata.getProcedureName()
                );

        if (!procedureName.equals(metadataProcedureName)) {
            throw new IllegalArgumentException(
                    "Procedure definition name and metadata procedure name must match."
            );
        }

        if (definition.getParameterCount()
                != metadata.getParameterCount()) {
            throw new IllegalArgumentException(
                    "Procedure definition and metadata parameter counts must match."
            );
        }
    }

    public ProcedureDefinition unregisterProcedure(
            String procedureName
    ) {
        String normalizedName =
                ProcedureNameValidator.validate(procedureName);

        ProcedureDefinition removedProcedure =
                procedures.remove(normalizedName);

        if (removedProcedure == null) {
            throw new ProcedureNotFoundException(
                    "Procedure not found: " + normalizedName
            );
        }

        metadataEntries.remove(normalizedName);

        return removedProcedure;
    }

    public ProcedureDefinition getProcedure(
            String procedureName
    ) {
        String normalizedName =
                ProcedureNameValidator.validate(procedureName);

        ProcedureDefinition definition =
                procedures.get(normalizedName);

        if (definition == null) {
            throw new ProcedureNotFoundException(
                    "Procedure not found: " + normalizedName
            );
        }

        return definition;
    }

    public ProcedureMetadata getMetadata(
            String procedureName
    ) {
        String normalizedName =
                ProcedureNameValidator.validate(procedureName);

        ProcedureMetadata metadata =
                metadataEntries.get(normalizedName);

        if (metadata == null) {
            throw new ProcedureNotFoundException(
                    "Procedure metadata not found: " + normalizedName
            );
        }

        return metadata;
    }

    public boolean containsProcedure(
            String procedureName
    ) {
        if (procedureName == null || procedureName.isBlank()) {
            return false;
        }

        try {
            return procedures.containsKey(
                    ProcedureNameValidator.validate(procedureName)
            );

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public List<ProcedureDefinition> listProcedures() {
        return List.copyOf(
                procedures.values()
        );
    }

    public List<String> listProcedureNames() {
        return List.copyOf(
                procedures.keySet()
        );
    }

    public List<ProcedureMetadata> listMetadata() {
        return List.copyOf(
                metadataEntries.values()
        );
    }

    public int size() {
        return procedures.size();
    }

    public boolean isEmpty() {
        return procedures.isEmpty();
    }

    public void clear() {
        procedures.clear();
        metadataEntries.clear();
    }

    @Override
    public String toString() {
        return "ProcedureCatalog{" +
                "procedureNames=" + procedures.keySet() +
                '}';
    }
}
