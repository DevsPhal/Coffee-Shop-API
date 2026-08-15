package org.group1.coffeeshopapi.telegram.command;


import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;
import org.group1.coffeeshopapi.telegram.repository.TelegramLinkRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnlinkCommand implements TelegramCommand {

    private final TelegramApiClient apiClient;
    private final TelegramLinkRepository linkRepository;

    public UnlinkCommand(TelegramApiClient apiClient, TelegramLinkRepository linkRepository) {
        this.apiClient = apiClient;
        this.linkRepository = linkRepository;
    }

    @Override
    public String name() { return "unlink"; }

    @Override
    @Transactional
    public void execute(TelegramMessage message, String argument) {
        Long chatId = message.getChat().getId();
        boolean existed = linkRepository.findByChatId(chatId)
                .map(link -> { linkRepository.delete(link); return true; })
                .orElse(false);

        apiClient.sendMessage(chatId, existed
                ? "🔓 Unlinked. You won't receive receipts here anymore. Send /start with a new code to relink."
                : "This chat isn't linked to any account.");
    }
}
