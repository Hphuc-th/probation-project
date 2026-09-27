package com.example.demo.mapper;

import com.example.demo.dto.request.transaction.CreateTransactionRequest;
import com.example.demo.dto.response.TransactionResponse;
import com.example.demo.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountNumber(transaction.getAccount() != null ? transaction.getAccount().getAccountNumber() : null)
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null)
                .fee(transaction.getFee())
                .location(transaction.getLocation())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    public Transaction toEntity(CreateTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setFee(request.getFee());
        transaction.setLocation(request.getLocation());
        return transaction;
    }
}
