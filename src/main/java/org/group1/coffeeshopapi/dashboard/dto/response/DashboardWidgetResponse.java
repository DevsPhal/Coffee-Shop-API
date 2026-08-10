package org.group1.coffeeshopapi.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetResponse {
    private UUID id;
    private String name;
    private String type;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
