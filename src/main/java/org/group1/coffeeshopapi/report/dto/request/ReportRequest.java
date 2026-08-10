package org.group1.coffeeshopapi.report.dto.request;

import java.time.LocalDate;
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
public class ReportRequest {
    private String name;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
