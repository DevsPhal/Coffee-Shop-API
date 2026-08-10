package org.group1.coffeeshopapi.dashboard.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetRequest {
    private String name;
    private String type;
    private String config;
}
