package org.group1.coffeeshopapi.common.enums;

public enum ReservationStatus {
    BOOKED, SEATED, COMPLETED, CANCELLED, NO_SHOW;

    // Booked and seated reservations still claim their table.
    public boolean isActive() {
        return this == BOOKED || this == SEATED;
    }
}
