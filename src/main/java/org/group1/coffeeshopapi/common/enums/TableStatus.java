package org.group1.coffeeshopapi.common.enums;

// AVAILABLE: free for walk-ins. OCCUPIED: a party is seated. RESERVED: held for a booking that starts soon.
// CLEANING: the party's orders are done; the table must be cleared before a new party sits down.
public enum TableStatus {
    AVAILABLE, OCCUPIED, RESERVED, CLEANING
}
