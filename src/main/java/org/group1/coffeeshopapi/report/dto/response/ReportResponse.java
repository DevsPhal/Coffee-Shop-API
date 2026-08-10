package org.group1.coffeeshopapi.report.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class ReportResponse {
    private Long id;
    private String name;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
