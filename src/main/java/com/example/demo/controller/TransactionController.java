package com.example.demo.controller;

import com.example.demo.dto.request.transaction.CreateTransactionRequest;
import com.example.demo.dto.request.transaction.DepositRequest;
import com.example.demo.dto.request.transaction.TransferRequest;
import com.example.demo.dto.request.transaction.WithdrawRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.TransactionResponse;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> search(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<TransactionResponse> page = transactionService.searchTransactions(accountId, accountNumber, transactionType,
                fromDate, toDate, pageable);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(transactionService.getTransactionById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.created(transactionService.createTransaction(request));
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ApiResponse.created(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ApiResponse.created(transactionService.withdraw(request));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ApiResponse.created(transactionService.transfer(request));
    }
}