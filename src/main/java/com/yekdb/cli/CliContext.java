package com.yekdb.cli;

public final class CliContext {

    private boolean running;

    public CliContext() {
        this.running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public void requestExit() {
        this.running = false;
    }
}