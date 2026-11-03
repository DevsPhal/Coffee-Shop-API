package org.group1.coffeeshopapi.inventory.dto.request;

import java.util.UUID;

// One sold line as stock sees it: so many of a variant, each using its recipe or unitsPerSale of the product itself.
public record SaleLine(UUID productId, UUID variantId, int unitsPerSale, int quantity) {
}
