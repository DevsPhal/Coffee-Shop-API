package org.group1.coffeeshopapi.categories.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryCreateRequest {

    @NotBlank(message = "Category code is required")
    private String code;

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private Boolean isActive = true;
}