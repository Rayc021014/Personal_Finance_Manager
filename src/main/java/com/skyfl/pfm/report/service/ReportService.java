package com.skyfl.pfm.report.service;

import com.skyfl.pfm.common.util.DateUtils;
import com.skyfl.pfm.report.dto.CategoryBreakdownItem;
import com.skyfl.pfm.report.dto.SummaryResponse;
import com.skyfl.pfm.report.dto.TrendPoint;
import com.skyfl.pfm.transaction.entity.Transaction;
import com.skyfl.pfm.transaction.entity.TransactionType;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    public ReportService(TransactionRepository transactionRepository, CsvExportService csvExportService,
            PdfExportService pdfExportService) {
        this.transactionRepository = transactionRepository;
        this.csvExportService = csvExportService;
        this.pdfExportService = pdfExportService;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID userId, int year, int month) {
        return summarizeRange(userId, DateUtils.monthStart(year, month), DateUtils.monthEnd(year, month));
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID userId, String period, LocalDate referenceDate) {
        DateRange range = resolveRange(period, referenceDate);
        return summarizeRange(userId, range.start(), range.end());
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID userId, LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        return summarizeRange(userId, range.start(), range.end());
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> trend(UUID userId, String period, LocalDate start, LocalDate end) {
        List<Transaction> transactions = transactionRepository.findAllForTrend(userId, start, end);
        Map<String, AmountBucket> buckets = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            String label = labelFor(period, transaction.getTransactionDate());
            AmountBucket bucket = buckets.computeIfAbsent(label, ignored -> new AmountBucket());
            if (transaction.getType() == TransactionType.INCOME) {
                bucket.income = bucket.income.add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                bucket.expense = bucket.expense.add(transaction.getAmount());
            }
        }
        return buckets.entrySet().stream()
                .map(entry -> new TrendPoint(entry.getKey(), entry.getValue().income, entry.getValue().expense))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownItem> categoryBreakdown(UUID userId, int year, int month) {
        return transactionRepository.categoryBreakdown(userId, DateUtils.monthStart(year, month), DateUtils.monthEnd(year, month));
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownItem> categoryBreakdown(UUID userId, String period, LocalDate referenceDate) {
        DateRange range = resolveRange(period, referenceDate);
        return transactionRepository.categoryBreakdown(userId, range.start(), range.end());
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownItem> categoryBreakdown(UUID userId, LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        return transactionRepository.categoryBreakdown(userId, range.start(), range.end());
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID userId, LocalDate start, LocalDate end) {
        List<Transaction> rows = transactionRepository.exportRows(userId, start, end);
        return csvExportService.export(rows, start, end);
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(UUID userId, int year, int month) {
        SummaryResponse summary = summary(userId, year, month);
        return pdfExportService.export(year, month, summary, categoryBreakdown(userId, year, month));
    }

    private String labelFor(String period, LocalDate date) {
        return switch (period.toLowerCase(Locale.ROOT)) {
            case "weekly" -> date.getYear() + "-W" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
            case "yearly" -> String.valueOf(date.getYear());
            case "monthly" -> YearMonth.from(date).toString();
            default -> date.toString();
        };
    }

    private SummaryResponse summarizeRange(UUID userId, LocalDate start, LocalDate end) {
        BigDecimal income = transactionRepository.sumByType(userId, TransactionType.INCOME, start, end);
        BigDecimal expense = transactionRepository.sumByType(userId, TransactionType.EXPENSE, start, end);
        BigDecimal savings = income.subtract(expense);
        BigDecimal rate = income.signum() == 0 ? BigDecimal.ZERO :
                savings.divide(income, 4, RoundingMode.HALF_UP);
        return new SummaryResponse(income, expense, savings, rate);
    }

    private DateRange resolveRange(String period, LocalDate referenceDate) {
        LocalDate anchor = referenceDate == null ? LocalDate.now() : referenceDate;
        return switch (period == null ? "month" : period.toLowerCase(Locale.ROOT)) {
            case "day" -> new DateRange(anchor, anchor);
            case "week" -> new DateRange(DateUtils.weekStart(anchor), DateUtils.weekEnd(anchor));
            case "month" -> new DateRange(anchor.withDayOfMonth(1), anchor.withDayOfMonth(anchor.lengthOfMonth()));
            default -> throw new IllegalArgumentException("Unsupported period: " + period);
        };
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStart = startDate == null ? resolvedEnd : startDate;
        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }
        return new DateRange(resolvedStart, resolvedEnd);
    }

    private static final class AmountBucket {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
