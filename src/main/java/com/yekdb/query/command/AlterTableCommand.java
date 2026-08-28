package com.yekdb.query.command;

import java.util.Objects;

/**
 * ALTER TABLE komutunu temsil eder.
 *
 * Örnek:
 * ALTER TABLE users ADD COLUMN age INT;
 */

public final class AlterTableCommand implements Command{
    private final String tableName;
    private final AlterTableAction action;

    public AlterTableCommand(String tableName,AlterTableAction action){
        if (tableName == null|| tableName.isBlank()){
            throw new IllegalArgumentException("Table name cannot be null.");
        }
        this.tableName=tableName.trim();
        this.action=Objects.requireNonNull(action,"Alter table action cannot be null.");
    }
    public String tableName(){
        return tableName;
    }
    public AlterTableAction action(){
        return action;
    }
    @Override
    public String toString(){
        return "AlterTableCommand{" +
                "tableName='" + tableName + '\'' +
                ", action=" + action +
                '}';
    }
}
