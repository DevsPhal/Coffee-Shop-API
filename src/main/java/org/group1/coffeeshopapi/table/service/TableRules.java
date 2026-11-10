package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.common.enums.TableStatus;

// Which manual status changes staff may make from the tables screen. The admin app keeps the same table in
// src/lib/tableTransitions.ts, so change both together.
//
//              to AVAILABLE   to OCCUPIED    to RESERVED   to CLEANING
// AVAILABLE    -              yes            never         yes
// OCCUPIED     admin          -              never         yes
// RESERVED     yes (*)        admin          -             yes
// CLEANING     yes            yes            never         -
//
// RESERVED is only ever set by a booking, and seating a booked party is a check-in, not a status change.
// (*) only when no booking holds the table any more; otherwise the booking has to be cancelled or missed.
// On top of this table: a party with active orders can't be cleared from its table, and seating anyone
// at a table a booking is holding needs an admin override.
public final class TableRules {

    private TableRules() {}

    public enum Verdict { ALLOWED, ADMIN_OVERRIDE, NEVER }

    public record Decision(Verdict verdict, String message) {
        public static Decision allowed() {
            return new Decision(Verdict.ALLOWED, null);
        }
    }

    public static Decision manualChange(TableStatus from, TableStatus to, String tableNumber) {
        if (from == to) {
            return Decision.allowed();
        }
        if (to == TableStatus.RESERVED) {
            return new Decision(Verdict.NEVER, from == TableStatus.CLEANING
                    ? "Mark table " + tableNumber + " clean. It is held for its booking automatically."
                    : "Add a reservation to hold table " + tableNumber + ".");
        }
        return switch (from) {
            case AVAILABLE, CLEANING -> Decision.allowed();
            case OCCUPIED -> to == TableStatus.AVAILABLE
                    ? new Decision(Verdict.ADMIN_OVERRIDE, "Table " + tableNumber
                            + " must be cleaned before it is available again. Mark the guests as gone first.")
                    : Decision.allowed();
            case RESERVED -> to == TableStatus.OCCUPIED
                    ? new Decision(Verdict.ADMIN_OVERRIDE, "Table " + tableNumber
                            + " is reserved. Check the booked party in from its reservation instead.")
                    : Decision.allowed();
        };
    }
}
