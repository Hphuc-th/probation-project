package com.example.demo.recurringTransaction.entity;

import com.example.demo.account.entity.Account;
import com.example.demo.recurringTransaction.entity.enums.RecurringTransactionStatus;
import com.example.demo.transaction.entity.enums.TransactionType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.scheduling.support.CronExpression;
import java.time.LocalDateTime;


@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recurring_account"))
    private Account account;

    @Column(name = "to_account_id")
    private Long toAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal fee = java.math.BigDecimal.ZERO;

    @Column(name = "location")
    private String location;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecurringTransactionStatus status = RecurringTransactionStatus.ACTIVE;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = RecurringTransactionStatus.ACTIVE;
        }
    }

    public void computeNextRun() {
        CronExpression cron = CronExpression.parse(cronExpression);
        LocalDateTime base = (nextRunAt != null && nextRunAt.isAfter(LocalDateTime.now())) ? nextRunAt : LocalDateTime.now();
        nextRunAt = cron.next(base);
    }

    public void markSuccess() {
        lastRunAt = LocalDateTime.now();
        lastError = null;
        consecutiveFailures = 0;
        computeNextRun();
    }

    public void markFailure(String error, int maxFailures) {
        lastRunAt = LocalDateTime.now();
        lastError = error;
        consecutiveFailures++;
        if (consecutiveFailures >= maxFailures) {
            status = RecurringTransactionStatus.PAUSED;
        }
    }

    public boolean isRunnable() {
        return status == RecurringTransactionStatus.ACTIVE
                && nextRunAt != null
                && !nextRunAt.isAfter(LocalDateTime.now());
    }

    public boolean isTransfer() {
        return transactionType == TransactionType.TRANSFER && toAccountId != null;
    }
}