package com.yekdb.query.statement;


import java.util.Objects;

public final class TableReference {
    private final String tableName;
    private final String alias;

    public TableReference(String tableName) {
        this(tableName, null);
    }
    public TableReference(String tableName,String alias){
        this.tableName=Objects.requireNonNull(tableName,"Table name cannot be null.");
        this.alias=normalizeAlias(alias);
    }
    public String getTableName(){
        return tableName;
    }
    public String getAlias(){
        return alias;
    }

    public boolean hasAlias(){
        return alias !=null && !alias.isBlank();
    }

    public String getEffectiveName(){
        return hasAlias() ? alias :tableName;
    }

    public boolean matches(String name){
        if(name ==null){
            return false;
        }
        return tableName.equalsIgnoreCase(name)||(hasAlias() && alias.equalsIgnoreCase(name));
    }
    private static String normalizeAlias(String alias){
        if(alias==null){
            return null;
        }
        String normalized=alias.trim();
        return normalized.isEmpty() ?null :normalized;
    }

    @Override
    public String toString(){
        if(hasAlias()){
            return  tableName+ "AS"+alias;
        }
        return tableName;
    }
}
