package org.group1.coffeeshopapi.banner.dto.request;

import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.Status;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record UpdateBannerRequest(
        @Pattern(regexp = ValidationPatterns.NOT_BLANK_REGEX, message = "Title can't be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        @Size(max = 255, message = "Link URL must not exceed 255 characters")
        String linkUrl,
        @Min(value = 0, message = "Sort order must not be negative")
        Integer sortOrder,
        Status status
) {
}
