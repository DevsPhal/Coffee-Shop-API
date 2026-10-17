package org.group1.coffeeshopapi.telegram.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TelegramUser(Long id, String firstName, String lastName, String username) {

    @JsonCreator
    public TelegramUser {
    }

    public TelegramUser(Long id, String firstName, String username) {
        this(id, firstName, null, username);
    }
}
