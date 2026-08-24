package org.group1.coffeeshopapi.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDailyReportResponse(
        LocalDate date,
        long totalOrders,
        BigDecimal cashTotal,
        BigDecimal bakongTotal,
        BigDecimal grandTotal,
        List<DailyReportResponse> baristas
) {
}
