package com.yekdb.query.command;

import com.yekdb.trigger.TriggerEvent;
import com.yekdb.trigger.TriggerTiming;

import java.util.Objects;

/**
 * CREATE TRIGGER SQL komutunu temsil eder.
 */
public final class CreateTriggerCommand implements Command {

    private final String triggerName;
    private final String tableName;
    private final TriggerTiming timing;
    private final TriggerEvent event;
    private final String body;

    public CreateTriggerCommand(
            String triggerName,
            String tableName,
            TriggerTiming timing,
            TriggerEvent event,
            String body
    ) {
        this.triggerName = Objects.requireNonNull(
                triggerName,
                "Trigger name cannot be null."
        ).trim();

        if (this.triggerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Trigger name cannot be blank."
            );
        }

        this.tableName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (this.tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        this.timing = Objects.requireNonNull(
                timing,
                "Trigger timing cannot be null."
        );

        this.event = Objects.requireNonNull(
                event,
                "Trigger event cannot be null."
        );

        this.body = Objects.requireNonNull(
                body,
                "Trigger body cannot be null."
        ).trim();

        if (this.body.isBlank()) {
            throw new IllegalArgumentException(
                    "Trigger body cannot be blank."
            );
        }
    }

    public String getTriggerName() {
        return triggerName;
    }

    public String getTableName() {
        return tableName;
    }

    public TriggerTiming getTiming() {
        return timing;
    }

    public TriggerEvent getEvent() {
        return event;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "CreateTriggerCommand{" +
                "triggerName='" + triggerName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", timing=" + timing +
                ", event=" + event +
                ", body='" + body + '\'' +
                '}';
    }
}
