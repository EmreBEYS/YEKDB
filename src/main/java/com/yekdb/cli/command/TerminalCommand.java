package com.yekdb.cli.command;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TerminalCommand  {
    private final TerminalCommandType type;
    private final List<String> arguments;

    public TerminalCommand(TerminalCommandType type, List<String> arguments){
        this.type=Objects.requireNonNull(type,"The command type cannot be null.");
        this.arguments=arguments == null ? Collections.emptyList() :List.copyOf(arguments);
    }
    public TerminalCommandType getType(){
        return type;
    }
    public List<String> getArguments(){
        return arguments;
    }

    public boolean hasArguments(){
        return !arguments.isEmpty();
    }
    public String getArgument(int index) {
        return arguments.get(index);
    }

    @Override
    public String toString() {
        return "TerminalCommand{" +
                "type=" + type +
                ", arguments=" + arguments +
                '}';
    }
}
