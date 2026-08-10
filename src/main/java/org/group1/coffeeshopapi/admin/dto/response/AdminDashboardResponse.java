package org.group1.coffeeshopapi.admin.dto.response;

public class AdminDashboardResponse {

    private long totalProducts;

    private long activeProducts;

    private long totalCategories;

    private long totalUsers;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(long totalProducts, long activeProducts, long totalCategories, long totalUsers) {
        this.totalProducts = totalProducts;
        this.activeProducts = activeProducts;
        this.totalCategories = totalCategories;
        this.totalUsers = totalUsers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getActiveProducts() {
        return activeProducts;
    }

    public void setActiveProducts(long activeProducts) {
        this.activeProducts = activeProducts;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }
}