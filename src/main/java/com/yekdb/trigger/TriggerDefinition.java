package com.yekdb.trigger;

import java.util.Objects;

/**
 * YEKDB trigger tanımını temsil eder.
 */
public final class TriggerDefinition {

    private final String triggerName;
    private final String tableName;
    private final TriggerTiming timing;
    private final TriggerEvent event;
    private final String body;

    public TriggerDefinition(
            String triggerName,
            String tableName,
            TriggerTiming timing,
            TriggerEvent event,
            String body
    ) {
        this.triggerName =
                TriggerNameValidator.validate(
                        triggerName
                );

        this.tableName =
                TriggerNameValidator.validateObjectName(
                        tableName,
                        "Table name"
                );

        this.timing = Objects.requireNonNull(
                timing,
                "Trigger timing cannot be null."
        );

        this.event = Objects.requireNonNull(
                event,
                "Trigger event cannot be null."
        );

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "Trigger body cannot be null or blank."
            );
        }

        this.body = body.trim();
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

    public boolean matches(
            String tableName,
            TriggerTiming timing,
            TriggerEvent event
    ) {
        return this.tableName.equals(
                TriggerNameValidator.validateObjectName(
                        tableName,
                        "Table name"
                )
        )
                && this.timing == timing
                && this.event == event;
    }

    @Override
    public String toString() {
        return "TriggerDefinition{" +
                "triggerName='" + triggerName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", timing=" + timing +
                ", event=" + event +
                ", body='" + body + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof TriggerDefinition that)) {
            return false;
        }

        return triggerName.equals(that.triggerName)
                && tableName.equals(that.tableName)
                && timing == that.timing
                && event == that.event
                && body.equals(that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                triggerName,
                tableName,
                timing,
                event,
                body
        );
    }
}
