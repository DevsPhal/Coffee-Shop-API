package org.group1.coffeeshopapi.realtime.event;

import org.group1.coffeeshopapi.realtime.dto.StaffCallMessage;

// Raised inside the call/answer transaction; only pushed once it commits. customerEmail is who
// hears back when a call is answered.
public record StaffCallEvent(StaffCallMessage message, String customerEmail) {
}
