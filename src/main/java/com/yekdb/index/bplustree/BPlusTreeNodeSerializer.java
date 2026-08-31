package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * BPlusTreeNodeImage nesnelerini versioned binary formata dönüştürür.
 *
 * Binary format node başına bağımsızdır.
 * Phase 12'de bu byte dizisi doğrudan INDEX tipindeki
 * fiziksel Page payload alanına yazılacaktır.
 */
public final class BPlusTreeNodeSerializer<
        K extends Comparable<K>> {

    public static final int MAGIC_NUMBER =
            0x594B4254; // YKBT

    public static final short FORMAT_VERSION =
            1;

    public static final byte NODE_TYPE_LEAF =
            1;

    public static final byte NODE_TYPE_INTERNAL =
            2;

    public static final ByteOrder BYTE_ORDER =
            ByteOrder.BIG_ENDIAN;

    public static final int HEADER_SIZE =
            Integer.BYTES
                    + Short.BYTES
                    + Byte.BYTES
                    + Byte.BYTES
                    + (Integer.BYTES * 7);

    private final BPlusTreeKeyCodec<K> keyCodec;

    public BPlusTreeNodeSerializer(
            BPlusTreeKeyCodec<K> keyCodec
    ) {

        if (keyCodec == null) {

            throw new IllegalArgumentException(
                    "B+ Tree key codec cannot be null."
            );
        }

        this.keyCodec = keyCodec;
    }

    public byte[] serialize(
            BPlusTreeNodeImage<K> image
    ) {

        if (image == null) {

            throw new IllegalArgumentException(
                    "B+ Tree node image cannot be null."
            );
        }

        List<byte[]> encodedKeys =
                new ArrayList<>(
                        image.getKeyCount()
                );

        int size =
                HEADER_SIZE;

        for (K key : image.getKeys()) {

            byte[] encoded =
                    keyCodec.encode(key);

            if (encoded == null) {

                throw new IllegalArgumentException(
                        "Key codec returned a null payload."
                );
            }

            encodedKeys.add(encoded);

            size = safeAdd(
                    size,
                    Integer.BYTES
                            + encoded.length
            );
        }

        if (image.isLeaf()) {

            for (List<RecordPointer> bucket
                    : image.getValueBuckets()) {

                size = safeAdd(
                        size,
                        Integer.BYTES
                );

                size = safeAdd(
                        size,
                        bucket.size()
                                * (Integer.BYTES * 2)
                );
            }

        } else {

            size = safeAdd(
                    size,
                    image.getChildPageIds()
                            .size()
                            * Integer.BYTES
            );
        }

        ByteBuffer buffer =
                ByteBuffer
                        .allocate(size)
                        .order(BYTE_ORDER);

        buffer.putInt(
                MAGIC_NUMBER
        );

        buffer.putShort(
                FORMAT_VERSION
        );

        buffer.put(
                image.isLeaf()
                        ? NODE_TYPE_LEAF
                        : NODE_TYPE_INTERNAL
        );

        /*
         * Gelecekte flags için kullanılabilecek reserved byte.
         */
        buffer.put(
                (byte) 0
        );

        buffer.putInt(
                image.getOrder()
        );

        buffer.putInt(
                image.getPageId()
        );

        buffer.putInt(
                image.getParentPageId()
        );

        buffer.putInt(
                image.getPreviousLeafPageId()
        );

        buffer.putInt(
                image.getNextLeafPageId()
        );

        buffer.putInt(
                image.getKeyCount()
        );

        buffer.putInt(
                image.isLeaf()
                        ? image
                        .getValueBuckets()
                        .size()
                        : image
                        .getChildPageIds()
                        .size()
        );

        for (byte[] encodedKey
                : encodedKeys) {

            buffer.putInt(
                    encodedKey.length
            );

            buffer.put(
                    encodedKey
            );
        }

        if (image.isLeaf()) {

            for (List<RecordPointer> bucket
                    : image.getValueBuckets()) {

                buffer.putInt(
                        bucket.size()
                );

                for (RecordPointer pointer
                        : bucket) {

                    buffer.putInt(
                            pointer.getPageId()
                    );

                    buffer.putInt(
                            pointer.getSlotId()
                    );
                }
            }

        } else {

            for (Integer childPageId
                    : image.getChildPageIds()) {

                buffer.putInt(
                        childPageId
                );
            }
        }

        return buffer.array();
    }

    public BPlusTreeNodeImage<K> deserialize(
            byte[] bytes
    ) {

        if (bytes == null) {

            throw new IllegalArgumentException(
                    "B+ Tree node payload cannot be null."
            );
        }

        if (bytes.length < HEADER_SIZE) {

            throw new IllegalArgumentException(
                    "B+ Tree node payload is too small."
            );
        }

        try {

            ByteBuffer buffer =
                    ByteBuffer
                            .wrap(bytes)
                            .order(BYTE_ORDER);

            int magic =
                    buffer.getInt();

            if (magic != MAGIC_NUMBER) {

                throw new IllegalArgumentException(
                        "Invalid B+ Tree node magic number."
                );
            }

            short version =
                    buffer.getShort();

            if (version != FORMAT_VERSION) {

                throw new IllegalArgumentException(
                        "Unsupported B+ Tree node format version: "
                                + version
                );
            }

            byte nodeType =
                    buffer.get();

            /*
             * Reserved byte.
             */
            buffer.get();

            if (nodeType != NODE_TYPE_LEAF
                    && nodeType
                    != NODE_TYPE_INTERNAL) {

                throw new IllegalArgumentException(
                        "Unknown B+ Tree node type: "
                                + nodeType
                );
            }

            int order =
                    buffer.getInt();

            int pageId =
                    buffer.getInt();

            int parentPageId =
                    buffer.getInt();

            int previousLeafPageId =
                    buffer.getInt();

            int nextLeafPageId =
                    buffer.getInt();

            int keyCount =
                    buffer.getInt();

            int referenceCount =
                    buffer.getInt();

            if (keyCount < 0
                    || keyCount > order - 1) {

                throw new IllegalArgumentException(
                        "Invalid B+ Tree node key count: "
                                + keyCount
                );
            }

            if (referenceCount < 0) {

                throw new IllegalArgumentException(
                        "Invalid B+ Tree node reference count: "
                                + referenceCount
                );
            }

            List<K> keys =
                    new ArrayList<>(
                            keyCount
                    );

            for (int i = 0;
                 i < keyCount;
                 i++) {

                int length =
                        buffer.getInt();

                if (length < 0
                        || length
                        > buffer.remaining()) {

                    throw new IllegalArgumentException(
                            "Invalid B+ Tree key payload length: "
                                    + length
                    );
                }

                byte[] encoded =
                        new byte[length];

                buffer.get(encoded);

                K key =
                        keyCodec.decode(
                                encoded
                        );

                if (key == null) {

                    throw new IllegalArgumentException(
                            "Key codec decoded a null key."
                    );
                }

                keys.add(key);
            }

            BPlusTreeNodeImage<K> image;

            if (nodeType
                    == NODE_TYPE_LEAF) {

                if (referenceCount
                        != keyCount) {

                    throw new IllegalArgumentException(
                            "Leaf value bucket count must match key count."
                    );
                }

                List<List<RecordPointer>> buckets =
                        new ArrayList<>(
                                referenceCount
                        );

                for (int i = 0;
                     i < referenceCount;
                     i++) {

                    int pointerCount =
                            buffer.getInt();

                    if (pointerCount <= 0) {

                        throw new IllegalArgumentException(
                                "Leaf pointer count must be positive."
                        );
                    }

                    if (pointerCount
                            > buffer.remaining()
                            / (Integer.BYTES * 2)) {

                        throw new IllegalArgumentException(
                                "Leaf pointer payload exceeds remaining bytes."
                        );
                    }

                    List<RecordPointer> bucket =
                            new ArrayList<>(
                                    pointerCount
                            );

                    for (int p = 0;
                         p < pointerCount;
                         p++) {

                        bucket.add(
                                new RecordPointer(
                                        buffer.getInt(),
                                        buffer.getInt()
                                )
                        );
                    }

                    buckets.add(bucket);
                }

                image =
                        BPlusTreeNodeImage.leaf(
                                order,
                                pageId,
                                parentPageId,
                                previousLeafPageId,
                                nextLeafPageId,
                                keys,
                                buckets
                        );

            } else {

                if (!(keyCount == 0
                        && referenceCount == 0)
                        && referenceCount
                        != keyCount + 1) {

                    throw new IllegalArgumentException(
                            "Internal child count must equal key count plus one."
                    );
                }

                if (referenceCount
                        > buffer.remaining()
                        / Integer.BYTES) {

                    throw new IllegalArgumentException(
                            "Internal child payload exceeds remaining bytes."
                    );
                }

                List<Integer> childPageIds =
                        new ArrayList<>(
                                referenceCount
                        );

                for (int i = 0;
                     i < referenceCount;
                     i++) {

                    childPageIds.add(
                            buffer.getInt()
                    );
                }

                image =
                        BPlusTreeNodeImage.internal(
                                order,
                                pageId,
                                parentPageId,
                                keys,
                                childPageIds
                        );
            }

            if (buffer.hasRemaining()) {

                throw new IllegalArgumentException(
                        "Unexpected trailing bytes in B+ Tree node payload."
                );
            }

            return image;

        } catch (BufferUnderflowException exception) {

            throw new IllegalArgumentException(
                    "Truncated B+ Tree node payload.",
                    exception
            );
        }
    }

    public int calculateSerializedSize(
            BPlusTreeNodeImage<K> image
    ) {

        return serialize(image)
                .length;
    }

    private int safeAdd(
            int left,
            int right
    ) {

        if (right < 0
                || left
                > Integer.MAX_VALUE - right) {

            throw new IllegalArgumentException(
                    "B+ Tree node serialized size exceeds supported limit."
            );
        }

        return left + right;
    }
}