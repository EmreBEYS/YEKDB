package com.yekdb.database;

import java.nio.file.Path;
import java.util.Objects;

/**
 * YEKDB içinde açılmış bir veritabanını temsil eder.
 *
 * Bir Veritabanı nesnesi, seçilen veritabanı hakkında çalışma zamanı bilgileri içerir.
 */
public class Database {
    private final String name;
    private final Path databasePath;
    private final DatabaseMetadata metadata;

    public Database(
            String name,
            Path databasePath,
            DatabaseMetadata metadata
    ){
        this.name=validateName(name);
        this.databasePath=Objects.requireNonNull(databasePath,"Database path cannot be null");
        this.metadata=Objects.requireNonNull(metadata,"Database metadata cannot be null");
    }
    public String getName(){
        return name;
    }
    public Path getDatabasePath(){
        return databasePath;
    }
    public DatabaseMetadata getMetadata(){
        return metadata;
    }
    private String validateName(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("Database name cannot be null or blank");
        }
        return name.trim();
    }
    @Override
    public String toString() {

        return "Database{" +
                "name='" + name + '\'' +
                ", databasePath=" + databasePath +
                ", metadata=" + metadata +
                '}';
    }
}
