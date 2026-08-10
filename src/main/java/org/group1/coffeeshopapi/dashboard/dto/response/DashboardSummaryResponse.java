package org.group1.coffeeshopapi.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private double dailySales;
    private long totalOrders;
    private long totalCustomers;
    private long queueItems;
    private long lowStockAlerts;
    private long totalProducts;
    private long activeProducts;
    private long totalStaff;
}