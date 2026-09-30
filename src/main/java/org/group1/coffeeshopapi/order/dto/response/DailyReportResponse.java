package org.group1.coffeeshopapi.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyReportResponse(
        UUID baristaId,
        String baristaName,
        LocalDate date,
        long totalOrders,
        BigDecimal cashTotal,
        BigDecimal bakongTotal,
        BigDecimal grandTotal
) {
}
