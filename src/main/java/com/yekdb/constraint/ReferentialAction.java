package com.yekdb.constraint;

/**
 * Defines the action applied to referencing rows when a referenced
 * FOREIGN KEY value is deleted or updated.
 *
 * Sprint 00-27 Phase 1.
 */
public enum ReferentialAction {

    RESTRICT,
    CASCADE,
    SET_NULL
}
