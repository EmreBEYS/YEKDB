package com.yekdb.cli.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TerminalSession {

    private boolean running;
    private final List<String> history;

    public TerminalSession() {
        this.running = false;
        this.history = new ArrayList<>();
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void addHistory(String statement) {
        if (statement == null || statement.isBlank()) {
            return;
        }

        history.add(statement.trim());
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public int getHistorySize() {
        return history.size();
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public void clearHistory() {
        history.clear();
    }
}