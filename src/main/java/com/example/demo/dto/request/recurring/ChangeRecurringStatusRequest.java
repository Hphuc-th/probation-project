package com.example.demo.dto.request.recurring;

import com.example.demo.entity.enums.RecurringTransactionStatus;
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