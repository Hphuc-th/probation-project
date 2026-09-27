package com.example.demo.dto.request.account;

import com.example.demo.entity.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
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
public class UpdateAccountRequest {

    private AccountType accountType;

    @DecimalMin(value = "0.0", message = "transaction limit must be >= 0")
    private BigDecimal transactionLimit;

    private Long customerId;
}
