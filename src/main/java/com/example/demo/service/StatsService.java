package com.example.demo.service;

import com.example.demo.dto.response.AccountTransactionStatsResponse;
import com.example.demo.dto.response.AccountTypeStatResponse;
import com.example.demo.entity.enums.AccountType;
import com.example.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public AccountTransactionStatsResponse getAccountTransactionStats() {
        Map<AccountType, long[]> byType = new EnumMap<>(AccountType.class);
        for (AccountType type : AccountType.values()) {
            byType.put(type, new long[]{0L, 0L});
        }

        for (Object[] row : accountRepository.countAccountsAndTransactionsByType()) {
            AccountType type = (AccountType) row[0];
            long accountCount = (Long) row[1];
            long transactionCount = (Long) row[2];
            byType.put(type, new long[]{accountCount, transactionCount});
        }

        List<AccountTypeStatResponse> stats = new ArrayList<>();
        long totalAccounts = 0;
        long totalTransactions = 0;
        for (AccountType type : AccountType.values()) {
            long[] counts = byType.get(type);
            stats.add(AccountTypeStatResponse.builder()
                    .accountType(type.name())
                    .accountCount(counts[0])
                    .transactionCount(counts[1])
                    .build());
            totalAccounts += counts[0];
            totalTransactions += counts[1];
        }

        return AccountTransactionStatsResponse.builder()
                .totalAccounts(totalAccounts)
                .totalTransactions(totalTransactions)
                .byAccountType(stats)
                .build();
    }
}
