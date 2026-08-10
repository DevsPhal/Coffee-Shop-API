package org.group1.coffeeshopapi.report.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class SalesReportResponse {
    private String period;
    private double totalSales;
    private long totalOrders;
    private double averageOrderValue;
    private List<KeyCount> topProducts;
    private List<MethodAmount> paymentMethods;

    @Data
    public static class KeyCount {
        private String name;
        private int count;
    }

    @Data
    public static class MethodAmount {
        private String method;
        private double amount;
    }
}
