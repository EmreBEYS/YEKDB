package com.yekdb.query.command;

/**
 * ALTER TABLE ... ADD COLUMN ...
 */

public final class AddColumnAlterAction implements AlterTableAction{
    private final String columnName;
    private final String dataType;

    public AddColumnAlterAction(String columnName,String dataType){
        if(columnName==null  || columnName.isBlank()){
            throw new IllegalArgumentException("Column name cannot be null.");
        }
        if(dataType == null || dataType.isBlank()){
            throw new IllegalArgumentException("Column data type cannot be null.");
        }
        this.columnName=columnName;
        this.dataType=dataType;
    }
    public String columnName(){
        return columnName;
    }
    public String dataType(){
        return dataType;
    }
    @Override
    public String toString() {
        return "AddColumnAlterAction{" +
                "columnName='" + columnName + '\'' +
                ", dataType='" + dataType + '\'' +
                '}';
    }

}
