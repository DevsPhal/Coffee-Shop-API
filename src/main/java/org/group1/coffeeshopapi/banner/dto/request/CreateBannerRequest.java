package org.group1.coffeeshopapi.banner.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBannerRequest(
        @NotBlank(message = "Title is required")
        String title,
        String linkUrl,
        Integer sortOrder
) {
}
