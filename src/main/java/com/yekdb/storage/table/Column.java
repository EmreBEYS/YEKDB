package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;

import java.util.Objects;

/**
 * YEKDB tablosundaki bir sütun tanımını temsil eder.
 *
 * Her sütunun bir adı ve veri tipi bulunur.
 *
 * Column nesnesi oluşturulduğu anda kendi geçerliliğini
 * garanti eder. Böylece sistem içerisinde geçersiz bir
 * sütun nesnesi dolaşamaz.
 *
 * Sürüm: 1.0
 */
public class Column {

    private final String name;
    private final DataType dataType;

    /**
     * Yeni bir sütun oluşturur.
     *
     * @param name     sütun adı
     * @param dataType sütun veri tipi
     */
    public Column(
            String name,
            DataType dataType
    ) {

        this.name =
                ColumnNameValidator.validate(name);

        if (dataType == null) {
            throw new InvalidColumnException(
                    "Data type cannot be null."
            );
        }

        this.dataType = dataType;
    }

    /**
     * Sütun adını döndürür.
     *
     * @return normalize edilmiş sütun adı
     */
    public String getName() {
        return name;
    }

    /**
     * Sütunun veri tipini döndürür.
     *
     * @return veri tipi
     */
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return "Column{" +
                "name='" + name + '\'' +
                ", dataType=" + dataType +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Column column)) {
            return false;
        }

        return name.equals(column.name)
                && dataType == column.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                dataType
        );
    }
}