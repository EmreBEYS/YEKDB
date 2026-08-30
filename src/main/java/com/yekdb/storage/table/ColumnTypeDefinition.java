package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and represents SQL column type declarations used by YEKDB.
 *
 * Sprint 00-28 Phase 9:
 * CHAR(n), VARCHAR(n), TEXT and BOOLEAN support.
 *
 * Sprint 00-28 Phase 10:
 * FLOAT(n), NUMERIC, NUMERIC(p) and NUMERIC(p,s) support.
 *
 * Sprint 00-28 Phase 11:
 * UUID, DATE, TIME, TIMESTAMP and INTERVAL support.
 *
 * Sprint 00-28 Phase 12:
 * JSON and one-dimensional ARRAY support.
 *
 * Sprint 00-28 Phase 13:
 * HSTORE support.
 */
public final class ColumnTypeDefinition {

    private static final Pattern SIZED_CHARACTER_TYPE = Pattern.compile(
            "^(CHAR|VARCHAR)\\s*\\(\\s*(\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FLOAT_TYPE = Pattern.compile(
            "^FLOAT\\s*\\(\\s*(\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NUMERIC_TYPE = Pattern.compile(
            "^(NUMERIC|DECIMAL)\\s*\\(\\s*(\\d+)\\s*(?:,\\s*(\\d+)\\s*)?\\)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UDT_TYPE = Pattern.compile(
            "^UDT\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)$",
            Pattern.CASE_INSENSITIVE
    );

    private final DataType dataType;
    private final Integer length;
    private final Integer precision;
    private final Integer scale;
    private final ColumnTypeDefinition arrayElementType;
    private final String userDefinedTypeName;

    public ColumnTypeDefinition(DataType dataType, Integer length) {
        this(dataType, length, null, null, null, null);
    }

    public ColumnTypeDefinition(
            DataType dataType,
            Integer length,
            Integer precision,
            Integer scale
    ) {
        this(dataType, length, precision, scale, null, null);
    }

    public ColumnTypeDefinition(
            DataType dataType,
            Integer length,
            Integer precision,
            Integer scale,
            ColumnTypeDefinition arrayElementType
    ) {
        this(dataType, length, precision, scale, arrayElementType, null);
    }

    public ColumnTypeDefinition(
            DataType dataType,
            Integer length,
            Integer precision,
            Integer scale,
            ColumnTypeDefinition arrayElementType,
            String userDefinedTypeName
    ) {
        if (dataType == null) {
            throw new InvalidColumnException("Data type cannot be null.");
        }

        if (dataType == DataType.ARRAY) {
            if (arrayElementType == null) {
                throw new InvalidColumnException("ARRAY element type cannot be null.");
            }
            if (arrayElementType.dataType() == DataType.ARRAY) {
                throw new InvalidColumnException(
                        "Multi-dimensional ARRAY types are not supported in V1."
                );
            }
            if (arrayElementType.dataType() == DataType.JSON
                    || arrayElementType.dataType() == DataType.HSTORE
                    || arrayElementType.dataType() == DataType.UDT) {
                throw new InvalidColumnException(
                        arrayElementType.dataType() + "[] is not supported in V1."
                );
            }
            if (length != null || precision != null || scale != null) {
                throw new InvalidColumnException(
                        "ARRAY metadata belongs to its element type."
                );
            }
        } else if (arrayElementType != null) {
            throw new InvalidColumnException(
                    "Array element type is only supported for ARRAY columns."
            );
        }

        if (dataType == DataType.UDT) {
            if (userDefinedTypeName == null || userDefinedTypeName.isBlank()) {
                throw new InvalidColumnException("UDT name cannot be null or blank.");
            }
            if (!userDefinedTypeName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new InvalidColumnException("Invalid UDT name: " + userDefinedTypeName);
            }
            if (length != null || precision != null || scale != null || arrayElementType != null) {
                throw new InvalidColumnException("UDT columns cannot define length, precision, scale or ARRAY metadata in V1.");
            }
        } else if (userDefinedTypeName != null) {
            throw new InvalidColumnException("UDT name is only supported for UDT columns.");
        }

        if (dataType == DataType.CHAR || dataType == DataType.VARCHAR) {
            if (length == null || length <= 0) {
                throw new InvalidColumnException(
                        dataType + " length must be greater than zero."
                );
            }
            if (precision != null || scale != null) {
                throw new InvalidColumnException(
                        "Precision/scale cannot be specified for " + dataType + "."
                );
            }
        } else if (length != null) {
            throw new InvalidColumnException(
                    "Length is only supported for CHAR and VARCHAR."
            );
        }

        if (dataType == DataType.DOUBLE && precision != null) {
            if (precision < 1 || precision > 53) {
                throw new InvalidColumnException(
                        "FLOAT precision must be between 1 and 53."
                );
            }
            if (scale != null) {
                throw new InvalidColumnException(
                        "FLOAT does not support scale."
                );
            }
        } else if (dataType == DataType.NUMERIC) {
            validateNumericDefinition(precision, scale);
        } else if (precision != null || scale != null) {
            throw new InvalidColumnException(
                    "Precision/scale is not supported for " + dataType + "."
            );
        }

        this.dataType = dataType;
        this.length = length;
        this.precision = precision;
        this.scale = scale;
        this.arrayElementType = arrayElementType;
        this.userDefinedTypeName = userDefinedTypeName == null
                ? null
                : userDefinedTypeName.trim().toLowerCase(Locale.ROOT);
    }

