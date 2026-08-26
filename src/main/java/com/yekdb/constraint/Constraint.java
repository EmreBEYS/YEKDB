package com.yekdb.constraint;

import java.util.List;

public interface Constraint {

    ConstraintType type();

    List<String> columns();
}