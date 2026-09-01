package com.yekdb.database;

import com.yekdb.view.ViewCatalog;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseViewCatalogTest {

    @Test
    void shouldCreateDatabaseWithDefaultViewCatalog() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        assertNotNull(database.getViewCatalog());
        assertTrue(database.getViewCatalog().isEmpty());
    }

    @Test
    void shouldUseProvidedViewCatalog() {
        ViewCatalog viewCatalog =
                new ViewCatalog();

        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb"),
                        viewCatalog
                );

        assertSame(viewCatalog, database.getViewCatalog());
    }

    @Test
    void shouldRejectNullViewCatalog() {
        assertThrows(
                NullPointerException.class,
                () -> new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb"),
                        null
                )
        );
    }
}