    public static ColumnTypeDefinition parse(String declaration) {
        if (declaration == null || declaration.isBlank()) {
            throw new InvalidColumnException("Column data type cannot be null or blank.");
        }

        String normalized = declaration.trim().toUpperCase(Locale.ROOT);

        if (normalized.endsWith("[]")) {
            String elementDeclaration = normalized.substring(0, normalized.length() - 2).trim();
            if (elementDeclaration.isEmpty()) {
                throw new InvalidColumnException("ARRAY element type cannot be empty: " + declaration);
            }
            ColumnTypeDefinition elementType = parse(elementDeclaration);
            return new ColumnTypeDefinition(DataType.ARRAY, null, null, null, elementType);
        }

        Matcher udtMatcher = UDT_TYPE.matcher(normalized);
        if (udtMatcher.matches()) {
            return new ColumnTypeDefinition(
                    DataType.UDT, null, null, null, null, udtMatcher.group(1)
            );
        }

        Matcher characterMatcher = SIZED_CHARACTER_TYPE.matcher(normalized);
        if (characterMatcher.matches()) {
            DataType type = DataType.valueOf(characterMatcher.group(1));
            int length = parsePositiveInt(characterMatcher.group(2), "character type length", declaration);
            return new ColumnTypeDefinition(type, length);
        }

        Matcher floatMatcher = FLOAT_TYPE.matcher(normalized);
        if (floatMatcher.matches()) {
            int precision = parsePositiveInt(floatMatcher.group(1), "FLOAT precision", declaration);
            return new ColumnTypeDefinition(DataType.DOUBLE, null, precision, null);
        }

        Matcher numericMatcher = NUMERIC_TYPE.matcher(normalized);
        if (numericMatcher.matches()) {
            int precision = parsePositiveInt(numericMatcher.group(2), "NUMERIC precision", declaration);
            String scaleGroup = numericMatcher.group(3);
            int scale = scaleGroup == null
                    ? 0
                    : parseNonNegativeInt(scaleGroup, "NUMERIC scale", declaration);
            return new ColumnTypeDefinition(DataType.NUMERIC, null, precision, scale);
        }

        if (normalized.startsWith("UDT(")) {
            throw new InvalidColumnException("Invalid UDT type declaration: " + declaration);
        }
        if (normalized.startsWith("CHAR(") || normalized.startsWith("VARCHAR(")) {
            throw new InvalidColumnException("Invalid character type declaration: " + declaration);
        }
        if (normalized.startsWith("FLOAT(")) {
            throw new InvalidColumnException("Invalid FLOAT type declaration: " + declaration);
        }
        if (normalized.startsWith("NUMERIC(") || normalized.startsWith("DECIMAL(")) {
            throw new InvalidColumnException("Invalid NUMERIC type declaration: " + declaration);
        }

        DataType type = switch (normalized) {
            case "INT", "INTEGER" -> DataType.INT;
            case "LONG", "BIGINT" -> DataType.LONG;
            case "DOUBLE", "FLOAT", "REAL" -> DataType.DOUBLE;
            case "NUMERIC", "DECIMAL" -> DataType.NUMERIC;
            case "BOOLEAN", "BOOL" -> DataType.BOOLEAN;
            case "STRING" -> DataType.STRING;
            case "TEXT" -> DataType.TEXT;
            case "UUID" -> DataType.UUID;
            case "DATE" -> DataType.DATE;
            case "TIME" -> DataType.TIME;
            case "TIMESTAMP" -> DataType.TIMESTAMP;
            case "INTERVAL" -> DataType.INTERVAL;
            case "JSON" -> DataType.JSON;
            case "HSTORE" -> DataType.HSTORE;
            default -> throw new InvalidColumnException(
                    "Unsupported data type: " + declaration
            );
        };

        return new ColumnTypeDefinition(type, null, null, null);
    }

    private static void validateNumericDefinition(Integer precision, Integer scale) {
        if (precision == null) {
            if (scale != null) {
                throw new InvalidColumnException(
                        "NUMERIC scale requires precision."
                );
            }
            return;
        }

        if (precision <= 0) {
            throw new InvalidColumnException(
                    "NUMERIC precision must be greater than zero."
            );
        }

        if (scale == null || scale < 0) {
            throw new InvalidColumnException(
                    "NUMERIC scale must be zero or greater."
            );
        }

        if (scale > precision) {
            throw new InvalidColumnException(
                    "NUMERIC scale cannot be greater than precision."
            );
        }
    }

    private static int parsePositiveInt(String value, String label, String declaration) {
        int result = parseNonNegativeInt(value, label, declaration);
        if (result <= 0) {
            throw new InvalidColumnException(label + " must be greater than zero: " + declaration);
        }
        return result;
    }

    private static int parseNonNegativeInt(String value, String label, String declaration) {
        try {
            int result = Integer.parseInt(value);
            if (result < 0) {
                throw new InvalidColumnException(label + " cannot be negative: " + declaration);
            }
            return result;
        } catch (NumberFormatException exception) {
            throw new InvalidColumnException("Invalid " + label + ": " + declaration);
        }
    }

    public DataType dataType() {
        return dataType;
    }

    public Integer length() {
        return length;
    }

    public Integer precision() {
        return precision;
    }

    public Integer scale() {
        return scale;
    }

    public ColumnTypeDefinition arrayElementType() {
        return arrayElementType;
    }

    public boolean isArray() {
        return dataType == DataType.ARRAY;
    }

    public String userDefinedTypeName() {
        return userDefinedTypeName;
    }

    public boolean isUserDefinedType() {
        return dataType == DataType.UDT;
    }

    public String declaration() {
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
}
