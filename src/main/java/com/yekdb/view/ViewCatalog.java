package com.yekdb.view;

import com.yekdb.view.exception.DuplicateViewException;
import com.yekdb.view.exception.ViewNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aktif veritabanındaki view tanımlarını ve metadata kayıtlarını yönetir.
 */
public class ViewCatalog {

    private final Map<String, ViewDefinition> views;
    private final Map<String, ViewMetadata> metadataEntries;

    public ViewCatalog() {
        this.views = new LinkedHashMap<>();
        this.metadataEntries = new LinkedHashMap<>();
    }

    public void registerView(
            ViewDefinition definition,
            ViewMetadata metadata
    ) {

        if (definition == null) {
            throw new IllegalArgumentException(
                    "View definition cannot be null."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "View metadata cannot be null."
            );
        }

        String viewName =
                ViewNameValidator.validate(
                        definition.getViewName()
                );

        String metadataViewName =
                ViewNameValidator.validate(
                        metadata.getViewName()
                );

        if (!viewName.equals(metadataViewName)) {
            throw new IllegalArgumentException(
                    "View definition name and metadata view name must match."
            );
        }

        if (views.containsKey(viewName)) {
            throw new DuplicateViewException(
                    "View already exists: " + viewName
            );
        }

        views.put(viewName, definition);
        metadataEntries.put(viewName, metadata);
    }

    public ViewDefinition unregisterView(String viewName) {

        String normalizedName =
                ViewNameValidator.validate(viewName);

        ViewDefinition removedView =
                views.remove(normalizedName);

        if (removedView == null) {
            throw new ViewNotFoundException(
                    "View not found: " + normalizedName
            );
        }

        metadataEntries.remove(normalizedName);

        return removedView;
    }

    public ViewDefinition getView(String viewName) {

        String normalizedName =
                ViewNameValidator.validate(viewName);

        ViewDefinition definition =
                views.get(normalizedName);

        if (definition == null) {
            throw new ViewNotFoundException(
                    "View not found: " + normalizedName
            );
        }

        return definition;
    }

    public ViewMetadata getMetadata(String viewName) {

        String normalizedName =
                ViewNameValidator.validate(viewName);

        ViewMetadata metadata =
                metadataEntries.get(normalizedName);

        if (metadata == null) {
            throw new ViewNotFoundException(
                    "View metadata not found: " + normalizedName
            );
        }

        return metadata;
    }

    public boolean containsView(String viewName) {

        if (viewName == null || viewName.isBlank()) {
            return false;
        }

        try {
            return views.containsKey(
                    ViewNameValidator.validate(viewName)
            );

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public List<ViewDefinition> listViews() {
        return List.copyOf(
                views.values()
        );
    }

    public List<String> listViewNames() {
        return List.copyOf(
                views.keySet()
        );
    }

    public List<ViewMetadata> listMetadata() {
        return List.copyOf(
                metadataEntries.values()
        );
    }

    public int size() {
        return views.size();
    }

    public boolean isEmpty() {
        return views.isEmpty();
    }

    public void clear() {
        views.clear();
        metadataEntries.clear();
    }

    @Override
    public String toString() {
        return "ViewCatalog{" +
                "viewNames=" + views.keySet() +
                '}';
    }
}
