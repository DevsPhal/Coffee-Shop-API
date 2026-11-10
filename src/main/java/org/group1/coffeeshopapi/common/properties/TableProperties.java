package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.table")
public class TableProperties {
    private String scanBaseUrl = "https://590stcafe.shop/table/";
    // One QR for the whole shop: opens the menu in dine-in mode and the customer types their table number.
    private String menuScanUrl = "https://590stcafe.shop/dine-in";

    private Reservation reservation = new Reservation();

    @Getter
    @Setter
    public static class Reservation {
        // How long before a booking starts its table is held (shown as reserved, closed to walk-ins).
        private int holdBeforeMinutes = 60;
        // How long a booking keeps its table when no length is given. Two bookings on a table can't overlap.
        private int defaultDurationMinutes = 90;
        // A booked party this late is marked as a no-show and the table is freed. 0 turns this off.
        private int noShowAfterMinutes = 30;
        // How far ahead a table can be booked.
        private int maxAdvanceDays = 60;
    }
}
