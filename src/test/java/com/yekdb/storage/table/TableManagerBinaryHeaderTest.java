package com.yekdb.storage.table;

import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderConstants;
import com.yekdb.storage.table.header.TableHeaderIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableManagerBinaryHeaderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createdTableShouldContainBinaryHeader()
            throws IOException {

        TableManager manager =
                new TableManager(tempDirectory);

        Table table =
                createUsersTable();

        manager.createTable(table);

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        TableHeader header =
                TableHeaderIO.read(tableFile);

        assertEquals(
                "users",
                header.getTableName()
        );

        assertEquals(
                table.getColumnCount(),
                header.getColumnCount()
        );
    }

    @Test
    void newlyCreatedTableShouldHaveZeroRows()
            throws IOException {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                createUsersTable()
        );

        TableHeader header =
                TableHeaderIO.read(
                        tempDirectory.resolve("users.tbl")
                );

        assertEquals(
                0L,
                header.getRowCount()
        );
    }

    @Test
    void newlyCreatedTableShouldHaveNoDataPages()
            throws IOException {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                createUsersTable()
        );

        TableHeader header =
                TableHeaderIO.read(
                        tempDirectory.resolve("users.tbl")
                );

        assertEquals(
                -1L,
                header.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                header.getLastDataPageId()
        );
    }

    @Test
    void schemaShouldStartAfterBinaryHeader()
            throws IOException {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                createUsersTable()
        );

        byte[] data =
                Files.readAllBytes(
                        tempDirectory.resolve("users.tbl")
                );

        String schema =
                new String(
                        data,
                        TableHeaderConstants.HEADER_SIZE,
                        data.length
                                - TableHeaderConstants.HEADER_SIZE,
                        TableHeaderConstants.TABLE_NAME_CHARSET
                );

        assertTrue(
                schema.startsWith("YEKDB_TABLE")
        );

        assertTrue(
                schema.contains("tableName=users")
        );
    }

    private Table createUsersTable() {

        return new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );
    }
}