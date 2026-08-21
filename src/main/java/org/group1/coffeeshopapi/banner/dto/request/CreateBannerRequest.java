package org.group1.coffeeshopapi.banner.dto.request;

public record CreateBannerRequest(
        String title,
        String linkUrl,
        Integer sortOrder
) {
}
