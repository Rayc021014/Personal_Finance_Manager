package com.skyfl.pfm.report.controller;

import com.skyfl.pfm.common.security.CurrentUserResolver;
import com.skyfl.pfm.report.dto.CategoryBreakdownItem;
import com.skyfl.pfm.report.dto.SummaryResponse;
import com.skyfl.pfm.report.dto.TrendPoint;
import com.skyfl.pfm.report.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null || endDate != null) {
            return reportService.summary(CurrentUserResolver.get().getId(), startDate, endDate);
        }
        if (period != null) {
            return reportService.summary(CurrentUserResolver.get().getId(), period, referenceDate);
        }
        return reportService.summary(CurrentUserResolver.get().getId(), year, month);
    }

    @GetMapping("/trend")
    public List<TrendPoint> trend(@RequestParam String period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return reportService.trend(CurrentUserResolver.get().getId(), period, start, end);
    }

    @GetMapping("/category-breakdown")
    public List<CategoryBreakdownItem> categoryBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null || endDate != null) {
            return reportService.categoryBreakdown(CurrentUserResolver.get().getId(), startDate, endDate);
        }
        if (period != null) {
            return reportService.categoryBreakdown(CurrentUserResolver.get().getId(), period, referenceDate);
        }
        return reportService.categoryBreakdown(CurrentUserResolver.get().getId(), year, month);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportService.exportCsv(CurrentUserResolver.get().getId(), start, end));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + year + "-" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.exportPdf(CurrentUserResolver.get().getId(), year, month));
    }
}
