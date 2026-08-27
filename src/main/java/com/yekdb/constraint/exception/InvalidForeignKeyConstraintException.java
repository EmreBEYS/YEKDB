package com.yekdb.constraint.exception;

/**
 * FOREIGN KEY schema tanımı geçersiz olduğunda fırlatılır.
 *
 * <p>Bu exception runtime row violation için değil, CREATE TABLE
 * sırasında foreign key metadata ilişkisinin doğrulanması için kullanılır.</p>
 */
public class InvalidForeignKeyConstraintException
        extends IllegalArgumentException {

    public InvalidForeignKeyConstraintException(
            String message
    ) {
        super(message);
    }

    public InvalidForeignKeyConstraintException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
