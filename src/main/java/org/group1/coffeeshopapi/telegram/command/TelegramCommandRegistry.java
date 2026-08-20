package org.group1.coffeeshopapi.telegram.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TelegramCommandRegistry {
    private final Map<String, TelegramCommand> commands;

    public TelegramCommandRegistry(List<TelegramCommand> commandList) {
        this.commands = commandList.stream()
                .collect(Collectors.toMap(TelegramCommand::name, Function.identity()));
    }

    public Optional<TelegramCommand> find(String name) {
        return Optional.ofNullable(commands.get(name));
    }
}