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
public class UpdateRecurringTransactionRequest {
    private BigDecimal amount;
    private BigDecimal fee;
    private String location;
    private String cronExpression;
    private Long toAccountId;
}