package com.yekdb.index.bplustree;

import com.yekdb.index.RecordPointer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * YEKDB B+ Tree implementasyonunun ana sınıfıdır.
 *
 * B+ Tree insert, exact/range search ve delete işlemlerini yönetir.
 * Phase 9 kapsamında leaf/internal merge, recursive parent cleanup ve
 * root shrink işlemleri desteklenir.
 *
 * @param <K> index anahtar tipi
 */
public class BPlusTree<K extends Comparable<K>> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MIN_ORDER = 3;
    public static final int DEFAULT_ORDER = 32;

    private final int order;
    private BPlusTreeNode<K> root;

    public BPlusTree() {
        this(DEFAULT_ORDER);
    }

    public BPlusTree(int order) {
        validateOrder(order);
        this.order = order;
        this.root = new BPlusTreeLeafNode<>(order);
    }

    public int getOrder() {
        return order;
    }

    public int getMaxKeys() {
        return order - 1;
    }

    public int getMinLeafKeys() {
        return (int) Math.ceil((order - 1) / 2.0);
    }

    public int getMinInternalChildren() {
        return (int) Math.ceil(order / 2.0);
    }

    public int getMinInternalKeys() {
        return getMinInternalChildren() - 1;
    }

    public BPlusTreeNode<K> getRoot() {
        return root;
    }

    @SuppressWarnings("unchecked")
    public BPlusTreeLeafNode<K> getRootLeaf() {
        if (!root.isLeaf()) {
            throw new IllegalStateException("B+ Tree root node is not a leaf.");
        }
        return (BPlusTreeLeafNode<K>) root;
    }

    @SuppressWarnings("unchecked")
    public BPlusTreeInternalNode<K> getRootInternal() {
        if (root.isLeaf()) {
            throw new IllegalStateException("B+ Tree root node is not an internal node.");
        }
        return (BPlusTreeInternalNode<K>) root;
    }

    public boolean isEmpty() {
        return root.isLeaf() && root.isEmpty();
    }

    public int getHeight() {
        int height = 1;
        BPlusTreeNode<K> current = root;

        while (!current.isLeaf()) {
            BPlusTreeInternalNode<K> internal = castToInternal(current);
            if (internal.getChildCount() == 0) {
                break;
            }
            current = internal.getChild(0);
            height++;
        }

        return height;
    }

    /**
     * B+ Tree içerisine key/pointer ekler.
     *
     * Leaf veya internal node overflow oluşursa split sonucu recursive olarak
     * parent seviyesine taşınır. Root seviyesine split ulaşırsa yeni internal
     * root oluşturulur.
     *
     * @return yeni key eklendiyse true, mevcut key bucket'ı güncellendiyse false
     */
    public boolean insert(K key, RecordPointer pointer) {
        validateKey(key);
        validatePointer(pointer);

        InsertResult<K> result = insertRecursive(root, key, pointer);

        if (result.splitResult != null) {
            SplitResult<K> rootSplit = result.splitResult;

            BPlusTreeInternalNode<K> newRoot = new BPlusTreeInternalNode<>(order);
            newRoot.addChild(root);
            newRoot.addSeparatorKey(0, rootSplit.separatorKey);
            newRoot.addChild(rootSplit.rightNode);

            setRoot(newRoot);
        }

        return result.newKey;
    }

    /**
     * Exact key araması yapar.
     */
    public List<RecordPointer> search(K key) {
        validateKey(key);
        return findLeaf(key).findPointers(key);
    }

    public boolean containsKey(K key) {
        return !search(key).isEmpty();
    }

    /**
     * Verilen key'e ait bütün RecordPointer bucket'ını leaf node'dan siler.
     *
     * Leaf underflow oluşursa önce sibling borrow/redistribution denenir.
     * Borrow mümkün değilse leaf merge ve recursive parent cleanup uygulanır.
     *
     * @return key bulunup silindiyse true
     */
    public boolean delete(K key) {
        validateKey(key);

        DeletionPath<K> path = findDeletionPath(key);
        BPlusTreeLeafNode<K> leaf = path.leaf;
        int keyIndex = leaf.findKeyIndex(key);

        if (keyIndex < 0) {
            return false;
        }

        boolean minimumChanged = keyIndex == 0;
        leaf.removeEntry(keyIndex);

        rebalanceLeafAfterDelete(path, minimumChanged);

        return true;
    }

    /**
     * Verilen key bucket'ından yalnızca belirtilen RecordPointer'ı siler.
     * Bucket boşalırsa key de leaf node'dan kaldırılır.
     *
     * Leaf underflow oluşursa sibling borrow/redistribution denenir. Borrow
     * mümkün değilse leaf merge ve recursive parent cleanup uygulanır.
     *
     * @return pointer bulunup silindiyse true
     */
    public boolean delete(K key, RecordPointer pointer) {
        validateKey(key);
        validatePointer(pointer);

        DeletionPath<K> path = findDeletionPath(key);
        BPlusTreeLeafNode<K> leaf = path.leaf;
        int keyIndex = leaf.findKeyIndex(key);

        if (keyIndex < 0) {
            return false;
        }

        if (!leaf.removePointer(keyIndex, pointer)) {
            return false;
        }

        if (!leaf.getPointers(keyIndex).isEmpty()) {
            return true;
        }

        boolean minimumChanged = keyIndex == 0;
        leaf.removeEntry(keyIndex);

        rebalanceLeafAfterDelete(path, minimumChanged);

        return true;
    }

    /**
     * Verilen başlangıç ve bitiş key'leri arasındaki pointer'ları artan key
     * sırasına göre döndürür. Her iki sınır da dahildir.
     */
    public List<RecordPointer> searchRange(K fromKey, K toKey) {
        return searchRange(fromKey, true, toKey, true);
    }

    /**
     * Verilen başlangıç ve bitiş key'leri arasındaki pointer'ları artan key
     * sırasına göre döndürür. Sınırların dahil olup olmadığı parametrelerle
     * belirlenir.
     */
    public List<RecordPointer> searchRange(
            K fromKey,
            boolean fromInclusive,
            K toKey,
            boolean toInclusive
    ) {
        validateKey(fromKey);
        validateKey(toKey);

        if (fromKey.compareTo(toKey) > 0) {
            throw new IllegalArgumentException(
                    "Range start key cannot be greater than end key."
            );
        }

        return collectRange(
                fromKey,
                fromInclusive,
                toKey,
                toInclusive
        );
    }

    /**
     * key değerinden büyük olan kayıtların pointer'larını döndürür.
     */
    public List<RecordPointer> searchGreaterThan(K key) {
        validateKey(key);
        return collectRange(key, false, null, false);
    }

    /**
     * key değerinden büyük veya eşit olan kayıtların pointer'larını döndürür.
     */
    public List<RecordPointer> searchGreaterThanOrEqual(K key) {
        validateKey(key);
        return collectRange(key, true, null, false);
    }

    /**
     * key değerinden küçük olan kayıtların pointer'larını döndürür.
     */
    public List<RecordPointer> searchLessThan(K key) {
        validateKey(key);
        return collectRange(null, false, key, false);
    }

    /**
     * key değerinden küçük veya eşit olan kayıtların pointer'larını döndürür.
     */
    public List<RecordPointer> searchLessThanOrEqual(K key) {
        validateKey(key);
        return collectRange(null, false, key, true);
    }

    /**
     * Tree içindeki bütün pointer'ları leaf zinciri üzerinden artan key sırasına
     * göre döndürür.
     */
    public List<RecordPointer> scanAll() {
        return collectRange(null, false, null, false);
    }

    /**
     * Delete işlemi için root'tan leaf'e kadar parent ve child index yolunu
     * toplar. Parent referansı node üzerinde tutulmadığı için bu yol yalnızca
     * ilgili delete operasyonu süresince kullanılır.
     */
    private DeletionPath<K> findDeletionPath(K key) {
        List<BPlusTreeInternalNode<K>> parents = new ArrayList<>();
        List<Integer> childIndexes = new ArrayList<>();

        BPlusTreeNode<K> current = root;

        while (!current.isLeaf()) {
            BPlusTreeInternalNode<K> internal = castToInternal(current);
            int childIndex = findChildIndex(internal, key);

            parents.add(internal);
            childIndexes.add(childIndex);

            current = internal.getChild(childIndex);
        }

        return new DeletionPath<>(
                castToLeaf(current),
                parents,
                childIndexes
        );
    }

    /**
     * Delete sonrasında leaf minimum occupancy kontrolünü yapar. Root leaf için
     * minimum occupancy zorunluluğu yoktur. Underflow varsa önce sibling borrow
     * denenir; borrow mümkün değilse leaf merge uygulanır. Parent child sayısı
     * azaldığında internal rebalance recursive olarak root seviyesine kadar
     * devam eder.
     */
    private void rebalanceLeafAfterDelete(
            DeletionPath<K> path,
            boolean minimumChanged
    ) {
        BPlusTreeLeafNode<K> leaf = path.leaf;

        if (leaf == root) {
            return;
        }

        if (leaf.getKeyCount() >= getMinLeafKeys()) {
            refreshAllSeparators();
            return;
        }

        if (borrowFromLeftSibling(path) || borrowFromRightSibling(path)) {
            refreshAllSeparators();
            return;
        }

        mergeLeaf(path);
        normalizeRoot();
        refreshAllSeparators();
    }

    /**
     * Sol leaf sibling minimum occupancy'nin üzerinde ise son entry'yi
     * underflow leaf'in başına taşır.
     */
    private boolean borrowFromLeftSibling(DeletionPath<K> path) {
        if (path.parents.isEmpty()) {
            return false;
        }

        int parentLevel = path.parents.size() - 1;
        BPlusTreeInternalNode<K> parent = path.parents.get(parentLevel);
        int childIndex = path.childIndexes.get(parentLevel);

        if (childIndex == 0) {
            return false;
        }

        BPlusTreeNode<K> siblingNode = parent.getChild(childIndex - 1);
        if (!siblingNode.isLeaf()) {
            return false;
        }

        BPlusTreeLeafNode<K> leftSibling = castToLeaf(siblingNode);
        if (leftSibling.getKeyCount() <= getMinLeafKeys()) {
            return false;
        }

        int sourceIndex = leftSibling.getKeyCount() - 1;
        K borrowedKey = leftSibling.getKey(sourceIndex);
        List<RecordPointer> borrowedPointers = leftSibling.removeEntry(sourceIndex);

        path.leaf.addEntry(0, borrowedKey, borrowedPointers);
        return true;
    }

    /**
     * Sağ leaf sibling minimum occupancy'nin üzerinde ise ilk entry'yi
     * underflow leaf'in sonuna taşır.
     */
    private boolean borrowFromRightSibling(DeletionPath<K> path) {
        if (path.parents.isEmpty()) {
            return false;
        }

        int parentLevel = path.parents.size() - 1;
        BPlusTreeInternalNode<K> parent = path.parents.get(parentLevel);
        int childIndex = path.childIndexes.get(parentLevel);

        if (childIndex + 1 >= parent.getChildCount()) {
            return false;
        }

        BPlusTreeNode<K> siblingNode = parent.getChild(childIndex + 1);
        if (!siblingNode.isLeaf()) {
            return false;
        }

        BPlusTreeLeafNode<K> rightSibling = castToLeaf(siblingNode);
        if (rightSibling.getKeyCount() <= getMinLeafKeys()) {
            return false;
        }

        K borrowedKey = rightSibling.getKey(0);
        List<RecordPointer> borrowedPointers = rightSibling.removeEntry(0);

        path.leaf.addEntry(
                path.leaf.getKeyCount(),
                borrowedKey,
                borrowedPointers
        );

        return true;
    }

    /**
     * Borrow mümkün olmadığında underflow leaf'i sibling ile birleştirir.
     * Mümkünse sol sibling korunur; aksi halde sağ sibling mevcut leaf içine
     * taşınır. Parent'tan bir child kaldırıldığı için internal rebalance başlar.
     */
    private void mergeLeaf(DeletionPath<K> path) {
        int parentLevel = path.parents.size() - 1;
        BPlusTreeInternalNode<K> parent = path.parents.get(parentLevel);
        int childIndex = path.childIndexes.get(parentLevel);
        BPlusTreeLeafNode<K> leaf = path.leaf;

        if (childIndex > 0) {
            BPlusTreeLeafNode<K> leftSibling = castToLeaf(parent.getChild(childIndex - 1));
            appendLeafEntries(leftSibling, leaf);

            leftSibling.setNextLeaf(leaf.getNextLeaf());
            if (leaf.getNextLeaf() != null) {
                leaf.getNextLeaf().setPreviousLeaf(leftSibling);
            }

            parent.removeChild(childIndex);
        } else if (childIndex + 1 < parent.getChildCount()) {
            BPlusTreeLeafNode<K> rightSibling = castToLeaf(parent.getChild(childIndex + 1));
            appendLeafEntries(leaf, rightSibling);

            leaf.setNextLeaf(rightSibling.getNextLeaf());
            if (rightSibling.getNextLeaf() != null) {
                rightSibling.getNextLeaf().setPreviousLeaf(leaf);
            }

            parent.removeChild(childIndex + 1);
        } else {
            throw new IllegalStateException(
                    "Leaf merge requires at least one sibling."
            );
        }

        rebuildSeparatorKeys(parent);
        rebalanceInternalAfterChildRemoval(path, parentLevel);
    }

    /**
     * source leaf içerisindeki bütün key/pointer bucket'larını target leaf'in
     * sonuna sıralı biçimde taşır.
     */
    private void appendLeafEntries(
            BPlusTreeLeafNode<K> target,
            BPlusTreeLeafNode<K> source
    ) {
        List<K> sourceKeys = new ArrayList<>(source.getKeys());
        List<List<RecordPointer>> sourceValues = new ArrayList<>(source.getValues());

        for (int i = 0; i < sourceKeys.size(); i++) {
            target.addEntry(
                    target.getKeyCount(),
                    sourceKeys.get(i),
                    sourceValues.get(i)
            );
        }
    }

    /**
     * Child kaldırıldıktan sonra internal minimum occupancy'yi recursive olarak
     * düzeltir. Önce sibling borrow, ardından internal merge uygulanır.
     */
    private void rebalanceInternalAfterChildRemoval(
            DeletionPath<K> path,
            int level
    ) {
        BPlusTreeInternalNode<K> internal = path.parents.get(level);

        if (internal == root) {
            normalizeRoot();
            return;
        }

        if (internal.getChildCount() >= getMinInternalChildren()) {
            return;
        }

        BPlusTreeInternalNode<K> parent = path.parents.get(level - 1);
        int childIndex = path.childIndexes.get(level - 1);

        if (borrowInternalFromLeft(parent, childIndex, internal)
                || borrowInternalFromRight(parent, childIndex, internal)) {
            return;
        }

        if (childIndex > 0) {
            BPlusTreeInternalNode<K> leftSibling =
                    castToInternal(parent.getChild(childIndex - 1));

            appendInternalChildren(leftSibling, internal);
            parent.removeChild(childIndex);
            rebuildSeparatorKeys(leftSibling);
            rebuildSeparatorKeys(parent);
        } else if (childIndex + 1 < parent.getChildCount()) {
            BPlusTreeInternalNode<K> rightSibling =
                    castToInternal(parent.getChild(childIndex + 1));

            appendInternalChildren(internal, rightSibling);
            parent.removeChild(childIndex + 1);
            rebuildSeparatorKeys(internal);
            rebuildSeparatorKeys(parent);
        } else {
            throw new IllegalStateException(
                    "Internal merge requires at least one sibling."
            );
        }

        rebalanceInternalAfterChildRemoval(path, level - 1);
    }

    /**
     * Sol internal sibling minimum child sayısının üzerinde ise son child'ı
     * target internal node'un başına taşır.
     */
    private boolean borrowInternalFromLeft(
            BPlusTreeInternalNode<K> parent,
            int childIndex,
            BPlusTreeInternalNode<K> target
    ) {
        if (childIndex == 0) {
            return false;
        }

        BPlusTreeInternalNode<K> leftSibling =
                castToInternal(parent.getChild(childIndex - 1));

        if (leftSibling.getChildCount() <= getMinInternalChildren()) {
            return false;
        }

        BPlusTreeNode<K> borrowedChild =
                leftSibling.removeChild(leftSibling.getChildCount() - 1);

        target.addChild(0, borrowedChild);

        rebuildSeparatorKeys(leftSibling);
        rebuildSeparatorKeys(target);
        rebuildSeparatorKeys(parent);
        return true;
    }

    /**
     * Sağ internal sibling minimum child sayısının üzerinde ise ilk child'ı
     * target internal node'un sonuna taşır.
     */
    private boolean borrowInternalFromRight(
            BPlusTreeInternalNode<K> parent,
            int childIndex,
            BPlusTreeInternalNode<K> target
    ) {
        if (childIndex + 1 >= parent.getChildCount()) {
            return false;
        }

        BPlusTreeInternalNode<K> rightSibling =
                castToInternal(parent.getChild(childIndex + 1));

        if (rightSibling.getChildCount() <= getMinInternalChildren()) {
            return false;
        }

        BPlusTreeNode<K> borrowedChild = rightSibling.removeChild(0);
        target.addChild(borrowedChild);

        rebuildSeparatorKeys(rightSibling);
        rebuildSeparatorKeys(target);
        rebuildSeparatorKeys(parent);
        return true;
    }

    /**
     * source internal node'un bütün child referanslarını target node'un sonuna
     * taşır. Separator key'ler child minimumlarından tekrar üretildiği için
     * doğrudan kopyalanmaz.
     */
    private void appendInternalChildren(
            BPlusTreeInternalNode<K> target,
            BPlusTreeInternalNode<K> source
    ) {
        List<BPlusTreeNode<K>> children = new ArrayList<>(source.getChildren());
        for (BPlusTreeNode<K> child : children) {
            target.addChild(child);
        }
    }

    /**
     * Internal separator key'lerini child subtree minimumlarına göre yeniden
     * üretir. B+ Tree semantiğinde separator[i], child[i + 1] subtree'sinin
     * minimum key'idir.
     */
    private void rebuildSeparatorKeys(BPlusTreeInternalNode<K> internal) {
        internal.clearKeys();

        for (int i = 1; i < internal.getChildCount(); i++) {
            internal.addSeparatorKey(
                    internal.getKeyCount(),
                    findSubtreeMinimumKey(internal.getChild(i))
            );
        }
    }

    /**
     * Verilen subtree'nin minimum key'ini döndürür.
     */
    private K findSubtreeMinimumKey(BPlusTreeNode<K> node) {
        BPlusTreeNode<K> current = node;

        while (!current.isLeaf()) {
            BPlusTreeInternalNode<K> internal = castToInternal(current);
            if (internal.getChildCount() == 0) {
                throw new IllegalStateException(
                        "Internal node must contain at least one child."
                );
            }
            current = internal.getChild(0);
        }

        BPlusTreeLeafNode<K> leaf = castToLeaf(current);
        if (leaf.isEmpty()) {
            throw new IllegalStateException(
                    "Non-root leaf cannot be empty while rebuilding separators."
            );
        }

        return leaf.getKey(0);
    }

    /**
     * Structural delete işlemlerinden sonra bütün internal separator'ları
     * subtree minimumlarına göre post-order biçimde yeniler.
     */
    private void refreshAllSeparators() {
        refreshSeparatorsRecursive(root);
    }

    private void refreshSeparatorsRecursive(BPlusTreeNode<K> node) {
        if (node.isLeaf()) {
            return;
        }

        BPlusTreeInternalNode<K> internal = castToInternal(node);
        for (BPlusTreeNode<K> child : internal.getChildren()) {
            refreshSeparatorsRecursive(child);
        }

        rebuildSeparatorKeys(internal);
    }

    /**
     * Root internal node tek child'a düştüğünde child yeni root yapılır. Bu
     * işlem ardışık tek-child root seviyeleri için tekrar edilir.
     */
    private void normalizeRoot() {
        while (!root.isLeaf()) {
            BPlusTreeInternalNode<K> internalRoot = castToInternal(root);

            if (internalRoot.getChildCount() > 1) {
                rebuildSeparatorKeys(internalRoot);
                return;
            }

            if (internalRoot.getChildCount() == 1) {
                setRoot(internalRoot.getChild(0));
                continue;
            }

            setRoot(new BPlusTreeLeafNode<>(order));
            return;
        }
    }

    /**
     * Verilen key'in bulunması gereken leaf node'u döndürür.
     */
    public BPlusTreeLeafNode<K> findLeafForKey(K key) {
        validateKey(key);
        return findLeaf(key);
    }

    void setRoot(BPlusTreeNode<K> root) {
        if (root == null) {
            throw new IllegalArgumentException("B+ Tree root cannot be null.");
        }
        if (root.getOrder() != order) {
            throw new IllegalArgumentException("Root order must match tree order.");
        }
        this.root = root;
    }

    /**
     * Node türüne göre recursive insert gerçekleştirir.
     */
    private InsertResult<K> insertRecursive(
            BPlusTreeNode<K> node,
            K key,
            RecordPointer pointer
    ) {
        if (node.isLeaf()) {
            return insertIntoLeaf(castToLeaf(node), key, pointer);
        }

        return insertIntoInternal(castToInternal(node), key, pointer);
    }

    /**
     * Leaf node'a sorted insert yapar ve overflow oluşursa split sonucunu döndürür.
     */
    private InsertResult<K> insertIntoLeaf(
            BPlusTreeLeafNode<K> leaf,
            K key,
            RecordPointer pointer
    ) {
        int existingIndex = leaf.findKeyIndex(key);

        if (existingIndex >= 0) {
            leaf.addPointer(existingIndex, pointer);
            return InsertResult.withoutSplit(false);
        }

        leaf.insertSorted(key, pointer);

        if (!leaf.isOverflow()) {
            return InsertResult.withoutSplit(true);
        }

        BPlusTreeLeafNode<K> rightLeaf = splitLeaf(leaf);
        K separatorKey = rightLeaf.getKey(0);

        return InsertResult.withSplit(
                true,
                new SplitResult<>(separatorKey, rightLeaf)
        );
    }

    /**
     * Internal node üzerinden uygun child'a iner. Child split olursa separator
     * ve right child mevcut internal node'a eklenir. Internal overflow oluşursa
     * internal split sonucu üst seviyeye döndürülür.
     */
    private InsertResult<K> insertIntoInternal(
            BPlusTreeInternalNode<K> internal,
            K key,
            RecordPointer pointer
    ) {
        int childIndex = findChildIndex(internal, key);
        BPlusTreeNode<K> child = internal.getChild(childIndex);

        InsertResult<K> childResult = insertRecursive(child, key, pointer);

        if (childResult.splitResult == null) {
            return InsertResult.withoutSplit(childResult.newKey);
        }

        SplitResult<K> childSplit = childResult.splitResult;

        internal.addSeparatorKey(childIndex, childSplit.separatorKey);
        internal.addChild(childIndex + 1, childSplit.rightNode);

        if (!internal.isOverflow()) {
            return InsertResult.withoutSplit(childResult.newKey);
        }

        SplitResult<K> internalSplit = splitInternal(internal);

        return InsertResult.withSplit(
                childResult.newKey,
                internalSplit
        );
    }

    /**
     * Leaf node'u iki parçaya ayırır ve leaf linked-list bağlantılarını korur.
     * Sol taraf mevcut leaf üzerinde kalır, sağ taraf yeni node olarak döner.
     */
    private BPlusTreeLeafNode<K> splitLeaf(BPlusTreeLeafNode<K> leaf) {
        int splitIndex = (leaf.getKeyCount() + 1) / 2;

        List<K> originalKeys = new ArrayList<>(leaf.getKeys());
        List<List<RecordPointer>> originalValues = new ArrayList<>(leaf.getValues());

        BPlusTreeLeafNode<K> rightLeaf = new BPlusTreeLeafNode<>(order);
        BPlusTreeLeafNode<K> oldNext = leaf.getNextLeaf();

        leaf.clearEntries();

        for (int i = 0; i < splitIndex; i++) {
            leaf.addEntry(
                    leaf.getKeyCount(),
                    originalKeys.get(i),
                    originalValues.get(i)
            );
        }

        for (int i = splitIndex; i < originalKeys.size(); i++) {
            rightLeaf.addEntry(
                    rightLeaf.getKeyCount(),
                    originalKeys.get(i),
                    originalValues.get(i)
            );
        }

        rightLeaf.setPreviousLeaf(leaf);
        rightLeaf.setNextLeaf(oldNext);
        leaf.setNextLeaf(rightLeaf);

        if (oldNext != null) {
            oldNext.setPreviousLeaf(rightLeaf);
        }

        return rightLeaf;
    }

    /**
     * Overflow durumundaki internal node'u iki internal node'a böler.
     *
     * Promote edilen key mevcut node'dan çıkarılır ve parent separator olarak
     * kullanılır. Sağ node promote key'in sağındaki child/key bölümünü alır.
     */
    private SplitResult<K> splitInternal(BPlusTreeInternalNode<K> internal) {
        List<K> originalKeys = new ArrayList<>(internal.getKeys());
        List<BPlusTreeNode<K>> originalChildren = new ArrayList<>(internal.getChildren());

        int promoteIndex = originalKeys.size() / 2;
        K separatorKey = originalKeys.get(promoteIndex);

        BPlusTreeInternalNode<K> rightInternal = new BPlusTreeInternalNode<>(order);

        internal.clearKeys();
        internal.clearChildren();

        for (int i = 0; i < promoteIndex; i++) {
            internal.addSeparatorKey(internal.getKeyCount(), originalKeys.get(i));
        }

        for (int i = 0; i <= promoteIndex; i++) {
            internal.addChild(originalChildren.get(i));
        }

        for (int i = promoteIndex + 1; i < originalKeys.size(); i++) {
            rightInternal.addSeparatorKey(
                    rightInternal.getKeyCount(),
                    originalKeys.get(i)
            );
        }

        for (int i = promoteIndex + 1; i < originalChildren.size(); i++) {
            rightInternal.addChild(originalChildren.get(i));
        }

        return new SplitResult<>(separatorKey, rightInternal);
    }

    /**
     * Range taramasını leaf linked-list yapısı üzerinden gerçekleştirir.
     * Başlangıç sınırı varsa doğrudan ilgili leaf'ten başlanır; aksi halde en
     * soldaki leaf bulunur. Bitiş sınırı geçildiğinde traversal sonlandırılır.
     */
    private List<RecordPointer> collectRange(
            K fromKey,
            boolean fromInclusive,
            K toKey,
            boolean toInclusive
    ) {
        List<RecordPointer> result = new ArrayList<>();

        BPlusTreeLeafNode<K> leaf = fromKey == null
                ? findFirstLeaf()
                : findLeaf(fromKey);

        while (leaf != null) {
            for (int i = 0; i < leaf.getKeyCount(); i++) {
                K currentKey = leaf.getKey(i);

                if (fromKey != null) {
                    int lowerCompare = currentKey.compareTo(fromKey);

                    if (lowerCompare < 0
                            || (lowerCompare == 0 && !fromInclusive)) {
                        continue;
                    }
                }

                if (toKey != null) {
                    int upperCompare = currentKey.compareTo(toKey);

                    if (upperCompare > 0
                            || (upperCompare == 0 && !toInclusive)) {
                        return result;
                    }
                }

                result.addAll(leaf.getPointers(i));
            }

            leaf = leaf.getNextLeaf();
        }

        return result;
    }

    /**
     * Tree'nin en soldaki leaf node'unu döndürür.
     */
    private BPlusTreeLeafNode<K> findFirstLeaf() {
        BPlusTreeNode<K> current = root;

        while (!current.isLeaf()) {
            BPlusTreeInternalNode<K> internal = castToInternal(current);

            if (internal.getChildCount() == 0) {
                throw new IllegalStateException(
                        "Internal node must contain at least one child."
                );
            }

            current = internal.getChild(0);
        }

        return castToLeaf(current);
    }

    /**
     * Internal node separator key'lerini kullanarak leaf'e kadar iner.
     * B+ Tree separator semantiği gereği key separator'a eşitse sağ child seçilir.
     */
    private BPlusTreeLeafNode<K> findLeaf(K key) {
        BPlusTreeNode<K> current = root;

        while (!current.isLeaf()) {
            BPlusTreeInternalNode<K> internal = castToInternal(current);
            int childIndex = findChildIndex(internal, key);
            current = internal.getChild(childIndex);
        }

        return castToLeaf(current);
    }

    /**
     * Separator key'lere göre uygun child index'ini hesaplar.
     */
    private int findChildIndex(BPlusTreeInternalNode<K> internal, K key) {
        int childIndex = 0;

        while (childIndex < internal.getKeyCount()
                && key.compareTo(internal.getKey(childIndex)) >= 0) {
            childIndex++;
        }

        return childIndex;
    }

    @SuppressWarnings("unchecked")
    private BPlusTreeInternalNode<K> castToInternal(BPlusTreeNode<K> node) {
        if (node.isLeaf()) {
            throw new IllegalStateException("Leaf node cannot be used as an internal node.");
        }
        return (BPlusTreeInternalNode<K>) node;
    }

    @SuppressWarnings("unchecked")
    private BPlusTreeLeafNode<K> castToLeaf(BPlusTreeNode<K> node) {
        if (!node.isLeaf()) {
            throw new IllegalStateException("Internal node cannot be used as a leaf node.");
        }
        return (BPlusTreeLeafNode<K>) node;
    }

    private void validateOrder(int order) {
        if (order < MIN_ORDER) {
            throw new IllegalArgumentException(
                    "B+ Tree order must be at least " + MIN_ORDER + ". Provided: " + order
            );
        }
    }

    private void validateKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("B+ Tree key cannot be null.");
        }
    }

    private void validatePointer(RecordPointer pointer) {
        if (pointer == null || !pointer.isValid()) {
            throw new IllegalArgumentException("A valid RecordPointer must be provided.");
        }
    }

    /**
     * Delete traversal sırasında leaf ile birlikte parent yolunu taşır.
     */
    private static final class DeletionPath<K extends Comparable<K>> {

        private final BPlusTreeLeafNode<K> leaf;
        private final List<BPlusTreeInternalNode<K>> parents;
        private final List<Integer> childIndexes;

        private DeletionPath(
                BPlusTreeLeafNode<K> leaf,
                List<BPlusTreeInternalNode<K>> parents,
                List<Integer> childIndexes
        ) {
            this.leaf = leaf;
            this.parents = parents;
            this.childIndexes = childIndexes;
        }
    }

    /**
     * Bir node split olduğunda parent'a taşınacak separator ve right node'u tutar.
     */
    private static final class SplitResult<K extends Comparable<K>> {

        private final K separatorKey;
        private final BPlusTreeNode<K> rightNode;

        private SplitResult(K separatorKey, BPlusTreeNode<K> rightNode) {
            this.separatorKey = separatorKey;
            this.rightNode = rightNode;
        }
    }

    /**
     * Recursive insert sonucunda yeni key bilgisini ve varsa split sonucunu taşır.
     */
    private static final class InsertResult<K extends Comparable<K>> {

        private final boolean newKey;
        private final SplitResult<K> splitResult;

        private InsertResult(boolean newKey, SplitResult<K> splitResult) {
            this.newKey = newKey;
            this.splitResult = splitResult;
        }

        private static <K extends Comparable<K>> InsertResult<K> withoutSplit(boolean newKey) {
            return new InsertResult<>(newKey, null);
        }

        private static <K extends Comparable<K>> InsertResult<K> withSplit(
                boolean newKey,
                SplitResult<K> splitResult
        ) {
            return new InsertResult<>(newKey, splitResult);
        }
    }

    @Override
    public String toString() {
        return "BPlusTree{" +
                "order=" + order +
                ", height=" + getHeight() +
                ", rootType=" + (root.isLeaf() ? "LEAF" : "INTERNAL") +
                ", rootKeyCount=" + root.getKeyCount() +
                '}';
    }
}
