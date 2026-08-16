package org.group1.coffeeshopapi.telegram.dto.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramMessage {

    @JsonProperty("message_id")
    private Long messageId;

    private TelegramChat chat;
    private TelegramUser from;
    private String text;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public TelegramChat getChat() { return chat; }
    public void setChat(TelegramChat chat) { this.chat = chat; }

    public TelegramUser getFrom() { return from; }
    public void setFrom(TelegramUser from) { this.from = from; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
