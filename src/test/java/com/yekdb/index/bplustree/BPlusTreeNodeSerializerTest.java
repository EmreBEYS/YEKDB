package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B+ Tree Phase 11 node persistence ve serialization testleri.
 */
class BPlusTreeNodeSerializerTest {

    @Test
    void shouldRoundTripLeafNode() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        BPlusTreeNodeImage<Integer> original =
                BPlusTreeNodeImage.leaf(
                        4,
                        10,
                        2,
                        9,
                        11,
                        List.of(
                                20,
                                30
                        ),
                        List.of(
                                List.of(
                                        new RecordPointer(
                                                5,
                                                0
                                        )
                                ),
                                List.of(
                                        new RecordPointer(
                                                6,
                                                1
                                        )
                                )
                        )
                );

        BPlusTreeNodeImage<Integer> restored =
                serializer.deserialize(
                        serializer.serialize(
                                original
                        )
                );

        assertTrue(restored.isLeaf());

        assertEquals(
                4,
                restored.getOrder()
        );

        assertEquals(
                10,
                restored.getPageId()
        );

        assertEquals(
                2,
                restored.getParentPageId()
        );

        assertEquals(
                9,
                restored.getPreviousLeafPageId()
        );

        assertEquals(
                11,
                restored.getNextLeafPageId()
        );

        assertEquals(
                List.of(
                        20,
                        30
                ),
                restored.getKeys()
        );

