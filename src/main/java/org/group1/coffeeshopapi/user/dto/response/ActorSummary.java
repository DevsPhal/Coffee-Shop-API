package org.group1.coffeeshopapi.user.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;

import java.util.UUID;

// Readable identity behind an audit id (StockMovement.performedBy, Product.createdBy, ...) —
// resolved from either a real Admin/Barista row or the fixed Super Admin id.
public record ActorSummary(
        UUID id,
        String name,
        Role role
) {
}
