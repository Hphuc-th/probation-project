package com.example.demo.report.service;

import com.example.demo.account.entity.enums.AccountType;
import com.example.demo.account.repository.AccountRepository;
import com.example.demo.report.dto.response.AccountTransactionStatsResponse;
import com.example.demo.report.dto.response.AccountTypeStatResponse;
import com.example.demo.report.dto.response.PeriodTransactionReportResponse;
import com.example.demo.report.dto.response.PeriodTransactionSummary;
import com.example.demo.report.entity.enums.PeriodType;
import com.example.demo.transaction.dto.response.TransactionResponse;
import com.example.demo.transaction.entity.Transaction;
import com.example.demo.transaction.entity.enums.TransactionType;
import com.example.demo.transaction.mapper.TransactionMapper;
import com.example.demo.transaction.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public AccountTransactionStatsResponse getAccountTransactionStats() {
        Map<AccountType, long[]> byType = new EnumMap<>(AccountType.class);
        for (AccountType type : AccountType.values()) {
            byType.put(type, new long[]{0L, 0L});
        }

        for (Object[] row : accountRepository.countAccountsAndTransactionsByType()) {
            AccountType type = (AccountType) row[0];
            long accountCount = (Long) row[1];
            long transactionCount = (Long) row[2];
            byType.put(type, new long[]{accountCount, transactionCount});
        }

        List<AccountTypeStatResponse> stats = new ArrayList<>();
        long totalAccounts = 0;
        long totalTransactions = 0;
        for (AccountType type : AccountType.values()) {
            long[] counts = byType.get(type);
            stats.add(AccountTypeStatResponse.builder()
                    .accountType(type.name())
                    .accountCount(counts[0])
                    .transactionCount(counts[1])
                    .build());
            totalAccounts += counts[0];
            totalTransactions += counts[1];
        }

        return AccountTransactionStatsResponse.builder()
                .totalAccounts(totalAccounts)
                .totalTransactions(totalTransactions)
                .byAccountType(stats)
                .build();
    }

    @Transactional(readOnly = true)
    public PeriodTransactionReportResponse getTransactionReport(PeriodType periodType,
                                                                 Integer periods,
                                                                 LocalDateTime fromDate,
                                                                 LocalDateTime toDate,
                                                                 int page,
                                                                 int size) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolvedToDate;
        LocalDateTime resolvedFromDate;

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new IllegalArgumentException("fromDate must be before toDate");
            }
            resolvedFromDate = fromDate;
            resolvedToDate = toDate;
        } else {
            int resolvedPeriods = periods != null && periods > 0 ? periods : getDefaultPeriods(periodType);
            resolvedToDate = now;
            resolvedFromDate = calculateFromDate(resolvedToDate, periodType, resolvedPeriods);
        }

        List<Transaction> transactions = transactionRepository.findByDateRange(resolvedFromDate, resolvedToDate);

        Map<String, List<Transaction>> grouped = groupByPeriod(transactions, periodType);

        List<PeriodTransactionSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            List<Transaction> periodTransactions = entry.getValue();
            PeriodTransactionSummary summary = buildSummary(entry.getKey(), periodType, periodTransactions, page, size);
            summaries.add(summary);
        }

        summaries.sort(Comparator.comparing(PeriodTransactionSummary::getPeriodStart).reversed());

        return PeriodTransactionReportResponse.builder()
                .summaries(summaries)
                .build();
    }

    private int getDefaultPeriods(PeriodType periodType) {
        return switch (periodType) {
            case WEEK -> 12;
            case MONTH -> 12;
            case QUARTER -> 4;
        };
    }

    private LocalDateTime calculateFromDate(LocalDateTime toDate, PeriodType periodType, int periods) {
        return switch (periodType) {
            case WEEK -> toDate.minusWeeks(periods);
            case MONTH -> toDate.minusMonths(periods);
            case QUARTER -> toDate.minusMonths(periods * 3L);
        };
    }

    private Map<String, List<Transaction>> groupByPeriod(List<Transaction> transactions, PeriodType periodType) {
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();

        for (Transaction tx : transactions) {
            LocalDateTime createdAt = tx.getCreatedAt();
            String key = getPeriodKey(createdAt, periodType);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }

        return grouped;
    }

    private String getPeriodKey(LocalDateTime dateTime, PeriodType periodType) {
        LocalDate date = dateTime.toLocalDate();
        return switch (periodType) {
            case WEEK -> {
                LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int weekOfYear = weekStart.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                int year = weekStart.get(java.time.temporal.WeekFields.ISO.weekBasedYear());
                yield String.format("%d-W%02d", year, weekOfYear);
            }
            case MONTH -> {
                YearMonth ym = YearMonth.from(date);
                yield ym.toString();
            }
            case QUARTER -> {
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                yield String.format("%d-Q%d", date.getYear(), quarter);
            }
        };
    }

    private LocalDate getPeriodStart(String periodKey, PeriodType periodType) {
        return switch (periodType) {
            case WEEK -> {
                String[] parts = periodKey.split("-W");
                int year = Integer.parseInt(parts[0]);
                int week = Integer.parseInt(parts[1]);
                yield LocalDate.ofYearDay(year, 1)
                        .with(java.time.temporal.WeekFields.ISO.weekBasedYear(), year)
                        .with(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear(), week)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            }
            case MONTH -> {
                YearMonth ym = YearMonth.parse(periodKey);
                yield ym.atDay(1);
            }
            case QUARTER -> {
                String[] parts = periodKey.split("-Q");
                int year = Integer.parseInt(parts[0]);
                int quarter = Integer.parseInt(parts[1]);
                int month = (quarter - 1) * 3 + 1;
                yield LocalDate.of(year, month, 1);
            }
        };
    }

    private LocalDate getPeriodEnd(String periodKey, PeriodType periodType) {
        return switch (periodType) {
            case WEEK -> getPeriodStart(periodKey, periodType).plusDays(6);
            case MONTH -> YearMonth.parse(periodKey).atEndOfMonth();
            case QUARTER -> {
                String[] parts = periodKey.split("-Q");
                int year = Integer.parseInt(parts[0]);
                int quarter = Integer.parseInt(parts[1]);
                int endMonth = quarter * 3;
                yield LocalDate.of(year, endMonth, 1).plusMonths(1).minusDays(1);
            }
        };
    }

    private PeriodTransactionSummary buildSummary(String periodKey,
                                                   PeriodType periodType,
                                                   List<Transaction> transactions,
                                                   int page,
                                                   int size) {
        long totalCount = transactions.size();

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfers = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal minAmount = null;
        BigDecimal maxAmount = null;
        long depositCount = 0;
        long withdrawCount = 0;
        long transferCount = 0;

        for (Transaction tx : transactions) {
            BigDecimal amount = tx.getAmount();
            BigDecimal fee = tx.getFee();
            totalFees = totalFees.add(fee);

            if (minAmount == null || amount.compareTo(minAmount) < 0) {
                minAmount = amount;
            }
            if (maxAmount == null || amount.compareTo(maxAmount) > 0) {
                maxAmount = amount;
            }

            switch (tx.getTransactionType()) {
                case DEPOSIT -> {
                    totalDeposits = totalDeposits.add(amount);
                    depositCount++;
                }
                case WITHDRAW -> {
                    totalWithdrawals = totalWithdrawals.add(amount);
                    withdrawCount++;
                }
                case TRANSFER -> {
                    totalTransfers = totalTransfers.add(amount);
                    transferCount++;
                }
            }
        }

        BigDecimal totalNetAmount = totalDeposits.subtract(totalWithdrawals)
                .subtract(totalTransfers).subtract(totalFees);
        BigDecimal averageTransactionAmount = totalCount > 0
                ? totalNetAmount.divide(BigDecimal.valueOf(totalCount), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal averageFee = totalCount > 0
                ? totalFees.divide(BigDecimal.valueOf(totalCount), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal finalMinAmount = minAmount != null ? minAmount : BigDecimal.ZERO;
        BigDecimal finalMaxAmount = maxAmount != null ? maxAmount : BigDecimal.ZERO;

        List<Transaction> sortedTransactions = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .toList();

        int totalPages = (int) Math.ceil((double) totalCount / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, (int) totalCount);
        List<Transaction> pagedTransactions = (fromIndex < totalCount)
                ? sortedTransactions.subList(fromIndex, toIndex)
                : List.of();

        List<TransactionResponse> transactionResponses = pagedTransactions.stream()
                .map(transactionMapper::toResponse)
                .toList();

        return PeriodTransactionSummary.builder()
                .periodType(periodType)
                .periodLabel(periodKey)
                .periodStart(getPeriodStart(periodKey, periodType))
                .periodEnd(getPeriodEnd(periodKey, periodType))
                .totalTransactions(totalCount)
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .totalTransfers(totalTransfers)
                .totalFees(totalFees)
                .averageTransactionAmount(averageTransactionAmount)
                .minTransactionAmount(finalMinAmount)
                .maxTransactionAmount(finalMaxAmount)
                .totalNetAmount(totalNetAmount)
                .averageFee(averageFee)
                .depositCount(depositCount)
                .withdrawCount(withdrawCount)
                .transferCount(transferCount)
                .depositAmount(totalDeposits)
                .withdrawAmount(totalWithdrawals)
                .transferAmount(totalTransfers)
                .transactions(transactionResponses)
                .currentPage(page)
                .totalPages(totalPages)
                .totalElements(totalCount)
                .build();
    }
}
