package org.group1.coffeeshopapi.report.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.report.dto.request.ReportRequest;
import org.group1.coffeeshopapi.report.dto.response.ReportResponse;
import org.group1.coffeeshopapi.report.dto.response.SalesReportResponse;

public interface ReportService {
    ReportResponse createReport(ReportRequest request);
    ReportResponse getReportById(UUID id);
    ReportResponse updateReport(UUID id, ReportRequest request);
    void deleteReport(UUID id);
    List<ReportResponse> getAllReports();
    SalesReportResponse sales(String start, String end);
    SalesReportResponse orders(String start, String end);
    SalesReportResponse inventory();
    String export(String format);
}
