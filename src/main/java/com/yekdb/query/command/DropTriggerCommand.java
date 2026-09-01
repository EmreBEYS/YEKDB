package com.yekdb.query.command;

import java.util.Objects;

/**
 * DROP TRIGGER SQL komutunu temsil eder.
 */
public final class DropTriggerCommand implements Command {

    private final String triggerName;

    public DropTriggerCommand(String triggerName) {
        this.triggerName = Objects.requireNonNull(
                triggerName,
                "Trigger name cannot be null."
        ).trim();

        if (this.triggerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Trigger name cannot be blank."
            );
        }
    }

    public String getTriggerName() {
        return triggerName;
    }

    @Override
    public String toString() {
        return "DropTriggerCommand{" +
                "triggerName='" + triggerName + '\'' +
                '}';
    }
}
