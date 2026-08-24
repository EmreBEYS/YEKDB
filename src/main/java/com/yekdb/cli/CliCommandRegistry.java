package com.yekdb.cli;

import com.yekdb.cli.exception.UnknownCliCommandException;

import java.util.*;

public final class CliCommandRegistry {

    private final Map<String, CliCommand> commands;

    public CliCommandRegistry() {
        this.commands = new LinkedHashMap<>();
    }

    public void register(CliCommand command) {

        Objects.requireNonNull(
                command,
                "command cannot be null"
        );

        String name = normalizeName(command.name());

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "CLI command name cannot be blank"
            );
        }

        commands.put(name, command);
    }

    public Optional<CliCommand> find(String commandName) {

        if (commandName == null || commandName.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                commands.get(normalizeName(commandName))
        );
    }

    public CliCommand require(String commandName) {

        return find(commandName)
                .orElseThrow(
                        () -> new UnknownCliCommandException(commandName)
                );
    }

    public boolean contains(String commandName) {
        return find(commandName).isPresent();
    }

    public Collection<CliCommand> commands() {
        return List.copyOf(commands.values());
    }

    public int size() {
        return commands.size();
    }

    private String normalizeName(String commandName) {

        return Objects.requireNonNull(
                        commandName,
                        "commandName cannot be null"
                )
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}