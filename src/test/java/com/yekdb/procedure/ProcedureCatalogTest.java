package com.yekdb.procedure;

import com.yekdb.procedure.exception.DuplicateProcedureException;
import com.yekdb.procedure.exception.ProcedureNotFoundException;
import com.yekdb.storage.table.ColumnTypeDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcedureCatalogTest {

    private ProcedureCatalog catalog;
    private ProcedureDefinition createUserProcedure;
    private ProcedureMetadata createUserMetadata;

    @BeforeEach
    void setUp() {
        catalog = new ProcedureCatalog();

        createUserProcedure =
                new ProcedureDefinition(
                        "create_user",
                        List.of(
                                new ProcedureParameter(
                                        "user_id",
                                        ColumnTypeDefinition.parse("INT")
                                ),
                                new ProcedureParameter(
                                        "user_name",
                                        ColumnTypeDefinition.parse("STRING")
                                )
                        ),
                        "INSERT INTO users (id, name) VALUES (:user_id, :user_name)"
                );

        createUserMetadata =
                new ProcedureMetadata(
                        "create_user",
                        2
                );
    }

    @Test
    void shouldCreateEmptyCatalog() {
        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
        assertTrue(catalog.listProcedures().isEmpty());
        assertTrue(catalog.listProcedureNames().isEmpty());
        assertTrue(catalog.listMetadata().isEmpty());
    }

    @Test
    void shouldRegisterProcedureSuccessfully() {
        catalog.registerProcedure(
                createUserProcedure,
                createUserMetadata
        );

        assertFalse(catalog.isEmpty());
        assertEquals(1, catalog.size());
        assertTrue(catalog.containsProcedure("CREATE_USER"));
    }

    @Test
    void shouldRejectDuplicateProcedureName() {
        catalog.registerProcedure(
                createUserProcedure,
                createUserMetadata
        );

        ProcedureDefinition duplicate =
                new ProcedureDefinition(
                        "Create_User",
                        List.of(),
                        "SELECT * FROM users"
                );

        ProcedureMetadata duplicateMetadata =
                new ProcedureMetadata(
                        "Create_User",
                        0
                );

        assertThrows(
                DuplicateProcedureException.class,
                () -> catalog.registerProcedure(
                        duplicate,
                        duplicateMetadata
                )
        );
    }

    @Test
    void shouldReplaceExistingProcedure() {
        catalog.registerProcedure(
                createUserProcedure,
                createUserMetadata
        );

        ProcedureDefinition replacement =
                new ProcedureDefinition(
                        "CREATE_USER",
                        List.of(),
                        "SELECT * FROM users"
                );

        ProcedureMetadata replacementMetadata =
                new ProcedureMetadata(
                        "CREATE_USER",
                        0,
                        createUserMetadata.getCreatedAt(),
                        createUserMetadata.getVersion() + 1
                );

        catalog.replaceProcedure(
                replacement,
                replacementMetadata
        );

        assertEquals(
                replacement,
                catalog.getProcedure("create_user")
        );
        assertEquals(
                2,
                catalog.getMetadata("create_user").getVersion()
        );
        assertEquals(1, catalog.size());
    }

    @Test
    void shouldRejectReplacingMissingProcedure() {
        assertThrows(
                ProcedureNotFoundException.class,
                () -> catalog.replaceProcedure(
                        createUserProcedure,
                        createUserMetadata
                )
        );
    }

    @Test
    void shouldRejectDefinitionAndMetadataMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerProcedure(
                        createUserProcedure,
                        new ProcedureMetadata("other_proc", 2)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerProcedure(
                        createUserProcedure,
                        new ProcedureMetadata("create_user", 1)
                )
        );
    }

    @Test
    void shouldReturnRegisteredProcedureAndMetadata() {
        catalog.registerProcedure(
                createUserProcedure,
                createUserMetadata
        );

        assertEquals(
                createUserProcedure,
                catalog.getProcedure("Create_User")
        );

        assertEquals(
                createUserMetadata,
                catalog.getMetadata("create_user")
        );
    }

    @Test
    void shouldUnregisterProcedureSuccessfully() {
        catalog.registerProcedure(
                createUserProcedure,
                createUserMetadata
        );

        ProcedureDefinition removed =
                catalog.unregisterProcedure("CREATE_USER");

        assertEquals(createUserProcedure, removed);
        assertFalse(catalog.containsProcedure("create_user"));
        assertTrue(catalog.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenProcedureDoesNotExist() {
        assertThrows(
                ProcedureNotFoundException.class,
                () -> catalog.getProcedure("missing_procedure")
        );

        assertThrows(
                ProcedureNotFoundException.class,
                () -> catalog.getMetadata("missing_procedure")
        );

        assertThrows(
                ProcedureNotFoundException.class,
                () -> catalog.unregisterProcedure("missing_procedure")
        );
    }

    @Test
    void shouldReturnImmutableLists() {
        catalog.registerProcedure(
                createUserProcedure,
                createUserMetadata
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listProcedures().add(createUserProcedure)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listProcedureNames().add("other_procedure")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listMetadata().add(createUserMetadata)
        );
    }
}
