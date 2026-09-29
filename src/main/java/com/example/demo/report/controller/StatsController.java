package com.example.demo.report.controller;

import com.example.demo.customer.service.CustomerService;
import com.example.demo.report.dto.response.AccountTransactionStatsResponse;
import com.example.demo.report.dto.response.LocationStatResponse;
import com.example.demo.report.dto.response.PeriodTransactionReportResponse;
import com.example.demo.report.entity.enums.PeriodType;
import com.example.demo.report.service.ReportExportService;
import com.example.demo.report.service.StatsService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final CustomerService customerService;
    private final ReportExportService reportExportService;

    @GetMapping("/accounts-transactions")
    public AccountTransactionStatsResponse getAccountTransactionStats() {
        return statsService.getAccountTransactionStats();
    }

    @GetMapping("/customers-by-location")
    public List<LocationStatResponse> getCustomersByLocation() {
        return customerService.getCustomersByLocation();
    }

    @GetMapping("/transactions/report")
    @PreAuthorize("hasRole('ADMIN')")
    public PeriodTransactionReportResponse getTransactionReport(
            @RequestParam PeriodType periodType,
            @RequestParam(required = false) Integer periods,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return statsService.getTransactionReport(periodType, periods, fromDate, toDate, page, size);
    }

    @GetMapping("/transactions/report/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam PeriodType periodType,
            @RequestParam(required = false) Integer periods,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        byte[] excelData = reportExportService.exportToExcel(periodType, periods, fromDate, toDate);
        String filename = String.format("bao-cao-giao-dich-%s-%s.xlsx",
                periodType.name().toLowerCase(), LocalDate.now());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    @GetMapping("/transactions/report/export/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam PeriodType periodType,
            @RequestParam(required = false) Integer periods,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        byte[] pdfData = reportExportService.exportToPdf(periodType, periods, fromDate, toDate);
        String filename = String.format("bao-cao-giao-dich-%s-%s.pdf",
                periodType.name().toLowerCase(), LocalDate.now());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}