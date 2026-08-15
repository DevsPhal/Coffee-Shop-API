package org.group1.coffeeshopapi.telegram.service;


import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.entity.TelegramLink;
import org.group1.coffeeshopapi.telegram.repository.TelegramLinkRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramLinkService {

    private static final String REDIS_PREFIX = "tg:link_code:";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;
    private final TelegramLinkRepository telegramLinkRepository;
    private final TelegramProperties properties;

    public TelegramLinkService(RedisTemplate<String, String> redisTemplate,
                               TelegramLinkRepository telegramLinkRepository,
                               TelegramProperties properties) {
        this.redisTemplate = redisTemplate;
        this.telegramLinkRepository = telegramLinkRepository;
        this.properties = properties;
    }

    public String generateLinkCode(UUID customerId) {
        String code = randomCode(6);
        String key = REDIS_PREFIX + code;
        redisTemplate.opsForValue().set(key, String.valueOf(customerId),
                Duration.ofSeconds(properties.getLinkCodeTtlSeconds()));
        return code;
    }

    public LinkResult resolveCode(String code, Long chatId, String telegramUsername) {
        String key = REDIS_PREFIX + code.toUpperCase();
        String customerIdStr = redisTemplate.opsForValue().get(key);

        if (customerIdStr == null) {
            return LinkResult.expiredOrInvalid();
        }

        UUID customerId = UUID.fromString(customerIdStr);
        redisTemplate.delete(key);

        telegramLinkRepository.findByChatId(chatId).ifPresent(telegramLinkRepository::delete);
        telegramLinkRepository.findByCustomerId(customerId).ifPresent(telegramLinkRepository::delete);

        TelegramLink link = new TelegramLink();
        link.setCustomerId(customerId);
        link.setChatId(chatId);
        link.setTelegramUsername(telegramUsername);
        telegramLinkRepository.save(link);

        return LinkResult.success(customerId);
    }

    public Optional<Long> findChatIdForCustomer(UUID customerId) {
        return telegramLinkRepository.findByCustomerId(customerId).map(TelegramLink::getChatId);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public record LinkResult(boolean ok, UUID customerId) {
        static LinkResult success(UUID customerId) { return new LinkResult(true, customerId); }
        static LinkResult expiredOrInvalid() { return new LinkResult(false, null); }
    }
}
