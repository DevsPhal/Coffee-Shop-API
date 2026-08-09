package org.group1.coffeeshopapi.admin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPatchRequest {

    @Size(min = 1, max = 100, message = "Category name cannot be blank")
    private String name;

    @Size(max = 255, message = "Description is too long")
    private String description;

    private Boolean active;
}
