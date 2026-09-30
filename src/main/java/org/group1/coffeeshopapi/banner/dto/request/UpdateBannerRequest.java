package org.group1.coffeeshopapi.banner.dto.request;

import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.Status;

public record UpdateBannerRequest(
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        @Size(max = 255, message = "Link URL must not exceed 255 characters")
        String linkUrl,
        Integer sortOrder,
        Status status
) {
}
