package com.yekdb.view;

import com.yekdb.view.exception.DuplicateViewException;
import com.yekdb.view.exception.ViewNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ViewCatalogTest {

    private ViewCatalog catalog;
    private ViewDefinition adultUsersView;
    private ViewMetadata adultUsersMetadata;

    @BeforeEach
    void setUp() {
        catalog = new ViewCatalog();

        adultUsersView =
                new ViewDefinition(
                        "adult_users",
                        "SELECT id, name FROM users WHERE age >= 18"
                );

        adultUsersMetadata =
                new ViewMetadata("adult_users");
    }

    @Test
    void shouldCreateEmptyCatalog() {
        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
        assertTrue(catalog.listViews().isEmpty());
        assertTrue(catalog.listViewNames().isEmpty());
        assertTrue(catalog.listMetadata().isEmpty());
    }

    @Test
    void shouldRegisterViewSuccessfully() {
        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        assertFalse(catalog.isEmpty());
        assertEquals(1, catalog.size());
        assertTrue(catalog.containsView("ADULT_USERS"));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringDuplicateView() {
        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        ViewDefinition duplicateDefinition =
                new ViewDefinition(
                        "Adult_Users",
                        "SELECT * FROM users"
                );

        ViewMetadata duplicateMetadata =
                new ViewMetadata("Adult_Users");

        assertThrows(
                DuplicateViewException.class,
                () -> catalog.registerView(
                        duplicateDefinition,
                        duplicateMetadata
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenDefinitionAndMetadataNamesDoNotMatch() {
        ViewMetadata metadata =
                new ViewMetadata("active_users");

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerView(
                        adultUsersView,
                        metadata
                )
        );
    }

    @Test
    void shouldReturnRegisteredViewAndMetadata() {
        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        assertEquals(
                adultUsersView,
                catalog.getView("Adult_Users")
        );

        assertEquals(
                adultUsersMetadata,
                catalog.getMetadata("adult_users")
        );
    }

    @Test
    void shouldUnregisterViewSuccessfully() {
        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        ViewDefinition removedView =
                catalog.unregisterView("ADULT_USERS");

        assertEquals(adultUsersView, removedView);
        assertFalse(catalog.containsView("adult_users"));
        assertTrue(catalog.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenViewDoesNotExist() {
        assertThrows(
                ViewNotFoundException.class,
                () -> catalog.getView("missing_view")
        );

        assertThrows(
                ViewNotFoundException.class,
                () -> catalog.getMetadata("missing_view")
        );

        assertThrows(
                ViewNotFoundException.class,
                () -> catalog.unregisterView("missing_view")
        );
    }

    @Test
    void shouldListViewsInRegistrationOrder() {
        ViewDefinition activeUsersView =
                new ViewDefinition(
                        "active_users",
                        "SELECT id FROM users WHERE active = true"
                );

        ViewMetadata activeUsersMetadata =
                new ViewMetadata("active_users");

        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        catalog.registerView(
                activeUsersView,
                activeUsersMetadata
        );

        assertEquals(
                List.of(adultUsersView, activeUsersView),
                catalog.listViews()
        );

        assertEquals(
                List.of("adult_users", "active_users"),
                catalog.listViewNames()
        );
    }

    @Test
    void shouldReturnImmutableLists() {
        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listViews().add(adultUsersView)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listViewNames().add("other_view")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listMetadata().add(adultUsersMetadata)
        );
    }

    @Test
    void shouldReturnFalseForInvalidViewName() {
        assertFalse(catalog.containsView(null));
        assertFalse(catalog.containsView(""));
        assertFalse(catalog.containsView("   "));
        assertFalse(catalog.containsView("123bad"));
    }

    @Test
    void shouldClearCatalog() {
        catalog.registerView(
                adultUsersView,
                adultUsersMetadata
        );

        catalog.clear();

        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
    }
}
