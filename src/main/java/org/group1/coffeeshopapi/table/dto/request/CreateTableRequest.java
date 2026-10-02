package org.group1.coffeeshopapi.table.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.TableSize;

public record CreateTableRequest(
        @NotBlank(message = "Table number is required")
        @Size(max = 20, message = "Table number must not exceed 20 characters")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Table number may only contain letters, digits and dashes")
        String tableNumber,
        @NotNull(message = "Table size is required")
        TableSize size,
        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 50, message = "Capacity must not exceed 50")
        Integer capacity
) {
}
