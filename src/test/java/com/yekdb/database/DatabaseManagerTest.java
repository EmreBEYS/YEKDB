package com.yekdb.database;

import com.yekdb.database.exception.DatabaseAlreadyExistsException;
import com.yekdb.database.exception.DatabaseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @TempDir
    Path temporaryDirectory;

    private Path dataDirectory;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() {
        dataDirectory = temporaryDirectory.resolve("data");
        databaseManager = new DatabaseManager(dataDirectory);
    }

    @Test
    void shouldCreateDatabaseDirectory() {
        Database database =
                databaseManager.createDatabase("SchoolDB");

        Path expectedPath =
                dataDirectory.resolve("SchoolDB");

        assertNotNull(database);
        assertEquals("SchoolDB", database.getName());
        assertEquals(expectedPath, database.getDatabasePath());
        assertTrue(Files.isDirectory(expectedPath));
    }

    @Test
    void shouldCreateMetadataFile() {
        databaseManager.createDatabase("SchoolDB");

        Path metadataFile = dataDirectory
                .resolve("SchoolDB")
                .resolve("database.meta");

        assertTrue(Files.isRegularFile(metadataFile));
    }

    @Test
    void metadataFileShouldContainDatabaseInformation()
            throws IOException {

        databaseManager.createDatabase("SchoolDB");

        Path metadataFile = dataDirectory
                .resolve("SchoolDB")
                .resolve("database.meta");

        String content = Files.readString(metadataFile);

        assertAll(
                () -> assertTrue(
                        content.contains("YEKDB DATABASE")
                ),
                () -> assertTrue(
                        content.contains("Version=0.0.6")
                ),
                () -> assertTrue(
                        content.contains("Database=SchoolDB")
                ),
                () -> assertTrue(
                        content.contains("Encoding=UTF-8")
                ),
                () -> assertTrue(
                        content.contains("PageSize=4096")
                )
        );
    }

    @Test
    void existsShouldReturnTrueWhenDatabaseExists() {
        databaseManager.createDatabase("SchoolDB");

        assertTrue(databaseManager.exists("SchoolDB"));
    }

    @Test
    void existsShouldReturnFalseWhenDatabaseDoesNotExist() {
        assertFalse(databaseManager.exists("UnknownDB"));
    }

    @Test
    void shouldRejectDuplicateDatabaseName() {
        databaseManager.createDatabase("SchoolDB");

        assertThrows(
                DatabaseAlreadyExistsException.class,
                () -> databaseManager.createDatabase("SchoolDB")
        );
    }

    @Test
    void shouldRejectInvalidDatabaseName() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> databaseManager.createDatabase(null)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> databaseManager.createDatabase("")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> databaseManager.createDatabase("   ")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> databaseManager.createDatabase("1Database")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> databaseManager.createDatabase("School-DB")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> databaseManager.createDatabase("School DB")
                )
        );
    }

    @Test
    void shouldListCreatedDatabasesAlphabetically() {
        databaseManager.createDatabase("UniversityDB");
        databaseManager.createDatabase("SchoolDB");
        databaseManager.createDatabase("CompanyDB");

        List<String> databases =
                databaseManager.listDatabases();

        assertEquals(
                List.of(
                        "CompanyDB",
                        "SchoolDB",
                        "UniversityDB"
                ),
                databases
        );
    }

    @Test
    void shouldReturnEmptyListWhenDataDirectoryDoesNotExist() {
        List<String> databases =
                databaseManager.listDatabases();

        assertNotNull(databases);
        assertTrue(databases.isEmpty());
    }

    @Test
    void shouldUseExistingDatabase() {
        Database createdDatabase =
                databaseManager.createDatabase("SchoolDB");

        Database selectedDatabase =
                databaseManager.useDatabase("SchoolDB");

        assertNotNull(selectedDatabase);
        assertNotNull(databaseManager.getCurrentDatabase());

        assertEquals(
                createdDatabase.getName(),
                selectedDatabase.getName()
        );

        assertEquals(
                "SchoolDB",
                databaseManager.getCurrentDatabase().getName()
        );

        assertEquals(
                dataDirectory.resolve("SchoolDB"),
                selectedDatabase.getDatabasePath()
        );
    }

    @Test
    void shouldReadMetadataWhenDatabaseIsSelected() {
        Database createdDatabase =
                databaseManager.createDatabase("SchoolDB");

        Database selectedDatabase =
                databaseManager.useDatabase("SchoolDB");

        assertAll(
                () -> assertEquals(
                        createdDatabase.getMetadata().getDatabaseName(),
                        selectedDatabase.getMetadata().getDatabaseName()
                ),
                () -> assertEquals(
                        createdDatabase.getMetadata().getVersion(),
                        selectedDatabase.getMetadata().getVersion()
                ),
                () -> assertEquals(
                        createdDatabase.getMetadata().getCreatedAt(),
                        selectedDatabase.getMetadata().getCreatedAt()
                ),
                () -> assertEquals(
                        createdDatabase.getMetadata().getPageSize(),
                        selectedDatabase.getMetadata().getPageSize()
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenUsingUnknownDatabase() {
        assertThrows(
                DatabaseNotFoundException.class,
                () -> databaseManager.useDatabase("UnknownDB")
        );
    }

    @Test
    void shouldDropExistingDatabase() {
        databaseManager.createDatabase("SchoolDB");

        Path databasePath =
                dataDirectory.resolve("SchoolDB");

        assertTrue(Files.exists(databasePath));

        databaseManager.dropDatabase("SchoolDB");

        assertFalse(Files.exists(databasePath));
        assertFalse(databaseManager.exists("SchoolDB"));
    }

    @Test
    void shouldClearCurrentDatabaseWhenActiveDatabaseIsDropped() {
        databaseManager.createDatabase("SchoolDB");
        databaseManager.useDatabase("SchoolDB");

        assertNotNull(databaseManager.getCurrentDatabase());

        databaseManager.dropDatabase("SchoolDB");

        assertNull(databaseManager.getCurrentDatabase());
    }

    @Test
    void shouldKeepCurrentDatabaseWhenAnotherDatabaseIsDropped() {
        databaseManager.createDatabase("SchoolDB");
        databaseManager.createDatabase("CompanyDB");

        databaseManager.useDatabase("SchoolDB");
        databaseManager.dropDatabase("CompanyDB");

        assertNotNull(databaseManager.getCurrentDatabase());

        assertEquals(
                "SchoolDB",
                databaseManager.getCurrentDatabase().getName()
        );

        assertTrue(databaseManager.exists("SchoolDB"));
        assertFalse(databaseManager.exists("CompanyDB"));
    }

    @Test
    void shouldThrowExceptionWhenDroppingUnknownDatabase() {
        assertThrows(
                DatabaseNotFoundException.class,
                () -> databaseManager.dropDatabase("UnknownDB")
        );
    }

    @Test
    void shouldDeleteDatabaseFilesRecursively()
            throws IOException {

        databaseManager.createDatabase("SchoolDB");

        Path databasePath =
                dataDirectory.resolve("SchoolDB");

        Path tablesDirectory =
                databasePath.resolve("tables");

        Path dummyTableFile =
                tablesDirectory.resolve("students.tbl");

        Files.createDirectories(tablesDirectory);
        Files.writeString(
                dummyTableFile,
                "YEKDB test table data"
        );

        assertTrue(Files.exists(dummyTableFile));

        databaseManager.dropDatabase("SchoolDB");

        assertFalse(Files.exists(dummyTableFile));
        assertFalse(Files.exists(tablesDirectory));
        assertFalse(Files.exists(databasePath));
    }
}