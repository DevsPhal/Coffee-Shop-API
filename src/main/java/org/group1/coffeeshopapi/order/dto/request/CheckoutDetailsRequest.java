package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record CheckoutDetailsRequest(
        @NotNull(message = "Choose pickup, delivery or dine in") FulfillmentMethod method,
        @Size(max = 120, message = "Contact name must not exceed 120 characters") String contactName,
        @Size(max = 30, message = "Contact phone must not exceed 30 characters")
        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        String contactPhone,
        @Size(max = 500, message = "Address must not exceed 500 characters") String address,
        @Size(max = 20, message = "Table number must not exceed 20 characters") String tableNumber
) {
    public CheckoutDetailsRequest(FulfillmentMethod method, String contactName, String contactPhone, String address) {
        this(method, contactName, contactPhone, address, null);
    }
}
