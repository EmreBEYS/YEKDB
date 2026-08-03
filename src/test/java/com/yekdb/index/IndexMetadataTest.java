package com.yekdb.index;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IndexMetadataTest {

    @Test
    void shouldCreateMetadataWithMainConstructor() {
        IndexMetadata metadata = new IndexMetadata(
                1L,
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        assertEquals(1L, metadata.getIndexId());
        assertEquals("idx_students_id", metadata.getIndexName());
        assertEquals("school_db", metadata.getDatabaseName());
        assertEquals("students", metadata.getTableName());
        assertEquals("student_id", metadata.getColumnName());
        assertEquals(IndexType.PRIMARY, metadata.getIndexType());
        assertEquals(
                IndexMetadata.UNASSIGNED_ROOT_PAGE_ID,
                metadata.getRootPageId()
        );
        assertNotNull(metadata.getCreatedAt());
    }

    @Test
    void shouldCreateMetadataWithDefaultConstructor() {
        IndexMetadata metadata = new IndexMetadata();

        assertEquals(
                IndexMetadata.UNASSIGNED_ROOT_PAGE_ID,
                metadata.getRootPageId()
        );

        assertNotNull(metadata.getCreatedAt());
    }

    @Test
    void shouldCreateMetadataWithAllFieldsConstructor() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 8, 3, 10, 30);

        IndexMetadata metadata = new IndexMetadata(
                8L,
                "idx_students_email",
                "school_db",
                "students",
                "email",
                IndexType.UNIQUE,
                15,
                createdAt
        );

        assertEquals(8L, metadata.getIndexId());
        assertEquals("idx_students_email", metadata.getIndexName());
        assertEquals("school_db", metadata.getDatabaseName());
        assertEquals("students", metadata.getTableName());
        assertEquals("email", metadata.getColumnName());
        assertEquals(IndexType.UNIQUE, metadata.getIndexType());
        assertEquals(15, metadata.getRootPageId());
        assertEquals(createdAt, metadata.getCreatedAt());
    }

    @Test
    void shouldUpdateAllFieldsWithSetters() {
        IndexMetadata metadata = new IndexMetadata();

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 8, 3, 11, 0);

        metadata.setIndexId(10L);
        metadata.setIndexName("idx_city");
        metadata.setDatabaseName("school_db");
        metadata.setTableName("students");
        metadata.setColumnName("city");
        metadata.setIndexType(IndexType.NON_UNIQUE);
        metadata.setRootPageId(22);
        metadata.setCreatedAt(createdAt);

        assertEquals(10L, metadata.getIndexId());
        assertEquals("idx_city", metadata.getIndexName());
        assertEquals("school_db", metadata.getDatabaseName());
        assertEquals("students", metadata.getTableName());
        assertEquals("city", metadata.getColumnName());
        assertEquals(IndexType.NON_UNIQUE, metadata.getIndexType());
        assertEquals(22, metadata.getRootPageId());
        assertEquals(createdAt, metadata.getCreatedAt());
    }

    @Test
    void shouldBeValidWhenAllRequiredFieldsAreValid() {
        IndexMetadata metadata = createValidMetadata();

        assertTrue(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenIndexIdIsNegative() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setIndexId(-1L);

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenIndexNameIsBlank() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setIndexName(" ");

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenDatabaseNameIsNull() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setDatabaseName(null);

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenTableNameIsBlank() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setTableName("");

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenColumnNameIsNull() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setColumnName(null);

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenIndexTypeIsNull() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setIndexType(null);

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenRootPageIdIsLessThanMinusOne() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setRootPageId(-2);

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldBeInvalidWhenCreatedAtIsNull() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setCreatedAt(null);

        assertFalse(metadata.isValid());
    }

    @Test
    void shouldNotHaveRootPageInitially() {
        IndexMetadata metadata = createValidMetadata();

        assertFalse(metadata.hasRootPage());
    }

    @Test
    void shouldHaveRootPageAfterAssignment() {
        IndexMetadata metadata = createValidMetadata();
        metadata.setRootPageId(12);

        assertTrue(metadata.hasRootPage());
    }

    @Test
    void shouldBelongToExpectedTable() {
        IndexMetadata metadata = createValidMetadata();

        assertTrue(
                metadata.belongsToTable(
                        "school_db",
                        "students"
                )
        );
    }

    @Test
    void shouldNotBelongToDifferentTable() {
        IndexMetadata metadata = createValidMetadata();

        assertFalse(
                metadata.belongsToTable(
                        "school_db",
                        "teachers"
                )
        );
    }

    @Test
    void shouldBelongToExpectedColumn() {
        IndexMetadata metadata = createValidMetadata();

        assertTrue(
                metadata.belongsToColumn("student_id")
        );
    }

    @Test
    void shouldNotBelongToDifferentColumn() {
        IndexMetadata metadata = createValidMetadata();

        assertFalse(
                metadata.belongsToColumn("email")
        );
    }

    @Test
    void shouldBeEqualWhenAllFieldsAreEqual() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 8, 3, 10, 45);

        IndexMetadata first = new IndexMetadata(
                1L,
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY,
                4,
                createdAt
        );

        IndexMetadata second = new IndexMetadata(
                1L,
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY,
                4,
                createdAt
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIndexNamesAreDifferent() {
        IndexMetadata first = createValidMetadata();
        IndexMetadata second = createValidMetadata();

        second.setIndexName("idx_different");

        assertNotEquals(first, second);
    }

    @Test
    void shouldReturnExpectedStringRepresentation() {
        IndexMetadata metadata = createValidMetadata();

        String text = metadata.toString();

        assertTrue(text.contains("idx_students_id"));
        assertTrue(text.contains("school_db"));
        assertTrue(text.contains("students"));
        assertTrue(text.contains("student_id"));
        assertTrue(text.contains("PRIMARY"));
    }

    private IndexMetadata createValidMetadata() {
        return new IndexMetadata(
                1L,
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );
    }
}