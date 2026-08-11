package org.group1.coffeeshopapi.dashboard.repository;

import org.group1.coffeeshopapi.dashboard.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, UUID> {
}
