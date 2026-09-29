package com.example.demo.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private String accountNumber;
    private BigDecimal amount;
    private String transactionType;
    private BigDecimal fee;
    private String location;
    private LocalDateTime createdAt;
}
