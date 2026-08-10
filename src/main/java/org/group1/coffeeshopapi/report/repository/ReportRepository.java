package org.group1.coffeeshopapi.report.repository;

import org.group1.coffeeshopapi.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
