package com.example.demo.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransactionStatsResponse {

    private long totalAccounts;
    private long totalTransactions;
    private List<AccountTypeStatResponse> byAccountType;
}
