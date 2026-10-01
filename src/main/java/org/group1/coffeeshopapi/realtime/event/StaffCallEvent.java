package org.group1.coffeeshopapi.realtime.event;

import org.group1.coffeeshopapi.realtime.dto.StaffCallMessage;

public record StaffCallEvent(StaffCallMessage message, String customerEmail) {
}
