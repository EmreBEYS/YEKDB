package com.yekdb.cli.terminal;

public final class SqlStatementBuffer {

    private final StringBuilder buffer;

    public SqlStatementBuffer() {
        this.buffer = new StringBuilder();
    }

    public void append(String line) {
        if (line == null) {
            return;
        }

        if (!buffer.isEmpty()) {
            buffer.append(System.lineSeparator());
        }

        buffer.append(line);
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public boolean isComplete() {
        if (buffer.isEmpty()) {
            return false;
        }

        String statement = buffer.toString().trim();

        return statement.endsWith(";");
    }

    public String getStatement() {
        return buffer.toString();
    }

    public String consume() {
        String statement = getStatement();
        clear();
        return statement;
    }

    public void clear() {
        buffer.setLength(0);
    }

    public int length() {
        return buffer.length();
    }
}