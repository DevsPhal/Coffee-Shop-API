package org.group1.coffeeshopapi.dashboard.service.impl;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardSummaryResponse;
import org.group1.coffeeshopapi.dashboard.service.Dashboard;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements Dashboard {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardSummaryResponse getSummary() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.findAllActive().size();
        long totalCustomers = countUsers(Role.CUSTOMER);
        long totalStaff = userRepository.findAll().stream().filter(user -> user.getRole() != Role.CUSTOMER).count();

        return DashboardSummaryResponse.builder()
                .dailySales(0D)
                .totalOrders(0L)
                .totalCustomers(totalCustomers)
                .queueItems(0L)
                .lowStockAlerts(0L)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalStaff(totalStaff)
                .build();
    }

    private long countUsers(Role role) {
        return userRepository.findAll().stream()
                .map(User::getRole)
                .filter(role::equals)
                .count();
    }
}