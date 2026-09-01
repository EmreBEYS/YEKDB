package com.yekdb.database;

import com.yekdb.trigger.TriggerCatalog;
import com.yekdb.view.ViewCatalog;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTriggerCatalogTest {

    @Test
    void shouldCreateDatabaseWithDefaultTriggerCatalog() {
        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb")
                );

        assertNotNull(database.getTriggerCatalog());
        assertTrue(database.getTriggerCatalog().isEmpty());
    }

    @Test
    void shouldUseProvidedTriggerCatalog() {
        ViewCatalog viewCatalog =
                new ViewCatalog();

        TriggerCatalog triggerCatalog =
                new TriggerCatalog();

        Database database =
                new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb"),
                        viewCatalog,
                        triggerCatalog
                );

        assertSame(viewCatalog, database.getViewCatalog());
        assertSame(triggerCatalog, database.getTriggerCatalog());
    }

    @Test
    void shouldRejectNullTriggerCatalog() {
        assertThrows(
                NullPointerException.class,
                () -> new Database(
                        "yekdb",
                        Path.of("data", "yekdb"),
                        new DatabaseMetadata("yekdb"),
                        new ViewCatalog(),
                        null
                )
        );
    }
}
