package com.yekdb.storage.table.header;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableIdAllocatorTest {

    @Test
    void shouldStartFromOneByDefault() {

        TableIdAllocator allocator =
                new TableIdAllocator();

        assertEquals(
                1L,
                allocator.nextId()
        );
    }

    @Test
    void shouldGenerateSequentialIds() {

        TableIdAllocator allocator =
                new TableIdAllocator();

        assertEquals(1L, allocator.nextId());
        assertEquals(2L, allocator.nextId());
        assertEquals(3L, allocator.nextId());
    }

    @Test
    void shouldSupportCustomInitialValue() {

        TableIdAllocator allocator =
                new TableIdAllocator(10L);

        assertEquals(
                10L,
                allocator.nextId()
        );

        assertEquals(
                11L,
                allocator.nextId()
        );
    }

    @Test
    void shouldAdvanceAllocator() {

        TableIdAllocator allocator =
                new TableIdAllocator();

        allocator.ensureNextIdAtLeast(50L);

        assertEquals(
                50L,
                allocator.nextId()
        );
    }

    @Test
    void shouldNeverMoveAllocatorBackward() {

        TableIdAllocator allocator =
                new TableIdAllocator(100L);

        allocator.ensureNextIdAtLeast(20L);

        assertEquals(
                100L,
                allocator.nextId()
        );
    }

    @Test
    void peekShouldNotConsumeId() {

        TableIdAllocator allocator =
                new TableIdAllocator();

        assertEquals(
                1L,
                allocator.peekNextId()
        );

        assertEquals(
                1L,
                allocator.peekNextId()
        );

        assertEquals(
                1L,
                allocator.nextId()
        );
    }

    @Test
    void shouldRejectInvalidInitialValue() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new TableIdAllocator(0L)
        );
    }

    @Test
    void shouldRejectInvalidSynchronizationValue() {

        TableIdAllocator allocator =
                new TableIdAllocator();

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.ensureNextIdAtLeast(0L)
        );
    }
}