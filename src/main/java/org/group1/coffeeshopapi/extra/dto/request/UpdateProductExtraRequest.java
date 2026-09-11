package org.group1.coffeeshopapi.extra.dto.request;

import org.group1.coffeeshopapi.common.enums.Status;

public record UpdateProductExtraRequest(
        Integer sortOrder,
        Status status
) {
}
