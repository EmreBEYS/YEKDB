package com.yekdb.query.command;

/**
 * SHOW TRIGGERS yönetim komutunu temsil eder.
 *
 * Desteklenen biçimler:
 *
 * SHOW TRIGGERS
 * SHOW TRIGGERS FROM table_name
 */
public final class ShowTriggersCommand implements Command {

    private final String tableName;

    public ShowTriggersCommand() {
        this(null);
    }

    public ShowTriggersCommand(
            String tableName
    ) {
        if (tableName != null
                && tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        this.tableName =
                tableName == null
                        ? null
                        : tableName.trim();
    }

    public String getTableName() {
        return tableName;
    }

    public boolean hasTableName() {
        return tableName != null;
    }

    @Override
    public String toString() {
        return "ShowTriggersCommand{" +
                "tableName='" + tableName + '\'' +
                '}';
    }
}
