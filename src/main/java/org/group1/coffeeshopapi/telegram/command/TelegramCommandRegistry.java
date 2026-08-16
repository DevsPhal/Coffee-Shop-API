package org.group1.coffeeshopapi.telegram.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TelegramCommandRegistry {

    private final Map<String, TelegramCommand> commands;

    public TelegramCommandRegistry(List<TelegramCommand> commandBeans) {
        this.commands = commandBeans.stream()
                .collect(Collectors.toMap(TelegramCommand::name, Function.identity()));
    }

    public TelegramCommand find(String name) {
        return commands.get(name.toLowerCase());
    }
}
