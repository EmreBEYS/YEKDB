package com.yekdb.trigger;

import com.yekdb.trigger.exception.DuplicateTriggerException;
import com.yekdb.trigger.exception.TriggerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriggerCatalogTest {

    private TriggerCatalog catalog;
    private TriggerDefinition insertLogTrigger;
    private TriggerMetadata insertLogMetadata;

    @BeforeEach
    void setUp() {
        catalog = new TriggerCatalog();

        insertLogTrigger =
                new TriggerDefinition(
                        "users_insert_log",
                        "users",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT,
                        "INSERT INTO audit_log VALUES (NEW.id)"
                );

        insertLogMetadata =
                new TriggerMetadata(
                        "users_insert_log",
                        "users"
                );
    }

    @Test
    void shouldCreateEmptyCatalog() {
        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
        assertTrue(catalog.listTriggers().isEmpty());
        assertTrue(catalog.listTriggerNames().isEmpty());
        assertTrue(catalog.listMetadata().isEmpty());
    }

    @Test
    void shouldRegisterTriggerSuccessfully() {
        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );

        assertFalse(catalog.isEmpty());
        assertEquals(1, catalog.size());
        assertTrue(catalog.containsTrigger("USERS_INSERT_LOG"));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringDuplicateTrigger() {
        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );

        TriggerDefinition duplicateDefinition =
                new TriggerDefinition(
                        "Users_Insert_Log",
                        "users",
                        TriggerTiming.BEFORE,
                        TriggerEvent.INSERT,
                        "SET NEW.created_at = NOW()"
                );

        TriggerMetadata duplicateMetadata =
                new TriggerMetadata(
                        "Users_Insert_Log",
                        "users"
                );

        assertThrows(
                DuplicateTriggerException.class,
                () -> catalog.registerTrigger(
                        duplicateDefinition,
                        duplicateMetadata
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenDefinitionAndMetadataDoNotMatch() {
        TriggerMetadata differentName =
                new TriggerMetadata(
                        "users_update_log",
                        "users"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerTrigger(
                        insertLogTrigger,
                        differentName
                )
        );

        TriggerMetadata differentTable =
                new TriggerMetadata(
                        "users_insert_log",
                        "orders"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerTrigger(
                        insertLogTrigger,
                        differentTable
                )
        );
    }

    @Test
    void shouldReturnRegisteredTriggerAndMetadata() {
        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );

        assertEquals(
                insertLogTrigger,
                catalog.getTrigger("Users_Insert_Log")
        );

        assertEquals(
                insertLogMetadata,
                catalog.getMetadata("users_insert_log")
        );
    }

    @Test
    void shouldUnregisterTriggerSuccessfully() {
        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );

        TriggerDefinition removedTrigger =
                catalog.unregisterTrigger("USERS_INSERT_LOG");

        assertEquals(insertLogTrigger, removedTrigger);
        assertFalse(catalog.containsTrigger("users_insert_log"));
        assertTrue(catalog.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenTriggerDoesNotExist() {
        assertThrows(
                TriggerNotFoundException.class,
                () -> catalog.getTrigger("missing_trigger")
        );

        assertThrows(
                TriggerNotFoundException.class,
                () -> catalog.getMetadata("missing_trigger")
        );

        assertThrows(
                TriggerNotFoundException.class,
                () -> catalog.unregisterTrigger("missing_trigger")
        );
    }

    @Test
    void shouldFindTriggersByTableTimingAndEventInRegistrationOrder() {
        TriggerDefinition beforeInsert =
                new TriggerDefinition(
                        "users_before_insert",
                        "users",
                        TriggerTiming.BEFORE,
                        TriggerEvent.INSERT,
                        "SET NEW.created_at = NOW()"
                );

        TriggerMetadata beforeInsertMetadata =
                new TriggerMetadata(
                        "users_before_insert",
                        "users"
                );

        TriggerDefinition ordersBeforeInsert =
                new TriggerDefinition(
                        "orders_before_insert",
                        "orders",
                        TriggerTiming.BEFORE,
                        TriggerEvent.INSERT,
                        "SET NEW.created_at = NOW()"
                );

        TriggerMetadata ordersBeforeInsertMetadata =
                new TriggerMetadata(
                        "orders_before_insert",
                        "orders"
                );

        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );
        catalog.registerTrigger(
                beforeInsert,
                beforeInsertMetadata
        );
        catalog.registerTrigger(
                ordersBeforeInsert,
                ordersBeforeInsertMetadata
        );

        assertEquals(
                List.of(beforeInsert),
                catalog.findTriggers(
                        "USERS",
                        TriggerTiming.BEFORE,
                        TriggerEvent.INSERT
                )
        );

        assertEquals(
                List.of(insertLogTrigger),
                catalog.findTriggers(
                        "users",
                        TriggerTiming.AFTER,
                        TriggerEvent.INSERT
                )
        );
    }

    @Test
    void shouldReturnImmutableLists() {
        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listTriggers().add(insertLogTrigger)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listTriggerNames().add("other_trigger")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listMetadata().add(insertLogMetadata)
        );
    }

    @Test
    void shouldReturnFalseForInvalidTriggerName() {
        assertFalse(catalog.containsTrigger(null));
        assertFalse(catalog.containsTrigger(""));
        assertFalse(catalog.containsTrigger("   "));
        assertFalse(catalog.containsTrigger("123bad"));
    }

    @Test
    void shouldClearCatalog() {
        catalog.registerTrigger(
                insertLogTrigger,
                insertLogMetadata
        );

        catalog.clear();

        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
    }
}
