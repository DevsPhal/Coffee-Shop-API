package org.group1.coffeeshopapi.banner.dto.request;

import org.group1.coffeeshopapi.common.enums.Status;

public record UpdateBannerRequest(
        String title,
        String linkUrl,
        Integer sortOrder,
        Status status
) {
}
