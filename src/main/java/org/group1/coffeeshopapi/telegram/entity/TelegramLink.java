package org.group1.coffeeshopapi.telegram.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telegram_links", uniqueConstraints = {
        @UniqueConstraint(columnNames = "customer_id"),
        @UniqueConstraint(columnNames = "chat_id")
})
public class TelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
    public String getTelegramUsername() { return telegramUsername; }
    public void setTelegramUsername(String telegramUsername) { this.telegramUsername = telegramUsername; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
}
