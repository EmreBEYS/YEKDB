package com.yekdb.query.executor;

import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagementCommandParserCharacterTypeTest {

    @Test
    void shouldParseCharacterAndBooleanTypesInCreateTable() {
        ManagementCommandParser parser = new ManagementCommandParser();

        CreateTableCommand command = (CreateTableCommand) parser.parse(
                "CREATE TABLE profile (id INT, code CHAR(8), name VARCHAR(100), description TEXT, active BOOLEAN)"
        );

        assertEquals(5, command.getColumnCount());
        assertEquals(DataType.CHAR, command.getColumns().get(1).getDataType());
        assertEquals(8, command.getColumns().get(1).getLength());
        assertEquals(DataType.VARCHAR, command.getColumns().get(2).getDataType());
        assertEquals(100, command.getColumns().get(2).getLength());
        assertEquals(DataType.TEXT, command.getColumns().get(3).getDataType());
        assertEquals(DataType.BOOLEAN, command.getColumns().get(4).getDataType());
    }

    @Test
    void shouldRejectInvalidVarcharDeclaration() {
        ManagementCommandParser parser = new ManagementCommandParser();

        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TABLE invalid_type (id INT, name VARCHAR(0))"
                )
        );
    }
}
