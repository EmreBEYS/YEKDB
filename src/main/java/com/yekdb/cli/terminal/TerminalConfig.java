package com.yekdb.cli.terminal;

import java.util.Objects;

public final class TerminalConfig {
    private final String prompt;
    private final String continuationPrompt;
    private final boolean debugEnabled;

    public TerminalConfig(String prompt,String continuationPrompt,boolean debugEnabled){
        this.prompt=Objects.requireNonNull(prompt,"Terminal prompt cannot be null.");
        this.continuationPrompt=Objects.requireNonNull(continuationPrompt,"continuationPrompt cannot be null.");
        this.debugEnabled=debugEnabled;
    }
    public static TerminalConfig defaultConfig() {
        return new TerminalConfig(
                "yekdb> ",
                "...> ",
                false
        );
    }
    public String getPrompt(){
        return prompt;
    }
    public String getContinuationPrompt(){
        return continuationPrompt;
    }
    public boolean isDebugEnabled(){
        return debugEnabled;
    }
    @Override
    public String toString() {
        return "TerminalConfig{" +
                "prompt='" + prompt + '\'' +
                ", continuationPrompt='" + continuationPrompt + '\'' +
                ", debugEnabled=" + debugEnabled +
                '}';
    }
}
