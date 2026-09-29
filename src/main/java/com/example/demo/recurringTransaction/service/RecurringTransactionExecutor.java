package com.example.demo.recurringTransaction.service;

import com.example.demo.account.entity.Account;
import com.example.demo.account.repository.AccountRepository;
import com.example.demo.exception.BusinessException;
import com.example.demo.recurringTransaction.entity.RecurringTransaction;
import com.example.demo.recurringTransaction.entity.enums.RecurringTransactionStatus;
import com.example.demo.recurringTransaction.repository.RecurringTransactionRepository;
import com.example.demo.transaction.entity.Transaction;
import com.example.demo.transaction.entity.enums.TransactionType;
import com.example.demo.transaction.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionExecutor {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Value("${app.scheduler.max-failures:3}")
    private int maxFailures;

    @Transactional
    public void execute(RecurringTransaction rt) {
        if (rt.getStatus() != RecurringTransactionStatus.ACTIVE) {
            log.debug("Skipping recurring transaction {} (status={})", rt.getId(), rt.getStatus());
            return;
        }

        if (!rt.isRunnable()) {
            log.debug("Recurring transaction {} not yet due (nextRunAt={})", rt.getId(), rt.getNextRunAt());
            return;
        }

        Account account = rt.getAccount();
        if (account == null || account.getStatus() != com.example.demo.account.entity.enums.AccountStatus.ACTIVE) {
            handleFailure(rt, "Source account not active or not found");
            return;
        }

        try {
            account.validateTransactionAmount(rt.getAmount());

            if (rt.isTransfer()) {
                executeTransfer(rt, account);
            } else {
                executeDeposit(rt, account);
            }

            rt.markSuccess();
            recurringRepository.save(rt);
            log.info("Executed recurring transaction {} ({}) for account {}",
                    rt.getId(), rt.getTransactionType(), account.getAccountNumber());

        }  catch (Exception e) {
    handleFailure(rt, e.getMessage());
}
    }

    private void executeDeposit(RecurringTransaction rt, Account account) {
        BigDecimal fee = rt.getFee() != null ? rt.getFee() : BigDecimal.ZERO;

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setAmount(rt.getAmount());
        tx.setFee(fee);
        tx.setLocation(rt.getLocation());
        transactionRepository.saveAndFlush(tx);

        account.deposit(rt.getAmount());
    }

    private void executeTransfer(RecurringTransaction rt, Account from) {
        if (rt.getToAccountId() == null) {
            throw new BusinessException("Target account not configured");
        }
        Account to = accountRepository.findById(rt.getToAccountId()).orElseThrow(
                () -> new BusinessException("Target account not found"));
        if (to.getStatus() != com.example.demo.account.entity.enums.AccountStatus.ACTIVE) {
            throw new BusinessException("Target account not active");
        }

        from.validateTransactionAmount(rt.getAmount());

        BigDecimal fee = rt.getFee() != null ? rt.getFee() : BigDecimal.ZERO;

        Transaction tx = new Transaction();
        tx.setAccount(from);
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setAmount(rt.getAmount());
        tx.setFee(fee);
        tx.setLocation(rt.getLocation());
        transactionRepository.saveAndFlush(tx);

        from.withdraw(rt.getAmount().add(fee));
        to.deposit(rt.getAmount());
    }

    private void handleFailure(RecurringTransaction rt, String error) {
        rt.markFailure(error, maxFailures);
        recurringRepository.save(rt);
        log.warn("Recurring transaction {} failed: {} (failures={}, status={})",
                rt.getId(), error, rt.getConsecutiveFailures(), rt.getStatus());
    }
}