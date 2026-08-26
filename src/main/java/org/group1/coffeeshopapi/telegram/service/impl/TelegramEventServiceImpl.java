package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramEventServiceImpl implements TelegramEventService {

    private static final DateTimeFormatter EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    // Once an event's reminder fires, this just needs to outlive the 24h lookahead window so a
    // later scheduler run never re-sends it — a week is generous headroom, not a meaningful cost.
    private static final Duration REMINDER_MARK_TTL = Duration.ofDays(7);

    private final EventRepository eventRepository;
    private final CustomerRepository customerRepository;
    private final TelegramApiClient apiClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String buildUpcomingEvents() {
        List<Event> events = eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(Status.ACTIVE, LocalDateTime.now());
        if (events.isEmpty()) {
            return "No upcoming events right now — check back soon! ☕";
        }

        StringBuilder sb = new StringBuilder("🎉 <b>Upcoming Events</b>\n");
        for (Event event : events) {
            sb.append('\n');
            appendEventBlock(sb, event);
        }
        return sb.toString();
    }

    @Override
    public void announceNewEvent(Event event) {
        List<Customer> recipients = customerRepository.findByTelegramChatIdIsNotNull();
        if (recipients.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder("📢 <b>New Event!</b>\n\n");
        appendEventBlock(sb, event);
        sb.append("\nSend /events to see what else is coming up.");
        broadcast(recipients, sb.toString());
    }

    @Override
    public void sendStartingSoonReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> startingSoon = eventRepository.findByStatusAndStartAtBetween(Status.ACTIVE, now, now.plusHours(24));
        if (startingSoon.isEmpty()) {
            return;
        }

        List<Customer> recipients = null;
        for (Event event : startingSoon) {
            String key = RedisKeys.TELEGRAM_EVENT_REMINDER_PREFIX + event.getId();
            boolean firstReminder = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key, "1", REMINDER_MARK_TTL));
            if (!firstReminder) {
                continue;
            }

            // Fetched lazily so a lookahead window with nothing new to remind about never queries
            // the customer table at all.
            if (recipients == null) {
                recipients = customerRepository.findByTelegramChatIdIsNotNull();
            }
            if (recipients.isEmpty()) {
                continue;
            }

            StringBuilder sb = new StringBuilder("⏰ <b>Starting Soon!</b>\n\n");
            appendEventBlock(sb, event);
            sb.append("\nDon't miss it!");
            broadcast(recipients, sb.toString());
        }
    }

    private void appendEventBlock(StringBuilder sb, Event event) {
        sb.append("<b>").append(TelegramFormat.escape(TelegramFormat.titleCase(event.getTitle()))).append("</b>\n");
        sb.append("🗓 ").append(event.getStartAt().format(EVENT_DATE_FORMAT))
                .append(" – ").append(event.getEndAt().format(EVENT_DATE_FORMAT)).append('\n');
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append(TelegramFormat.escape(TelegramFormat.professionalize(event.getDescription()))).append('\n');
        }
    }

    private void broadcast(List<Customer> recipients, String message) {
        for (Customer customer : recipients) {
            apiClient.sendHtmlMessage(Long.parseLong(customer.getTelegramChatId()), message);
        }
    }
}
