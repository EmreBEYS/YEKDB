package com.yekdb.storage.type;

import com.yekdb.storage.exception.InvalidColumnException;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.ColumnTypeDefinition;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDefinedTypePhase14Test {

    @Test
    void shouldParseExplicitUdtColumnType() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("UDT(Address)");
        assertEquals(DataType.UDT, type.dataType());
        assertEquals("address", type.userDefinedTypeName());
        assertEquals("UDT(address)", type.declaration());
    }

    @Test
    void shouldCreateColumnFromUdtTypeDefinition() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("UDT(email_address)");
        Column column = Column.fromTypeDefinition("email", type);
        assertTrue(column.isUserDefinedType());
        assertEquals("email_address", column.getUserDefinedTypeName());
        assertEquals("UDT(email_address)", column.getTypeDeclaration());
    }

    @Test
    void shouldRejectMalformedUdtDeclaration() {
        assertThrows(InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("UDT(123bad)"));
    }

    @Test
    void shouldRejectUdtArrayInV1() {
        assertThrows(InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("UDT(address)[]"));
    }

    @Test
    void shouldRegisterAndResolveTypeCaseInsensitively() {
        UserDefinedTypeRegistry registry = new UserDefinedTypeRegistry();
        registry.register(new UserDefinedTypeDefinition(
                "Email_Address",
                ColumnTypeDefinition.parse("VARCHAR(320)")
        ));
        assertSame(registry.resolve("email_address"), registry.resolve("EMAIL_ADDRESS"));
    }

    @Test
    void shouldRejectDuplicateUdtRegistration() {
        UserDefinedTypeRegistry registry = new UserDefinedTypeRegistry();
        registry.register(new UserDefinedTypeDefinition(
                "money_value", ColumnTypeDefinition.parse("NUMERIC(12,2)")));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new UserDefinedTypeDefinition(
                        "MONEY_VALUE", ColumnTypeDefinition.parse("NUMERIC(12,2)"))));
    }

    @Test
    void shouldRejectUnknownUdtResolution() {
        UserDefinedTypeRegistry registry = new UserDefinedTypeRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> registry.resolve("missing_type"));
    }

    @Test
    void shouldExposeImmutableDefinitionsCollection() {
        UserDefinedTypeRegistry registry = new UserDefinedTypeRegistry();
        registry.register(new UserDefinedTypeDefinition(
                "short_text", ColumnTypeDefinition.parse("VARCHAR(50)")));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.definitions().clear());
    }

    @Test
    void shouldRemoveRegisteredUdt() {
        UserDefinedTypeRegistry registry = new UserDefinedTypeRegistry();
        registry.register(new UserDefinedTypeDefinition(
                "event_code", ColumnTypeDefinition.parse("CHAR(8)")));
        assertTrue(registry.remove("EVENT_CODE"));
        assertTrue(registry.isEmpty());
    }

    @Test
    void shouldRejectUdtBackedByAnotherUdt() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserDefinedTypeDefinition(
                        "nested_type",
                        ColumnTypeDefinition.parse("UDT(base_type)")
                ));
    }
}
