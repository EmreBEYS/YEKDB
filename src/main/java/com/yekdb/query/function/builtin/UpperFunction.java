package com.yekdb.query.function.builtin;

import com.yekdb.query.function.FunctionParameter;
import com.yekdb.query.function.FunctionType;
import com.yekdb.query.function.SqlFunction;

import java.util.List;
import java.util.Locale;

public final class UpperFunction implements SqlFunction {

    @Override
    public String getName() {
        return "UPPER";
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

        return parameter.asString().toUpperCase(Locale.ROOT);
    }
}