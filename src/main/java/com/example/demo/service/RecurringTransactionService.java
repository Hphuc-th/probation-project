package com.example.demo.service;

import com.example.demo.dto.request.recurring.CreateRecurringTransactionRequest;
import com.example.demo.dto.request.recurring.UpdateRecurringTransactionRequest;
import com.example.demo.dto.request.recurring.ChangeRecurringStatusRequest;
import com.example.demo.dto.response.RecurringTransactionResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.RecurringTransaction;
import com.example.demo.entity.enums.RecurringTransactionStatus;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.RecurringTransactionMapper;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.RecurringTransactionRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final RecurringTransactionMapper mapper;
    private final RecurringTransactionExecutor executor;

    @Transactional
    public RecurringTransactionResponse create(CreateRecurringTransactionRequest request) {
        Account account = getAccountAndCheckOwnership(request.getAccountId());

        if (account.getStatus() != com.example.demo.entity.enums.AccountStatus.ACTIVE) {
            throw new BusinessException("Source account is not active");
        }

        Account toAccount = null;
        if (request.getTransactionType() == TransactionType.TRANSFER) {
            if (request.getToAccountId() == null) {
                throw new BusinessException("toAccountId is required for TRANSFER");
            }
            if (request.getToAccountId().equals(request.getAccountId())) {
                throw new BusinessException("Source and target account cannot be the same");
            }
            toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new NotFoundException("Target account not found: " + request.getToAccountId()));
            if (toAccount.getStatus() != com.example.demo.entity.enums.AccountStatus.ACTIVE) {
                throw new BusinessException("Target account is not active");
            }
        } else if (request.getTransactionType() == TransactionType.DEPOSIT) {
            if (request.getToAccountId() != null) {
                throw new BusinessException("toAccountId must be null for DEPOSIT");
            }
        } else {
            throw new BusinessException("Only DEPOSIT and TRANSFER transaction types are supported for recurring transactions");
        }

        if (!org.springframework.scheduling.support.CronExpression.isValidExpression(request.getCronExpression())) {
            throw new BusinessException("Invalid cron expression: " + request.getCronExpression());
        }

        if (request.getAmount().compareTo(account.getTransactionLimit()) > 0) {
            throw new BusinessException("Amount exceeds account transaction limit: " + account.getTransactionLimit());
        }

        RecurringTransaction rt = mapper.toEntity(request, account, toAccount);
        RecurringTransaction saved = recurringRepository.save(rt);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<RecurringTransactionResponse> search(String accountNumber, String transactionType,
                                                      RecurringTransactionStatus status, Pageable pageable) {
        Specification<RecurringTransaction> spec = (root, query, cb) -> cb.conjunction();

        if (SecurityUtils.isCustomer()) {
            Long customerId = SecurityUtils.getCurrentCustomerId();
            if (customerId != null) {
                spec = spec.and((root, q, cb) ->
                        cb.equal(root.get("account").get("customer").get("id"), customerId));
            }
        }

        if (StringUtils.hasText(accountNumber)) {
            String pattern = accountNumber.trim() + "%";
            spec = spec.and((root, q, cb) ->
                    cb.like(root.get("account").get("accountNumber"), pattern));
        }

        if (StringUtils.hasText(transactionType)) {
            try {
                TransactionType type = TransactionType.valueOf(transactionType.toUpperCase());
                spec = spec.and((root, q, cb) -> cb.equal(root.get("transactionType"), type));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid transaction type: " + transactionType);
            }
        }

        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        return recurringRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getById(Long id) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());
        return mapper.toResponse(rt);
    }

    @Transactional
    public RecurringTransactionResponse update(Long id, UpdateRecurringTransactionRequest request) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());

        Account toAccount = null;
        if (request.getToAccountId() != null) {
            toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new NotFoundException("Target account not found: " + request.getToAccountId()));
        }

        if (request.getCronExpression() != null && !org.springframework.scheduling.support.CronExpression.isValidExpression(request.getCronExpression())) {
            throw new BusinessException("Invalid cron expression: " + request.getCronExpression());
        }

        if (request.getAmount() != null && request.getAmount().compareTo(rt.getAccount().getTransactionLimit()) > 0) {
            throw new BusinessException("Amount exceeds account transaction limit: " + rt.getAccount().getTransactionLimit());
        }

        mapper.updateEntity(request, rt, toAccount);
        RecurringTransaction saved = recurringRepository.save(rt);
        return mapper.toResponse(saved);
    }

    @Transactional
    public RecurringTransactionResponse changeStatus(Long id, ChangeRecurringStatusRequest request) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());

        rt.setStatus(request.getStatus());
        if (request.getStatus() == RecurringTransactionStatus.ACTIVE && rt.getNextRunAt() == null) {
            rt.computeNextRun();
        }
        RecurringTransaction saved = recurringRepository.save(rt);
        return mapper.toResponse(saved);
    }

    @Transactional
    public RecurringTransactionResponse runNow(Long id) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());

        if (rt.getStatus() != RecurringTransactionStatus.ACTIVE) {
            throw new BusinessException("Cannot run recurring transaction with status: " + rt.getStatus());
        }

        executor.execute(rt);
        return mapper.toResponse(rt);
    }

    @Transactional
    public void delete(Long id) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());
        recurringRepository.delete(rt);
    }

    private RecurringTransaction getEntity(Long id) {
        return recurringRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recurring transaction not found: " + id));
    }

    private Account getAccountAndCheckOwnership(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        return account;
    }

    private void checkOwnershipOrAdmin(Long customerId) {
        if (!SecurityUtils.isAdmin()) {
            Long currentCustomerId = SecurityUtils.getCurrentCustomerId();
            if (currentCustomerId == null || !currentCustomerId.equals(customerId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied: resource belongs to another customer");
            }
        }
    }
}