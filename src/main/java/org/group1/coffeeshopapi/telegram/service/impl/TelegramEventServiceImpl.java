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
    public void sendUpcomingEvents(Long chatId) {
        List<Event> events = eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(Status.ACTIVE, LocalDateTime.now());
        if (events.isEmpty()) {
            apiClient.sendMessageWithButtons(chatId, "No upcoming events right now — check back soon! ☕");
            return;
        }

        apiClient.sendHtmlMessage(chatId, "🎉 <b>Upcoming Events</b>");
        for (Event event : events) {
            sendEvent(chatId, event, null);
        }
        // Each event above is its own message (possibly a photo/location), so the quick-action
        // keyboard can't ride along with any of them — this trailer is the one place it lands,
        // keeping the rest of the browsing menu one tap away after the list.
        apiClient.sendMessageWithButtons(chatId, "Use the buttons below to keep exploring 👇");
    }

    @Override
    public void announceNewEvent(Event event) {
        List<Customer> recipients = customerRepository.findByTelegramChatIdIsNotNull();
        if (recipients.isEmpty()) {
            return;
        }

        String header = "📢 <b>New Event!</b>\n\n";
        String footer = "\nSend /events to see what else is coming up.";
        broadcast(recipients, event, header, footer);
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

            broadcast(recipients, event, "⏰ <b>Starting Soon!</b>\n\n", "\nDon't miss it!");
        }
    }

    // Sends one event as its own message — a photo with caption when it has a flyer image, plain
    // HTML text otherwise — followed by a dropped pin when it has a venue location.
    private void sendEvent(Long chatId, Event event, String footer) {
        StringBuilder sb = new StringBuilder();
        appendEventBlock(sb, event);
        if (footer != null) {
            sb.append(footer);
        }
        String text = sb.toString();

        if (event.getImageUrl() != null) {
            apiClient.sendPhoto(chatId, event.getImageUrl(), text);
        } else {
            apiClient.sendHtmlMessage(chatId, text);
        }
        if (event.getLatitude() != null && event.getLongitude() != null) {
            apiClient.sendLocation(chatId, event.getLatitude(), event.getLongitude());
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

    private void broadcast(List<Customer> recipients, Event event, String header, String footer) {
        for (Customer customer : recipients) {
            Long chatId = Long.parseLong(customer.getTelegramChatId());
            apiClient.sendHtmlMessage(chatId, header.stripTrailing());
            sendEvent(chatId, event, footer);
        }
    }
}
