package org.group1.coffeeshopapi.report.repository;

import org.group1.coffeeshopapi.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
}
