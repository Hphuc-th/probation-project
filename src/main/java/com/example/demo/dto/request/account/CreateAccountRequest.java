package com.example.demo.dto.request.account;

import com.example.demo.entity.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "account number is required")
    private String accountNumber;

    @NotNull(message = "customer id is required")
    private Long customerId;

    @NotNull(message = "balance is required")
    @PositiveOrZero(message = "balance must be positive or zero")
    private BigDecimal balance;

    @NotNull(message = "account type is required")
    private AccountType accountType;

    @NotNull(message = "transaction limit is required")
    @Positive(message = "transaction limit must be positive")
    private BigDecimal transactionLimit;
}
