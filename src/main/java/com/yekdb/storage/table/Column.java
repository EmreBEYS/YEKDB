package com.yekdb.storage.table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * YEKDB tablosundaki bir sütun tanımını temsil eder.
 *
 * Sprint 00-28 Phase 9:
 * CHAR(n) / VARCHAR(n) uzunluk metadata desteği.
 *
 * Sprint 00-28 Phase 10:
 * FLOAT(n) precision ve NUMERIC(p,s) precision/scale metadata desteği.
 *
 * Sprint 00-28 Phase 11:
 * UUID ve temporal textual value validation desteği.
 *
 * Sprint 00-28 Phase 12:
 * JSON ve tek boyutlu ARRAY element metadata/validation desteği.
 *
 * Sprint 00-28 Phase 13:
 * HSTORE textual validation desteği.
 */
public class Column {

    private final String name;
    private final DataType dataType;
    private final Integer length;
    private final Integer precision;
    private final Integer scale;
    private final ColumnTypeDefinition arrayElementType;
    private final String userDefinedTypeName;

    public Column(String name, DataType dataType) {
        this(name, dataType, null, null, null, null, null);
    }

    public Column(String name, DataType dataType, Integer length) {
        this(name, dataType, length, null, null, null, null);
    }

    public Column(
            String name,
            DataType dataType,
            Integer length,
            Integer precision,
            Integer scale
    ) {
        this(name, dataType, length, precision, scale, null, null);
    }

    public Column(
            String name,
            DataType dataType,
            Integer length,
            Integer precision,
            Integer scale,
            ColumnTypeDefinition arrayElementType
    ) {
        this(name, dataType, length, precision, scale, arrayElementType, null);
    }

    public Column(
            String name,
            DataType dataType,
            Integer length,
            Integer precision,
            Integer scale,
            ColumnTypeDefinition arrayElementType,
            String userDefinedTypeName
    ) {
        this.name = ColumnNameValidator.validate(name);

        ColumnTypeDefinition typeDefinition =
                new ColumnTypeDefinition(
                        dataType,
                        length,
                        precision,
                        scale,
                        arrayElementType,
                        userDefinedTypeName
                );

        this.dataType = typeDefinition.dataType();
        this.length = typeDefinition.length();
        this.precision = typeDefinition.precision();
        this.scale = typeDefinition.scale();
        this.arrayElementType = typeDefinition.arrayElementType();
        this.userDefinedTypeName = typeDefinition.userDefinedTypeName();
    }

    public static Column fromTypeDefinition(
            String name,
            ColumnTypeDefinition typeDefinition
    ) {
        Objects.requireNonNull(typeDefinition, "typeDefinition");
        return new Column(
                name,
                typeDefinition.dataType(),
                typeDefinition.length(),
                typeDefinition.precision(),
                typeDefinition.scale(),
                typeDefinition.arrayElementType(),
                typeDefinition.userDefinedTypeName()
        );
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Integer getLength() {
        return length;
    }

    public boolean hasLength() {
        return length != null;
    }

    public Integer getPrecision() {
        return precision;
    }

    public boolean hasPrecision() {
        return precision != null;
    }

    public Integer getScale() {
        return scale;
    }

    public boolean hasScale() {
        return scale != null;
    }

    public ColumnTypeDefinition getArrayElementType() {
        return arrayElementType;
    }

    public boolean isArray() {
        return dataType == DataType.ARRAY;
    }

    public String getUserDefinedTypeName() {
        return userDefinedTypeName;
    }

    public boolean isUserDefinedType() {
        return dataType == DataType.UDT;
    }

    public String getTypeDeclaration() {
        if (dataType == DataType.UDT) {
            return "UDT(" + userDefinedTypeName + ")";
        }
        if (dataType == DataType.ARRAY) {
            return arrayElementType.declaration() + "[]";
        }
        if (length != null) {
            return dataType.name() + "(" + length + ")";
        }
        if (dataType == DataType.NUMERIC && precision != null) {
            return "NUMERIC(" + precision + "," + scale + ")";
        }
        if (dataType == DataType.DOUBLE && precision != null) {
            return "FLOAT(" + precision + ")";
        }
        return dataType.name();
    }

    public boolean acceptsStringLength(String value) {
        if (value == null || length == null) {
            return true;
        }
        return value.length() <= length;
    }

    /**
     * NUMERIC(p,s) kolonları için runtime değerinin precision/scale
     * sınırlarına uyup uymadığını kontrol eder.
     */
    public boolean acceptsNumericValue(Number value) {
        if (value == null || dataType != DataType.NUMERIC || precision == null) {
            return true;
        }

        return acceptsNumericDefinition(value, precision, scale);
    }

    static boolean acceptsNumericDefinition(
            Number value,
            Integer precision,
            Integer scale
    ) {
        if (value == null || precision == null) {
            return true;
        }

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(value.toString()).abs().stripTrailingZeros();
        } catch (NumberFormatException exception) {
            return false;
        }

        if (decimal.signum() == 0) {
            decimal = BigDecimal.ZERO;
        }

        int fractionalDigits = Math.max(decimal.scale(), 0);
        int integerDigits = Math.max(decimal.precision() - decimal.scale(), 0);
        int allowedIntegerDigits = precision - scale;

        return integerDigits <= allowedIntegerDigits
                && fractionalDigits <= scale;
    }

    /**
     * UUID / temporal / JSON / ARRAY / HSTORE kolonları için V1 textual runtime
     * temsilinin geçerli olup olmadığını kontrol eder.
     */
    public boolean acceptsTypedStringValue(String value) {
        if (dataType == DataType.UDT) {
            return true;
        }
        if (dataType == DataType.ARRAY
                || dataType == DataType.JSON
                || dataType == DataType.HSTORE) {
            return SqlStructuredValueValidator.isValid(this, value);
        }
        return SqlTemporalValueValidator.isValid(dataType, value);
    }

    @Override
    public String toString() {
        return "Column{" +
                "name='" + name + '\'' +
                ", dataType=" + getTypeDeclaration() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Column column)) return false;
        return name.equals(column.name)
                && dataType == column.dataType
                && Objects.equals(length, column.length)
                && Objects.equals(precision, column.precision)
                && Objects.equals(scale, column.scale)
                && Objects.equals(userDefinedTypeName, column.userDefinedTypeName)
                && Objects.equals(arrayElementTypeDeclaration(arrayElementType),
                                  arrayElementTypeDeclaration(column.arrayElementType));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                dataType,
                length,
                precision,
                scale,
                userDefinedTypeName,
                arrayElementTypeDeclaration(arrayElementType)
        );
    }

    private static String arrayElementTypeDeclaration(ColumnTypeDefinition definition) {
        return definition == null ? null : definition.declaration();
    }
}
