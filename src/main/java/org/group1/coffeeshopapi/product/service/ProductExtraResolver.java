package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.product.entity.Product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProductExtraResolver {
    private ProductExtraResolver() {}

    public static List<Extra> resolve(Product product, List<UUID> extraIds, List<ProductExtra> activeAttached) {
        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Extra> attachedByExtraId = new LinkedHashMap<>();
        for (ProductExtra attached : activeAttached) {
            attachedByExtraId.put(attached.getExtra().getId(), attached.getExtra());
        }

        List<Extra> resolved = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (UUID extraId : extraIds) {
            if (!seen.add(extraId)) {
                continue;
            }
            Extra extra = attachedByExtraId.get(extraId);
            if (extra == null) {
                throw new InvalidOperationException(
                        "One or more extras aren't available on '" + product.getName() + "'");
            }
            if (extra.getStatus() != Status.ACTIVE) {
                throw new InvalidOperationException("'" + extra.getName() + "' is not available right now");
            }
            if (extra.getQuantityOnHand() != null && extra.getQuantityOnHand().signum() <= 0) {
                throw new InvalidOperationException("'" + extra.getName() + "' is out of stock");
            }
            resolved.add(extra);
        }
        return resolved;
    }
}
