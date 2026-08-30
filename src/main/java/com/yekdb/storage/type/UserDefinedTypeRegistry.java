package com.yekdb.storage.type;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Case-insensitive registry for YEKDB user-defined type definitions.
 *
 * This is the Phase 14 foundation. SQL CREATE TYPE / DROP TYPE statements
 * can later delegate to this registry without changing the storage contract.
 */
public final class UserDefinedTypeRegistry {

    private final Map<String, UserDefinedTypeDefinition> types =
            new LinkedHashMap<>();

    public void register(UserDefinedTypeDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String name = definition.name();
        if (types.containsKey(name)) {
            throw new IllegalArgumentException(
                    "User-defined type '" + name + "' is already registered."
            );
        }
        types.put(name, definition);
    }

    public UserDefinedTypeDefinition resolve(String name) {
        String normalized = UserDefinedTypeDefinition.normalizeAndValidateName(name);
        UserDefinedTypeDefinition definition = types.get(normalized);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown user-defined type '" + normalized + "'."
            );
        }
        return definition;
    }

    public boolean contains(String name) {
        String normalized = UserDefinedTypeDefinition.normalizeAndValidateName(name);
        return types.containsKey(normalized);
    }

    public boolean remove(String name) {
        String normalized = UserDefinedTypeDefinition.normalizeAndValidateName(name);
        return types.remove(normalized) != null;
    }

    public int size() {
        return types.size();
    }

    public boolean isEmpty() {
        return types.isEmpty();
    }

    public Collection<UserDefinedTypeDefinition> definitions() {
        return Collections.unmodifiableCollection(types.values());
    }

    public void clear() {
        types.clear();
    }
}