        assertEquals(
                original.getValueBuckets(),
                restored.getValueBuckets()
        );
    }

    @Test
    void shouldRoundTripDuplicatePointerBucket() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        RecordPointer first =
                new RecordPointer(
                        1,
                        0
                );

        RecordPointer second =
                new RecordPointer(
                        2,
                        1
                );

        BPlusTreeNodeImage<Integer> image =
                BPlusTreeNodeImage.leaf(
                        4,
                        5,
                        -1,
                        -1,
                        -1,
                        List.of(100),
                        List.of(
                                List.of(
                                        first,
                                        second
                                )
                        )
                );

        BPlusTreeNodeImage<Integer> restored =
                serializer.deserialize(
                        serializer.serialize(image)
                );

        assertEquals(
                List.of(
                        first,
                        second
                ),
                restored
                        .getValueBuckets()
                        .get(0)
        );
    }

    @Test
    void shouldRoundTripInternalNode() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        BPlusTreeNodeImage<Integer> image =
                BPlusTreeNodeImage.internal(
                        4,
                        2,
                        -1,
                        List.of(
                                30,
                                50
                        ),
                        List.of(
                                7,
                                8,
                                9
                        )
                );

        BPlusTreeNodeImage<Integer> restored =
                serializer.deserialize(
                        serializer.serialize(image)
                );

        assertFalse(
                restored.isLeaf()
        );

        assertEquals(
                List.of(
                        30,
                        50
                ),
                restored.getKeys()
        );

        assertEquals(
                List.of(
                        7,
                        8,
                        9
                ),
                restored.getChildPageIds()
        );
    }

    @Test
    void shouldSerializeStringKeys() {

        BPlusTreeNodeSerializer<String> serializer =
                new BPlusTreeNodeSerializer<>(
                        BPlusTreeKeyCodec.stringCodec()
                );

        BPlusTreeNodeImage<String> image =
                BPlusTreeNodeImage.leaf(
                        4,
                        1,
                        -1,
                        -1,
                        -1,
                        List.of(
                                "Ankara",
                                "Malatya"
                        ),
                        List.of(
                                List.of(
                                        new RecordPointer(
                                                1,
                                                0
                                        )
                                ),
                                List.of(
                                        new RecordPointer(
                                                2,
                                                0
                                        )
                                )
                        )
                );

        assertEquals(
                List.of(
                        "Ankara",
                        "Malatya"
                ),
                serializer
                        .deserialize(
                                serializer.serialize(image)
                        )
                        .getKeys()
        );
    }

    @Test
    void shouldSerializeBigDecimalKeys() {

        BPlusTreeNodeSerializer<BigDecimal> serializer =
                new BPlusTreeNodeSerializer<>(
                        BPlusTreeKeyCodec.bigDecimalCodec()
                );

        BigDecimal first =
                new BigDecimal("10.125");

        BigDecimal second =
                new BigDecimal("99999.9900");

        BPlusTreeNodeImage<BigDecimal> image =
                BPlusTreeNodeImage.leaf(
                        4,
                        1,
                        -1,
                        -1,
                        -1,
                        List.of(
                                first,
                                second
                        ),
                        List.of(
                                List.of(
                                        new RecordPointer(
                                                1,
                                                0
                                        )
                                ),
                                List.of(
                                        new RecordPointer(
                                                2,
                                                0
                                        )
                                )
                        )
                );

        assertEquals(
                List.of(
                        first,
                        second
                ),
                serializer
                        .deserialize(
                                serializer.serialize(image)
                        )
                        .getKeys()
        );
    }

    @Test
    void shouldCalculateSerializedSize() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        BPlusTreeNodeImage<Integer> image =
                BPlusTreeNodeImage.internal(
                        4,
                        1,
                        -1,
                        List.of(
                                10,
                                20
                        ),
                        List.of(
                                2,
                                3,
                                4
                        )
                );

        byte[] bytes =
                serializer.serialize(image);

        assertEquals(
                bytes.length,
                serializer.calculateSerializedSize(
                        image
                )
        );
    }

    @Test
    void shouldRejectNullSerializerCodec() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new BPlusTreeNodeSerializer<Integer>(
                        null
                )
        );
    }

    @Test
    void shouldRejectNullNodeImage() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        assertThrows(
                IllegalArgumentException.class,
                () -> serializer.serialize(
                        null
                )
        );
    }

    @Test
    void shouldRejectInvalidMagicNumber() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        byte[] bytes =
                serializer.serialize(
                        sampleLeaf()
                );

        ByteBuffer.wrap(bytes)
                .putInt(
                        0,
                        0x12345678
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> serializer.deserialize(
                                bytes
                        )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("magic")
        );
    }

    @Test
    void shouldRejectUnsupportedVersion() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        byte[] bytes =
                serializer.serialize(
                        sampleLeaf()
                );

        ByteBuffer.wrap(bytes)
                .putShort(
                        Integer.BYTES,
                        (short) 99
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> serializer.deserialize(
                        bytes
                )
        );
    }

    @Test
    void shouldRejectTruncatedPayload() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        byte[] bytes =
                serializer.serialize(
                        sampleLeaf()
                );

        byte[] truncated =
                java.util.Arrays.copyOf(
                        bytes,
                        bytes.length - 3
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> serializer.deserialize(
                        truncated
                )
        );
    }

    @Test
    void shouldRejectTrailingBytes() {

        BPlusTreeNodeSerializer<Integer> serializer =
                integerSerializer();

        byte[] bytes =
                serializer.serialize(
                        sampleLeaf()
                );

        byte[] extended =
                java.util.Arrays.copyOf(
                        bytes,
                        bytes.length + 1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> serializer.deserialize(
                        extended
                )
        );
    }

    @Test
    void shouldRejectUnsortedNodeImageKeys() {

        assertThrows(
                IllegalArgumentException.class,
                () -> BPlusTreeNodeImage.leaf(
                        4,
                        1,
                        -1,
                        -1,
                        -1,
                        List.of(
                                30,
                                10
                        ),
                        List.of(
                                List.of(
                                        new RecordPointer(
                                                1,
                                                0
                                        )
                                ),
                                List.of(
                                        new RecordPointer(
                                                2,
                                                0
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void shouldRejectInvalidInternalChildCount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> BPlusTreeNodeImage.internal(
                        4,
                        1,
                        -1,
                        List.of(
                                10,
                                20
                        ),
                        List.of(
                                2,
                                3
                        )
                )
        );
    }

    private BPlusTreeNodeSerializer<Integer>
    integerSerializer() {

        return new BPlusTreeNodeSerializer<>(
                BPlusTreeKeyCodec.integerCodec()
        );
    }

    private BPlusTreeNodeImage<Integer>
    sampleLeaf() {

        return BPlusTreeNodeImage.leaf(
                4,
                1,
                -1,
                -1,
                -1,
                List.of(
                        10,
                        20
                ),
                List.of(
                        List.of(
                                new RecordPointer(
                                        1,
                                        0
                                )
                        ),
                        List.of(
                                new RecordPointer(
                                        2,
                                        0
                                )
                        )
                )
        );
    }
}