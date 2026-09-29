package com.example.demo.recurringTransaction.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

import com.example.demo.transaction.entity.enums.TransactionType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecurringTransactionRequest {
    @NotNull
    private Long accountId;

    private Long toAccountId;

    @NotNull
    private TransactionType transactionType;

    @NotNull
    @Positive
    private BigDecimal amount;

    private BigDecimal fee;

    private String location;

    @NotNull
    @Pattern(regexp = "^([0-9*,\\-?/\\s]{1,}){5,6}$", message = "Invalid cron expression format")
    private String cronExpression;
}