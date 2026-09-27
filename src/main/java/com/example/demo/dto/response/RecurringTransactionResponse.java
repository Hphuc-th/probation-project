package com.example.demo.dto.response;

import com.example.demo.entity.enums.RecurringTransactionStatus;
import com.example.demo.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {
    private Long id;
    private String accountNumber;
    private String toAccountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private String location;
    private String cronExpression;
    private RecurringTransactionStatus status;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private String lastError;
    private int consecutiveFailures;
    private LocalDateTime createdAt;
}