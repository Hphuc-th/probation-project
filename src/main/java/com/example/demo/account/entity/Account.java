package com.example.demo.account.entity;

import com.example.demo.account.entity.enums.AccountStatus;
import com.example.demo.account.entity.enums.AccountType;
import com.example.demo.customer.entity.Customer;
import com.example.demo.exception.BusinessException;
import com.example.demo.accountStatusHistory.entity.AccountStatusHistory;
import com.example.demo.recurringTransaction.entity.RecurringTransaction;
import com.example.demo.transaction.entity.Transaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "transaction_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal transactionLimit;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<Transaction> transactionList = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<RecurringTransaction> recurringTransactionList = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<AccountStatusHistory> statusHistoryList = new ArrayList<>();

    public void deposit(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }

        this.balance = this.balance.subtract(amount);
    }

    public void validateTransactionAmount(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (amount.compareTo(transactionLimit) > 0) {
            throw new BusinessException(
                    "Amount exceeds transaction limit: " + transactionLimit);
        }
    }

    public void changeStatus(AccountStatus target) {
        if (this.status == target) {
            throw new BusinessException("Account status is already " + target);
        }

        if (this.status == AccountStatus.CLOSED) {
            throw new BusinessException("Closed account status cannot be changed");
        }

        if (target == AccountStatus.CLOSED
                && this.balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot close account with non-zero balance");
        }

        this.status = target;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }
    }
}