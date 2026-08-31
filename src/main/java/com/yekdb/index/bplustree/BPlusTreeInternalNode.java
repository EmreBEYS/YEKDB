package com.yekdb.index.bplustree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B+ Tree internal node yapısıdır.
 *
 * Internal node separator key'leri ve child node referanslarını tutar.
 * Normal durumda child sayısı key sayısından bir fazladır.
 *
 * @param <K> B+ Tree anahtar tipi
 */
public class BPlusTreeInternalNode<K extends Comparable<K>> extends BPlusTreeNode<K> {

    private static final long serialVersionUID = 1L;

    private final List<BPlusTreeNode<K>> children;

    public BPlusTreeInternalNode(int order) {
        super(order);
        this.children = new ArrayList<>();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public int getChildCount() {
        return children.size();
    }

    public BPlusTreeNode<K> getChild(int index) {
        return children.get(index);
    }

    public List<BPlusTreeNode<K>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isChildCapacityFull() {
        return children.size() >= getOrder();
    }

    public boolean isChildOverflow() {
        return children.size() > getOrder();
    }

    void addChild(int index, BPlusTreeNode<K> child) {
        validateChild(child);
        children.add(index, child);
    }

    void addChild(BPlusTreeNode<K> child) {
        validateChild(child);
        children.add(child);
    }

    BPlusTreeNode<K> removeChild(int index) {
        return children.remove(index);
    }

    void addSeparatorKey(int index, K key) {
        addKey(index, key);
    }

    K removeSeparatorKey(int index) {
        return removeKey(index);
    }

    void clearChildren() {
        children.clear();
    }

    public boolean isStructurallyValid() {
        if (keys.isEmpty() && children.isEmpty()) {
            return true;
        }
        return children.size() == keys.size() + 1;
    }

    private void validateChild(BPlusTreeNode<K> child) {
        if (child == null) {
            throw new IllegalArgumentException("B+ Tree child node cannot be null.");
        }
        if (child.getOrder() != getOrder()) {
            throw new IllegalArgumentException("Child order must match internal node order.");
        }
    }

    @Override
    public String toString() {
        return "BPlusTreeInternalNode{" +
                "keys=" + keys +
                ", childCount=" + children.size() +
                '}';
    }
}
