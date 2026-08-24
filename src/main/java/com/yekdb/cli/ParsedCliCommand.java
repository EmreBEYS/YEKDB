package com.yekdb.cli;

import java.util.Objects;
import java.util.List;

public final class ParsedCliCommand {
    private final String commandName;
    private final List<String> arguments;

    public ParsedCliCommand(String commandName,List<String> arguments){
        this.commandName=Objects.requireNonNull(commandName,"commandName cannot be null.");
        this.arguments=arguments == null ? List.of() :List.copyOf(arguments);
    }

    public String getCommandName(){
        return commandName;
    }
    public  List<String> getArguments(){
        return arguments;
    }
    public  boolean hasArguments(){
        return !arguments.isEmpty();
    }
    public int argumentCount(){
        return arguments.size();
    }
    @Override
    public String toString() {
        return "ParsedCliCommand{" +
                "commandName='" + commandName + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
