package com.yekdb.query.executor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class JoinedRow {
    private final Map<String, Object> values;

    public JoinedRow(){
        this.values=new LinkedHashMap<>();
    }

    public JoinedRow(Map<String,Object> values){
        Objects.requireNonNull(values,"Values cannot be null.");
        this.values=new LinkedHashMap<>(values);
    }

    public void put(String qualifiedColumnName, Object value){
        Objects.requireNonNull(qualifiedColumnName,"qualifiedColumnName cannot be null.");
        if(qualifiedColumnName.isBlank()){
            throw new IllegalArgumentException("qualifiedColumnName cannot be blank.");
        }
        values.put(qualifiedColumnName, value);
    }

    public Object get(String qualifiedColumnName) {
        Objects.requireNonNull(qualifiedColumnName, "qualifiedColumnName cannot be null");
        return values.get(qualifiedColumnName);
    }

    public boolean contains(String qualifiedColumnName) {
        Objects.requireNonNull(qualifiedColumnName, "qualifiedColumnName cannot be null");

        return values.containsKey(qualifiedColumnName);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public String toString() {
        return "JoinedRow{" +
                "values=" + values +
                '}';
    }
}
