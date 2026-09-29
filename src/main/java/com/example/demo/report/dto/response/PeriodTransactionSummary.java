package com.example.demo.report.dto.response;

import com.example.demo.report.entity.enums.PeriodType;
import com.example.demo.transaction.dto.response.TransactionResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodTransactionSummary {

    private PeriodType periodType;
    private String periodLabel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private long totalTransactions;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalTransfers;
    private BigDecimal totalFees;
    private BigDecimal averageTransactionAmount;
    private BigDecimal minTransactionAmount;
    private BigDecimal maxTransactionAmount;
    private BigDecimal totalNetAmount;
    private BigDecimal averageFee;
    private Long depositCount;
    private Long withdrawCount;
    private Long transferCount;
    private BigDecimal depositAmount;
    private BigDecimal withdrawAmount;
    private BigDecimal transferAmount;
    private List<TransactionResponse> transactions;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}