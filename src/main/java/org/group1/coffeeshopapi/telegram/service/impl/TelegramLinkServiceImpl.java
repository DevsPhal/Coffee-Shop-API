package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramLinkServiceImpl implements TelegramLinkService {

    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final CustomerRepository customerRepository;
    private final TelegramProperties properties;

    @Override
    public TelegramLinkCodeResponse generateLinkCode(UUID customerId) {
        String code = generateCode();
        redisTemplate.opsForValue().set(
                RedisKeys.TELEGRAM_LINK_CODE_PREFIX + code,
                customerId.toString(),
                Duration.ofSeconds(properties.getLinkCodeTtlSeconds()));
        return new TelegramLinkCodeResponse(code, properties.getLinkCodeTtlSeconds(), properties.deepLink(code));
    }

    @Override
    @Transactional
    public String resolveLinkCode(String code, Long chatId) {
        String key = RedisKeys.TELEGRAM_LINK_CODE_PREFIX + code.toUpperCase();
        String customerId = redisTemplate.opsForValue().get(key);
        if (customerId == null) {
            throw new ResourceNotFoundException("This code is invalid or has expired. Please generate a new one.");
        }
        redisTemplate.delete(key);

        Customer customer = customerRepository.findById(UUID.fromString(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Account no longer exists"));

        customerRepository.findByTelegramChatId(chatId.toString())
                .filter(existing -> !existing.getId().equals(customer.getId()))
                .ifPresent(existing -> {
                    existing.setTelegramChatId(null);
                    customerRepository.save(existing);
                });

        customer.setTelegramChatId(chatId.toString());
        customerRepository.save(customer);
        return "Your account has been linked. You'll receive updates here.";
    }

    @Override
    @Transactional
    public String unlink(Long chatId) {
        Customer customer = customerRepository.findByTelegramChatId(chatId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("This chat is not linked to any account"));
        customer.setTelegramChatId(null);
        customerRepository.save(customer);
        return "Your account has been unlinked.";
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}