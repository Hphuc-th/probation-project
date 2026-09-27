package com.example.demo.service;

import com.example.demo.dto.request.transaction.CreateTransactionRequest;
import com.example.demo.dto.request.transaction.DepositRequest;
import com.example.demo.dto.request.transaction.TransferRequest;
import com.example.demo.dto.request.transaction.WithdrawRequest;
import com.example.demo.dto.response.TransactionResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.enums.AccountStatus;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.TransactionMapper;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchTransactions(Long accountId, String accountNumber,
                                                        TransactionType transactionType,
                                                        LocalDateTime fromDate, LocalDateTime toDate,
                                                        Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate must be before toDate");
        }
        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();
        if (SecurityUtils.isCustomer()) {
            Long currentCustomerId = SecurityUtils.getCurrentCustomerId();
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("account").get("customer").get("id"), currentCustomerId));
        }
        if (accountId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("account").get("id"), accountId));
        }
        if (StringUtils.hasText(accountNumber)) {
            String pattern = accountNumber.trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("account").get("accountNumber"), pattern));
        }
        if (transactionType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("transactionType"), transactionType));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
        return transactionRepository.findAll(spec, pageable).map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        Transaction tx = getEntity(id);
        SecurityUtils.checkOwnershipOrAdmin(tx.getAccount().getCustomer().getId());
        return transactionMapper.toResponse(tx);
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found: " + request.getAccountId()));
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setAccount(account);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        Account account = getActiveAccount(request.getAccountId());
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        account.validateTransactionAmount(request.getAmount());

        BigDecimal fee = nz(request.getFee());
        Transaction saved = insertAndFlush(account, TransactionType.DEPOSIT,
                request.getAmount(), fee, request.getLocation());
        account.deposit(request.getAmount());
        accountRepository.save(account);
        return transactionMapper.toResponse(saved);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        Account account = getActiveAccount(request.getAccountId());
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        account.validateTransactionAmount(request.getAmount());

        BigDecimal fee = nz(request.getFee());
        account.withdraw(request.getAmount().add(fee));
        Transaction saved = insertAndFlush(account, TransactionType.WITHDRAW,
                request.getAmount(), fee, request.getLocation());
        accountRepository.save(account);
        return transactionMapper.toResponse(saved);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BusinessException("fromAccountId and toAccountId must be different");
        }
        Account from = getActiveAccount(request.getFromAccountId());
        Account to = getActiveAccount(request.getToAccountId());
        SecurityUtils.checkOwnershipOrAdmin(from.getCustomer().getId());
        SecurityUtils.checkOwnershipOrAdmin(to.getCustomer().getId());
        from.validateTransactionAmount(request.getAmount());

        BigDecimal fee = nz(request.getFee());
        from.withdraw(request.getAmount().add(fee));
        Transaction saved = insertAndFlush(from, TransactionType.TRANSFER,
                request.getAmount(), fee, request.getLocation());
        to.deposit(request.getAmount());
        accountRepository.save(from);
        accountRepository.save(to);
        return transactionMapper.toResponse(saved);
    }

    private Account getActiveAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active: " + id);
        }
        return account;
    }

    private Transaction insertAndFlush(Account account, TransactionType type,
                                       BigDecimal amount, BigDecimal fee, String location) {
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setLocation(location);
        try {
            return transactionRepository.saveAndFlush(tx);
        } catch (DataAccessException e) {
            throw mapTriggerError(e);
        }
    }

    private RuntimeException mapTriggerError(DataAccessException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("Insufficient balance")) {
            return new BusinessException("Insufficient balance");
        }
        if (msg.contains("exceeds transaction limit")) {
            return new BusinessException("Amount exceeds transaction limit");
        }
        if (msg.contains("Account is not active")) {
            return new BusinessException("Account is not active");
        }
        if (msg.contains("Account not found")) {
            return new NotFoundException("Account not found");
        }
        return e;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }


    private Transaction getEntity(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }
}
