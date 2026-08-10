package org.group1.coffeeshopapi.dashboard.repository;

import org.group1.coffeeshopapi.dashboard.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {
}
