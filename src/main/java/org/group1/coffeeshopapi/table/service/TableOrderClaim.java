package org.group1.coffeeshopapi.table.service;

import java.util.UUID;

// An order asking to sit at a table.
//   source         POS (staff at the counter) or CUSTOMER (the dine-in QR menu, so the customer is at the table).
//   forSeatedParty staff confirmed the order is for the party already at the table (another round).
//   override       an admin bypasses the table rules; overrideReason is required and audited.
public record TableOrderClaim(
        String tableNumber,
        Source source,
        boolean forSeatedParty,
        boolean override,
        String overrideReason,
        UUID actorId,
        boolean mayOverride
) {
    public enum Source { POS, CUSTOMER }

    public static TableOrderClaim customer(String tableNumber, UUID customerId) {
        return new TableOrderClaim(tableNumber, Source.CUSTOMER, false, false, null, customerId, false);
    }

    public static TableOrderClaim pos(String tableNumber, boolean forSeatedParty, boolean override,
            String overrideReason, UUID actorId, boolean mayOverride) {
        return new TableOrderClaim(tableNumber, Source.POS, forSeatedParty, override, overrideReason, actorId,
                mayOverride);
    }

    public boolean fromCustomer() {
        return source == Source.CUSTOMER;
    }
}
