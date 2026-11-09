package org.group1.coffeeshopapi.banner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record CreateBannerRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        @Size(max = 255, message = "Link URL must not exceed 255 characters")
        String linkUrl,
        @Min(value = 0, message = "Sort order must not be negative")
        Integer sortOrder
) {
}
