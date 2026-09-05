package com.yekdb.query.command;

import java.util.Objects;

/**
 * SHOW PROCEDURES SQL komutunu temsil eder.
 */
public final class ShowProceduresCommand implements Command {

    private final String likePattern;

    public ShowProceduresCommand() {
        this.likePattern = null;
    }

    public ShowProceduresCommand(String likePattern) {
        this.likePattern = Objects.requireNonNull(
                likePattern,
                "LIKE pattern cannot be null."
        ).trim();

        if (this.likePattern.isBlank()) {
            throw new IllegalArgumentException(
                    "LIKE pattern cannot be blank."
            );
        }
    }

    public boolean hasLikePattern() {
        return likePattern != null;
    }

    public String getLikePattern() {
        return likePattern;
    }

    @Override
    public String toString() {
        return "ShowProceduresCommand{" +
                "likePattern='" + likePattern + '\'' +
                '}';
    }
}
