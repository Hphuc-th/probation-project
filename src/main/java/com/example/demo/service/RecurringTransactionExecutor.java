package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.RecurringTransaction;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.enums.RecurringTransactionStatus;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.RecurringTransactionRepository;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionExecutor {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;

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
        if (account == null || account.getStatus() != com.example.demo.entity.enums.AccountStatus.ACTIVE) {
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

        } catch (BusinessException e) {
            handleFailure(rt, e.getMessage());
        } catch (DataAccessException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Database error";
            if (msg.contains("Insufficient balance")) {
                handleFailure(rt, "Insufficient balance");
            } else if (msg.contains("exceeds transaction limit")) {
                handleFailure(rt, "Amount exceeds transaction limit");
            } else if (msg.contains("Account is not active")) {
                handleFailure(rt, "Account is not active");
            } else {
                handleFailure(rt, "Execution error: " + msg);
            }
        } catch (Exception e) {
            handleFailure(rt, "Unexpected error: " + e.getMessage());
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
        Account to = rt.getToAccount();
        if (to == null || to.getStatus() != com.example.demo.entity.enums.AccountStatus.ACTIVE) {
            throw new BusinessException("Target account not active or not found");
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