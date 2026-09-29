package com.example.demo.recurringTransaction.dto.request;

import com.example.demo.recurringTransaction.entity.enums.RecurringTransactionStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRecurringStatusRequest {
    @NotNull
    private RecurringTransactionStatus status;
}