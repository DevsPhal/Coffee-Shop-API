package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.TableAuditLog;

// The table an order was given, and the audit entry that records it (filled in with the order id once saved).
public record TableSeating(DiningTable table, TableAuditLog auditLog) {
}
