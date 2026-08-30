package com.yekdb.query.function.builtin;

import com.yekdb.query.function.FunctionException;
import com.yekdb.query.function.FunctionParameter;
import com.yekdb.query.function.FunctionType;
import com.yekdb.query.function.SqlFunction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class AbsFunction implements SqlFunction {

    @Override
    public String getName() {
        return "ABS";
    }

    @Override
    public FunctionType getType() {
        return FunctionType.SCALAR;
    }

    @Override
    public int getMinimumArgumentCount() {
        return 1;
    }

    @Override
    public int getMaximumArgumentCount() {
        return 1;
    }

    @Override
    public Object execute(List<FunctionParameter> parameters) {
        validateArgumentCount(parameters);

        FunctionParameter parameter = parameters.get(0);

        if (parameter.isNull()) {
            return null;
        }

        Number value = parameter.asNumber();

        if (value instanceof Integer integerValue) {
            if (integerValue == Integer.MIN_VALUE) {
                return Math.abs((long) integerValue);
            }

            return Math.abs(integerValue);
        }

        if (value instanceof Long longValue) {
            if (longValue == Long.MIN_VALUE) {
                return BigInteger.valueOf(longValue).abs();
            }

            return Math.abs(longValue);
        }

        if (value instanceof Double doubleValue) {
            return Math.abs(doubleValue);
        }

        if (value instanceof Float floatValue) {
            return Math.abs(floatValue);
        }

        if (value instanceof Short shortValue) {
            return Math.abs(shortValue.intValue());
        }

        if (value instanceof Byte byteValue) {
            return Math.abs(byteValue.intValue());
        }

        if (value instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue.abs();
        }

        if (value instanceof BigInteger bigIntegerValue) {
            return bigIntegerValue.abs();
        }

        throw new FunctionException(
                "Function ABS does not support numeric type "
                        + value.getClass().getSimpleName() + "."
        );
    }
}