package com.example.demo.controller;

import com.example.demo.dto.request.recurring.CreateRecurringTransactionRequest;
import com.example.demo.dto.request.recurring.UpdateRecurringTransactionRequest;
import com.example.demo.dto.request.recurring.ChangeRecurringStatusRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.RecurringTransactionResponse;
import com.example.demo.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
@Tag(name = "Recurring Transactions", description = "Scheduled recurring transactions (deposits and transfers)")
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @PostMapping
    @Operation(summary = "Create a new recurring transaction schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecurringTransactionResponse> create(@Valid @RequestBody CreateRecurringTransactionRequest request) {
        return ApiResponse.created(service.create(request));
    }

    @GetMapping
    @Operation(summary = "Search recurring transaction schedules")
    public ApiResponse<PageResponse<RecurringTransactionResponse>> search(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) com.example.demo.entity.enums.RecurringTransactionStatus status,
            Pageable pageable) {
        Page<RecurringTransactionResponse> page = service.search(accountNumber, transactionType, status, pageable);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring transaction by ID")
    public ApiResponse<RecurringTransactionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update recurring transaction configuration")
    public ApiResponse<RecurringTransactionResponse> update(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateRecurringTransactionRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change recurring transaction status (ACTIVE/PAUSED/CANCELLED)")
    public ApiResponse<RecurringTransactionResponse> changeStatus(@PathVariable Long id,
                                                                   @Valid @RequestBody ChangeRecurringStatusRequest request) {
        return ApiResponse.ok(service.changeStatus(id, request));
    }

    @PostMapping("/{id}/run-now")
    @Operation(summary = "Execute recurring transaction immediately (for testing)")
    public ApiResponse<RecurringTransactionResponse> runNow(@PathVariable Long id) {
        return ApiResponse.ok(service.runNow(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete recurring transaction schedule")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null, "Deleted");
    }
}